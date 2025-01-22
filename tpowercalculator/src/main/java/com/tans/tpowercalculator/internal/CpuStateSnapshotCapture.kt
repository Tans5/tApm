package com.tans.tpowercalculator.internal

import android.os.Process
import java.io.File
internal class CpuStateSnapshotCapture(
    private val powerProfile: PowerProfile
) {
    init {
        checkCpuSpeedAndTime()
        checkProcessCpuSpeedAndTime()
        tPowerLog.d(TAG, "Init CpuStateSnapshotCapture success.")
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

    companion object {

        private const val TAG = "CpuStateSnapshotCapture"

        private fun readCpuCoreTimeSum(cpuCoreIndex: Int): Long {
            return readCpuCoreTime(cpuCoreIndex).sumOf { it.second }
        }

        // first: Cpu speed, second: time in jiffies
        private fun readCpuCoreTime(cpuCoreIndex: Int): List<Pair<Int, Long>> {
            val f = File("/sys/devices/system/cpu/cpu${cpuCoreIndex}/cpufreq/stats/time_in_state")
            val result = ArrayList<Pair<Int, Long>>()
            val lines = f.inputStream().bufferedReader(Charsets.UTF_8).use {
                it.readLines()
            }
            for (l in lines) {
                result.add(
                    l.split(" ").let {
                        it[0].toInt() to it[1].toLong()
                    }
                )
            }
            return result
        }

        private fun readCurrentProcessCpuCoreTime(): Map<Int, List<Pair<Int, Long>>> = readProcessCpuCoreTime(Process.myPid())

        private fun readProcessCpuCoreTime(pid: Int): Map<Int, List<Pair<Int, Long>>> {
            val f = File("/proc/$pid/time_in_state")
            val lines = f.inputStream().bufferedReader(Charsets.UTF_8).use {
                it.readLines()
            }
            val reCpuTime = "cpu([0-9]*)".toRegex()
            val result = mutableMapOf<Int, List<Pair<Int, Long>>>()
            var parsingCpu: ArrayList<Pair<Int, Long>>? = null
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
                    parsingCpu!!.add(it[0].toInt() to it[1].toLong())
                }
            }
            if (parsingCpu != null) {
                result[parsingCpuIndex!!] = parsingCpu
            }
            return result
        }
    }
}