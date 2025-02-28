package com.tans.tpowercalculator.internal

import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

internal class CpuStateSnapshotCapture(private val powerProfile: PowerProfile) {

    val isInitSuccess: Boolean

    private val cpuSpeedSpecs: List<CpuSpec> by lazy {
        readCpuCoresSpec(powerProfile.cpuProfile.coreCount)
    }

    init {
        isInitSuccess = try {
            checkCpuSpeedAndTime()
            checkProcessCpuSpeedAndTime()
            checkCpuCoreIdleTime()
            checkCpuCoreSpeed()
            cpuSpeedSpecs
            tPowerLog.d(TAG, "Init CpuStateSnapshotCapture success.")
            true
        } catch (e: Throwable) {
            closeAllRandomFiles()
            tPowerLog.e(TAG, "Init CpuStateSnapshotCapture fail.", e)
            false
        }
    }

    fun readCpuStateSnapshotBuffer(): CpuStateSnapshotBuffer? {
        return if (isInitSuccess) {
            val cpuCoreCount = powerProfile.cpuProfile.coreCount
            val currentProcessCpuSpeedToTimeBuffer = readProcessCpuCoreTimeBuffer(Process.myPid())
            val coreStateBuffers = mutableListOf<SingleCoreStateSnapshotBuffer>()
            repeat(cpuCoreCount) { coreIndex ->
                val coreSpeedBuffer = readCpuCoreSpeedBuffer(coreIndex)
                val cpuCoreTimeBuffer = readCpuCoreTimeBuffer(coreIndex)
                val cpuIdleTimeBuffer = readCpuCoreIdleTimeBuffer(coreIndex)
                coreStateBuffers.add(
                    SingleCoreStateSnapshotBuffer(
                    coreIndex = coreIndex,
                    currentCoreSpeedBuffer = coreSpeedBuffer,
                    cpuSpeedToTimeBuffer = cpuCoreTimeBuffer,
                    cpuIdleTimeBuffer = cpuIdleTimeBuffer
                )
                )
            }
            CpuStateSnapshotBuffer(
                createTime = SystemClock.uptimeMillis(),
                coreStateBuffers = coreStateBuffers,
                currentProcessCpuSpeedToTimeBuffer = currentProcessCpuSpeedToTimeBuffer
            )
        } else {
            null
        }
    }

    fun parseCpuStateSnapshotBuffer(buffer: CpuStateSnapshotBuffer): CpuStateSnapshot {
        return CpuStateSnapshot(
            createTime = buffer.createTime,
            coreStates = buffer.coreStateBuffers.map {
                SingleCoreStateSnapshot(
                    coreIndex = it.coreIndex,
                    currentCoreSpeedInKHz = parseCpuCoreSpeed(it.currentCoreSpeedBuffer),
                    cpuSpeedToTime = parseCpuCoreTimeBuffer(it.cpuSpeedToTimeBuffer),
                    cpuIdleTime = parseCpuCoreIdleTime(it.cpuIdleTimeBuffer)
                )
            },
            currentProcessCpuSpeedToTime = parseProcessCpuCoreTimeBuffer(buffer.currentProcessCpuSpeedToTimeBuffer)
        )
    }

    fun calculateCpuUsage(state1: CpuStateSnapshot, state2: CpuStateSnapshot): CpuUsage {
        val previous: CpuStateSnapshot
        val next: CpuStateSnapshot
        if (state1.createTime < state2.createTime) {
            previous = state1
            next = state2
        } else {
            next = state1
            previous = state2
        }
        val durationInMillis = next.createTime - previous.createTime

        // All processes cpu cores usages.
        val cpuCoreUsages = mutableListOf<SingleCpuCoreUsage>()
        for (ns in next.coreStates) {
            val coreSpec = cpuSpeedSpecs[ns.coreIndex]
            val ps = previous.coreStates[ns.coreIndex]
            val cpuIdleTimeInJiffies = ns.cpuIdleTime - ps.cpuIdleTime
            var cpuWorkTimeInJiffies = 0L
            var allCpuTimeInJiffies = cpuIdleTimeInJiffies
            for ((index, nSpeedAndTime) in ns.cpuSpeedToTime.withIndex()) {
                val (speed, nTimeInJiffies) = nSpeedAndTime
                val (_, pTimeInJiffies) = ps.cpuSpeedToTime[index]
                val diffInJiffies = nTimeInJiffies - pTimeInJiffies
                allCpuTimeInJiffies += diffInJiffies
                cpuWorkTimeInJiffies += (diffInJiffies.toDouble() * speed.toDouble() / coreSpec.maxSpeedInKHz.toDouble()).toLong()
            }
            val cpuUsage = cpuWorkTimeInJiffies.toDouble() / allCpuTimeInJiffies.toDouble()
            cpuCoreUsages.add(
                SingleCpuCoreUsage(
                    coreIndex = ns.coreIndex,
                    coreSpec = coreSpec,
                    currentCoreSpeedInKHz = ns.currentCoreSpeedInKHz,
                    cpuUsage = cpuUsage,
                    allCpuTimeInJiffies = allCpuTimeInJiffies,
                    cpuIdleTimeInJiffies = cpuIdleTimeInJiffies,
                    cpuWorkTimeInJiffies = cpuWorkTimeInJiffies
                )
            )
        }
        val maxCpuSpeed = cpuSpeedSpecs.maxBy { it.maxSpeedInKHz }.maxSpeedInKHz
        var avgCpuUsageNum = 0.0
        var avgCpuUsageDen = 0.0
        for (usage in cpuCoreUsages) {
            val coreSpec = cpuSpeedSpecs[usage.coreIndex]
            avgCpuUsageDen += coreSpec.maxSpeedInKHz.toDouble() / maxCpuSpeed.toDouble()
            avgCpuUsageNum += coreSpec.maxSpeedInKHz.toDouble() / maxCpuSpeed.toDouble() * usage.cpuUsage
        }
        val avgCpuUsage = avgCpuUsageNum / avgCpuUsageDen

        // Current process cpu cores usage.
        val currentProcessCpuCoresUsage = mutableListOf<ProgressSingleCpuCoreUsage>()
        for (ns in next.currentProcessCpuSpeedToTime) {
            val coreIndex = ns.key
            val ps = previous.currentProcessCpuSpeedToTime[coreIndex]
            val core = cpuCoreUsages.find { it.coreIndex == coreIndex }!!
            var cpuWorkTimeInJiffies = 0L
            for ((speed, nTimeInJiffies) in ns.value) {
                val pTimeInJiffies = ps?.find { it.first == speed }?.second ?: 0L
                val diffInJiffies = nTimeInJiffies - pTimeInJiffies
                cpuWorkTimeInJiffies += (diffInJiffies.toDouble() * speed.toDouble() / core.coreSpec.maxSpeedInKHz.toDouble()).toLong()
            }
            val usage = cpuWorkTimeInJiffies.toDouble() / core.allCpuTimeInJiffies.toDouble()
            currentProcessCpuCoresUsage.add(
                ProgressSingleCpuCoreUsage(
                    refCore = core,
                    cpuWorkTimeInJiffies = cpuWorkTimeInJiffies,
                    cpuUsage = usage
                )
            )
        }
        var currentProcessAvgCpuUsageNum = 0.0
        var currentProcessAvgCpuUsageDen = 0.0
        for (usage in currentProcessCpuCoresUsage) {
            currentProcessAvgCpuUsageDen += usage.refCore.coreSpec.maxSpeedInKHz.toDouble() / maxCpuSpeed.toDouble()
            currentProcessAvgCpuUsageNum += usage.refCore.coreSpec.maxSpeedInKHz.toDouble() / maxCpuSpeed.toDouble() * usage.cpuUsage
        }
        val currentProcessAvgCpuUsage = currentProcessAvgCpuUsageNum / currentProcessAvgCpuUsageDen
        return CpuUsage(
            durationInMillis = durationInMillis,
            cpuCoresUsage = cpuCoreUsages,
            avgCpuUsage = avgCpuUsage,
            currentProcessCpuCoresUsage = currentProcessCpuCoresUsage,
            currentProcessAvgCpuUsage = currentProcessAvgCpuUsage
        )
    }

    private fun checkCpuSpeedAndTime() {
        val cpuProfile = powerProfile.cpuProfile
        repeat(cpuProfile.coreCount) { coreIndex ->
            val b = readCpuCoreTimeBuffer(coreIndex)
            parseCpuCoreTimeBuffer(b)
        }
    }

    private fun checkProcessCpuSpeedAndTime() {
        val b = readProcessCpuCoreTimeBuffer(Process.myPid())
        parseProcessCpuCoreTimeBuffer(b)
    }

    private fun checkCpuCoreIdleTime() {
        repeat(powerProfile.cpuProfile.coreCount) { coreIndex ->
            val b = readCpuCoreIdleTimeBuffer(coreIndex)
            parseCpuCoreIdleTime(b)
        }
    }

    private fun checkCpuCoreSpeed() {
        repeat(powerProfile.cpuProfile.coreCount) { coreIndex ->
            val b = readCpuCoreSpeedBuffer(coreIndex)
            parseCpuCoreSpeed(b)
        }
    }

    companion object {

        private const val TAG = "CpuStateSnapshotCapture"

        data class SingleCoreStateSnapshot(
            val coreIndex: Int,
            val currentCoreSpeedInKHz: Long,
            val cpuSpeedToTime: List<Pair<Long, Long>>,
            val cpuIdleTime: Long
        )

        data class CpuStateSnapshot(
            val createTime: Long,
            val coreStates: List<SingleCoreStateSnapshot>,
            val currentProcessCpuSpeedToTime: Map<Int, List<Pair<Long, Long>>>
        )

        data class SingleCoreStateSnapshotBuffer(
            val coreIndex: Int,
            val currentCoreSpeedBuffer: ByteArray,
            val cpuSpeedToTimeBuffer: ByteArray,
            val cpuIdleTimeBuffer: List<ByteArray>
        )

        data class CpuStateSnapshotBuffer(
            val createTime: Long,
            val coreStateBuffers: List<SingleCoreStateSnapshotBuffer>,
            val currentProcessCpuSpeedToTimeBuffer: ByteArray
        )

        data class SingleCpuCoreUsage(
            val coreIndex: Int,
            val coreSpec: CpuSpec,
            val currentCoreSpeedInKHz: Long,
            val cpuUsage: Double,
            val allCpuTimeInJiffies: Long,
            val cpuIdleTimeInJiffies: Long,
            val cpuWorkTimeInJiffies: Long,
        )

        data class ProgressSingleCpuCoreUsage(
            val refCore: SingleCpuCoreUsage,
            val cpuWorkTimeInJiffies: Long,
            val cpuUsage: Double
        )

        data class CpuUsage(
            val durationInMillis: Long,
            val cpuCoresUsage: List<SingleCpuCoreUsage>,
            val avgCpuUsage: Double,
            val currentProcessCpuCoresUsage: List<ProgressSingleCpuCoreUsage>,
            val currentProcessAvgCpuUsage: Double
        )

        val oneJiffyInMillis: Long by lazy {
            (1000.0 / Os.sysconf(OsConstants._SC_CLK_TCK).toDouble()).toLong()
        }

        private val randomFiles: ConcurrentHashMap<String, RandomAccessFile> = ConcurrentHashMap()


        private const val MAX_RANDOM_FILE_LENGTH = 1024 * 20 // 20 KB

        private val randomFileBuffer: ByteArray by lazy {
            ByteArray(MAX_RANDOM_FILE_LENGTH)
        }
        private val randomFileReadBuffer: ByteArray by lazy {
            ByteArray(1024)
        }
        private fun readRandomFileBuffer(key: String): ByteArray {
            val f = randomFiles[key]
            val targetFile = if (f != null) {
                f
            } else {
                val newFile = RandomAccessFile(key, "r")
                val oldFile = randomFiles.putIfAbsent(key, newFile)
                if (oldFile != null) {
                    newFile.close()
                    oldFile
                } else {
                    newFile
                }
            }
            targetFile.seek(0)
            var hasReadCount = 0
            var thisTimeReadCount = 0
            do {
                thisTimeReadCount = targetFile.read(randomFileReadBuffer)
                if (thisTimeReadCount > 0) {
                    if (hasReadCount > (MAX_RANDOM_FILE_LENGTH - thisTimeReadCount)) {
                        error("Random file max size is $MAX_RANDOM_FILE_LENGTH")
                    }
                    System.arraycopy(randomFileReadBuffer, 0, randomFileBuffer, hasReadCount, thisTimeReadCount)
                    hasReadCount += thisTimeReadCount
                }
            } while (thisTimeReadCount >= 0)

            return if (hasReadCount > 0) {
                randomFileBuffer.copyOf(hasReadCount)
            } else {
                ByteArray(0)
            }
        }

        private fun closeAllRandomFiles() {
            val i = randomFiles.iterator()
            if (i.hasNext()) {
                i.next().value.close()
                i.remove()
            }
        }

        /**
         * https://docs.kernel.org/cpu-freq/cpufreq-stats.html
         */
        private fun readCpuCoreTimeBuffer(cpuCoreIndex: Int): ByteArray {
            return readRandomFileBuffer("/sys/devices/system/cpu/cpu${cpuCoreIndex}/cpufreq/stats/time_in_state")
        }
        // first: Cpu speed, second: time in jiffies
        private fun parseCpuCoreTimeBuffer(buffer: ByteArray): List<Pair<Long, Long>> {
            val lines = String(buffer, Charsets.UTF_8).lines().filter { it.isNotBlank() }.map { it.trim() }
            val result = ArrayList<Pair<Long, Long>>()
            for (l in lines) {
                result.add(
                    l.split(" ").let {
                        it[0].toLong() to it[1].toLong()
                    }
                )
            }
            return result
        }

        private fun readProcessCpuCoreTimeBuffer(pid: Int): ByteArray {
            return readRandomFileBuffer("/proc/$pid/time_in_state")
        }

        private val reCpuTime = "cpu([0-9]*)".toRegex()
        private fun parseProcessCpuCoreTimeBuffer(buffer: ByteArray): Map<Int, List<Pair<Long, Long>>> {
            val lines = String(buffer, Charsets.UTF_8).lines().filter { it.isNotBlank() }.map { it.trim() }
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

        /**
         * https://android.googlesource.com/kernel/common/+/a7827a2a60218b25f222b54f77ed38f57aebe08b/Documentation/cpuidle/sysfs.txt
         */
        private fun readCpuCoreIdleTimeBuffer(cpuCoreIndex: Int): List<ByteArray> {
            val baseDir = File("/sys/devices/system/cpu/cpu$cpuCoreIndex/cpuidle")
            val result: MutableList<ByteArray> = mutableListOf()
            val childrenFiles = baseDir.listFiles() ?: emptyArray()
            var isFoundIdleFile = false
            for (c in childrenFiles) {
                if (c.name.startsWith("state")) {
                    val b = readRandomFileBuffer(c.canonicalPath + "/time")
                    result.add(b)
                    isFoundIdleFile = true
                }
            }
            if (!isFoundIdleFile) {
                error("Didn't found cpu idle file.")
            }
            return result
        }

        private fun parseCpuCoreIdleTimeInMicroseconds(buffers: List<ByteArray>): Long {
            var result = 0L
            for (b in buffers) {
                result += String(b, Charsets.UTF_8).trim().toLong()
            }
            return result
        }

        private fun parseCpuCoreIdleTime(buffers: List<ByteArray>): Long {
            return parseCpuCoreIdleTimeInMicroseconds(buffers) / (1000L * oneJiffyInMillis)
        }

        /**
         * https://www.kernel.org/doc/Documentation/cpu-freq/user-guide.txt
         */
        private fun readCpuCoreSpeedBuffer(coreIndex: Int): ByteArray {
            return readRandomFileBuffer("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
        }

        private fun parseCpuCoreSpeed(buffer: ByteArray): Long {
            return String(buffer, Charsets.UTF_8).trim().toLong()
        }

        data class CpuSpec(
            val cpuCoreIndex: Int,
            val minSpeedInKHz: Long,
            val maxSpeedInKHz: Long,
            val availableSpeedsInKHz: List<Long>
        )

        private fun readCpuCoresSpec(cpuCoreCount: Int): List<CpuSpec> {
            val result = ArrayList<CpuSpec>()
            repeat(cpuCoreCount) { cpuCoreIndex ->
                val baseDir = File("/sys/devices/system/cpu/cpu$cpuCoreIndex/cpufreq")
                val minSpeed = File(baseDir, "cpuinfo_min_freq").readText(Charsets.UTF_8).trim().toLong()
                val maxSpeed = File(baseDir, "cpuinfo_max_freq").readText(Charsets.UTF_8).trim().toLong()
                val availableSpeeds = File(baseDir, "scaling_available_frequencies")
                    .readText(Charsets.UTF_8)
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .map { it.trim().toLong() }
                result.add(
                    CpuSpec(
                        cpuCoreIndex = cpuCoreIndex,
                        minSpeedInKHz = minSpeed,
                        maxSpeedInKHz = maxSpeed,
                        availableSpeedsInKHz = availableSpeeds
                    )
                )
            }
            return result
        }
    }
}