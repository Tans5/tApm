package com.tans.tpowercalculator.internal

import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import java.io.File
internal class CpuStateSnapshotCapture(private val powerProfile: PowerProfile) {

    val isInitSuccess: Boolean

    init {
        isInitSuccess = try {
            checkCpuSpeedAndTime()
            checkProcessCpuSpeedAndTime()
            checkCpuCoreIdleTime()
            checkCpuCoreSpeed()
            tPowerLog.d(TAG, "Init CpuStateSnapshotCapture success.")
            true
        } catch (e: Throwable) {
            tPowerLog.e(TAG, "Init CpuStateSnapshotCapture fail.", e)
            false
        }
    }

    fun createCpuStateSnapshot(): CpuStateSnapshot {
        val cpuCoreCount = powerProfile.cpuProfile.coreCount
        val currentProcessCpuSpeedToTime = readCurrentProcessCpuCoreTime()
        val coreStates = mutableListOf<SingleCoreStateSnapshot>()
        repeat(cpuCoreCount) { coreIndex ->
            val cpuSpeed = readCpuCoreSpeed(coreIndex)
            val cpuSpeedToTime = readCpuCoreTime(coreIndex)
            val cpuIdleTime = readCpuCoreIdleTime(coreIndex)
            coreStates.add(
                SingleCoreStateSnapshot(
                    coreIndex = coreIndex,
                    cpuSpeed = cpuSpeed,
                    cpuSpeedToTime = cpuSpeedToTime,
                    cpuIdleTime = cpuIdleTime
                )
            )
        }
        return CpuStateSnapshot(
            createTime = SystemClock.uptimeMillis(),
            coreStates = coreStates,
            currentProcessCpuSpeedToTime = currentProcessCpuSpeedToTime
        )
    }

    private fun checkCpuSpeedAndTime() {
        val cpuProfile = powerProfile.cpuProfile
        repeat(cpuProfile.coreCount) { coreIndex ->
            val speedAndTime = readCpuCoreTime(coreIndex)
            val cluster = cpuProfile.cluster.find { coreIndex in it.coreIndexRange } ?: error("Didn't find Cluster of coreIndex=$coreIndex")
            if (cluster.frequencies.size != speedAndTime.size) {
                error("SpeedSize is ${cluster.frequencies.size}, but found ${speedAndTime.size}")
            }
            for ((speed, _) in speedAndTime) {
                val freq = cluster.frequencies.find { it.speedHz == speed }
                if (freq == null) {
                    error("Don't find speed: $speed")
                }
            }
        }
    }

    private fun checkProcessCpuSpeedAndTime() {
        val processCpuTime = readCurrentProcessCpuCoreTime()
        val cpuProfile = powerProfile.cpuProfile
        val maxCpuIndex = cpuProfile.coreCount - 1
        for ((cpuIndex, speedAndTime) in processCpuTime) {
            if (cpuIndex < 0 || cpuIndex > maxCpuIndex) {
                error("Wrong cpu index: $cpuIndex, maxCpuIndex=$maxCpuIndex")
            }
            val cluster = cpuProfile.cluster.find { cpuIndex in it.coreIndexRange } ?: error("Didn't find Cluster of coreIndex=$cpuIndex")
            for ((speed, _) in speedAndTime) {
                val freq = cluster.frequencies.find { it.speedHz == speed }
                if (freq == null) {
                    error("Don't find speed: $speed")
                }
            }
        }
    }

    private fun checkCpuCoreIdleTime() {
        repeat(powerProfile.cpuProfile.coreCount) { coreIndex ->
            readCpuCoreIdleTime(coreIndex)
        }
    }

    private fun checkCpuCoreSpeed() {
        repeat(powerProfile.cpuProfile.coreCount) { coreIndex ->
            readCpuCoreSpeed(coreIndex)
        }
    }

    companion object {

        private const val TAG = "CpuStateSnapshotCapture"

        data class SingleCoreStateSnapshot(
            val coreIndex: Int,
            val cpuSpeed: CpuSpeed,
            val cpuSpeedToTime: List<Pair<Long, Long>>,
            val cpuIdleTime: Long
        )

        data class CpuStateSnapshot(
            val createTime: Long,
            val coreStates: List<SingleCoreStateSnapshot>,
            val currentProcessCpuSpeedToTime: Map<Int, List<Pair<Long, Long>>>
        )

        val oneJiffyInMillis: Long by lazy {
            Os.sysconf(OsConstants._SC_CLK_TCK)
        }

        private fun readCpuCoreTimeSum(cpuCoreIndex: Int): Long {
            return readCpuCoreTime(cpuCoreIndex).sumOf { it.second }
        }

        // first: Cpu speed, second: time in jiffies
        private fun readCpuCoreTime(cpuCoreIndex: Int): List<Pair<Long, Long>> {
            val f = File("/sys/devices/system/cpu/cpu${cpuCoreIndex}/cpufreq/stats/time_in_state")
            val result = ArrayList<Pair<Long, Long>>()
            val lines = f.inputStream().bufferedReader(Charsets.UTF_8).use {
                it.readLines()
            }
            for (l in lines) {
                result.add(
                    l.split(" ").let {
                        it[0].toLong() to it[1].toLong()
                    }
                )
            }
            return result
        }

        private fun readCurrentProcessCpuCoreTime(): Map<Int, List<Pair<Long, Long>>> = readProcessCpuCoreTime(Process.myPid())

        private fun readProcessCpuCoreTime(pid: Int): Map<Int, List<Pair<Long, Long>>> {
            val f = File("/proc/$pid/time_in_state")
            val lines = f.inputStream().bufferedReader(Charsets.UTF_8).use {
                it.readLines()
            }
            val reCpuTime = "cpu([0-9]*)".toRegex()
            val result = mutableMapOf<Int, List<Pair<Long, Long>>>()
            var parsingCpu: ArrayList<Pair<Long, Long>>? = null
            var parsingCpuIndex: Int? = null
            for (l in lines) {
                if (reCpuTime.matches(l)) {
                    if (parsingCpu != null) {
                        result[parsingCpuIndex!!] = parsingCpu
                    }
                    parsingCpuIndex = reCpuTime.find(l)!!.groupValues[1].toInt()
                    parsingCpu = ArrayList()
                    continue
                }
                l.split(" ").let {
                    parsingCpu!!.add(it[0].toLong() to it[1].toLong())
                }
            }
            if (parsingCpu != null) {
                result[parsingCpuIndex!!] = parsingCpu
            }
            return result
        }

        // read cpu idle time in microseconds.
        private fun readCpuCoreIdleTimeInMicroseconds(cpuCoreIndex: Int): Long {
            val baseDir = File("/sys/devices/system/cpu/cpu$cpuCoreIndex/cpuidle")
            var result = 0L
            var isFoundIdleFile = false
            val childrenFiles = baseDir.listFiles() ?: emptyArray()
            for (c in childrenFiles) {
                if (c.name.startsWith("state")) {
                    val timeFile = File(c, "time")
                    result += timeFile.readText(Charsets.UTF_8).trim().toLong()
                    isFoundIdleFile = true
                }
            }
            if (!isFoundIdleFile) {
                error("Didn't found cpu idle file.")
            }
            return result
        }

        // read cpu idle time in jiffies.
        private fun readCpuCoreIdleTime(cpuCoreIndex: Int): Long {
            return readCpuCoreIdleTimeInMicroseconds(cpuCoreIndex) / (1000L * oneJiffyInMillis)
        }

        data class CpuSpeed(
            val minSpeedInHz: Long,
            val maxSpeedInHz: Long,
            val currentSpeedInHz: Long,
        )

        private fun readCpuCoreSpeed(cpuCoreIndex: Int): CpuSpeed {
            val baseDir = File("/sys/devices/system/cpu/cpu$cpuCoreIndex/cpufreq")
            val minSpeed = File(baseDir, "cpuinfo_min_freq").readText(Charsets.UTF_8).trim().toLong()
            val maxSpeed = File(baseDir, "cpuinfo_max_freq").readText(Charsets.UTF_8).trim().toLong()
            val currentSpeed = File(baseDir, "scaling_cur_freq").readText(Charsets.UTF_8).trim().toLong()
            return CpuSpeed(
                minSpeedInHz = minSpeed,
                maxSpeedInHz = maxSpeed,
                currentSpeedInHz = currentSpeed
            )
        }
    }
}