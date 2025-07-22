package com.tans.tapm

import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.CpuSpec
import com.tans.tlrucache.memory.LruByteArrayPool
import okio.IOException
import java.io.File
import kotlin.jvm.Throws

class CpuStateSnapshotCapture internal constructor(powerProfile: PowerProfile?) {

    val cpuCoreCount: Int = powerProfile?.cpuProfile?.coreCount ?: Runtime.getRuntime().availableProcessors()

    val isInitSuccess: Boolean

    val cpuSpeedSpecs: List<CpuSpec> by lazy {
        readCpuCoresSpec(cpuCoreCount)
    }

    private val bufferPool by lazy {
        LruByteArrayPool(BUFFER_POOL_SIZE)
    }

    init {
        isInitSuccess = try {
            if (cpuCoreCount <= 0) {
                error("Wrong cpu core count: $cpuCoreCount")
            }
            checkCpuSpeedAndTime()
            checkProcessCpuSpeedAndTime()
            checkCpuCoreIdleTime()
            checkCpuCoreSpeed()
            cpuSpeedSpecs
            tApmLog.d(TAG, "Init CpuStateSnapshotCapture success.")
            true
        } catch (e: Throwable) {
            tApmLog.e(TAG, "Init CpuStateSnapshotCapture fail.", e)
            false
        }
    }

    @Throws(IOException::class)
    fun readCpuStateSnapshot(): CpuStateSnapshot {
        return CpuStateSnapshot(
            createTime = SystemClock.uptimeMillis(),
            coreStates = (0 until cpuCoreCount).map {
                SingleCoreStateSnapshot(
                    coreIndex = it,
                    currentCoreSpeedInKHz = readCpuCoreSpeed(it, bufferPool),
                    cpuSpeedToTime = readCpuCoreTime(it, bufferPool),
                    cpuIdleTime = readCpuCoreIdleTime(it, bufferPool)
                )
            },
            currentProcessCpuSpeedToTime = readProcessCpuCoreTime(Process.myPid(), bufferPool)
        )
    }

    fun clearBufferPoolMemory() {
        bufferPool.clearMemory()
    }

    private fun checkCpuSpeedAndTime() {
        repeat(cpuCoreCount) { coreIndex ->
            readCpuCoreTime(coreIndex, bufferPool)
        }
    }

    private fun checkProcessCpuSpeedAndTime() {
        readProcessCpuCoreTime(Process.myPid(), bufferPool)
    }

    private fun checkCpuCoreIdleTime() {
        repeat(cpuCoreCount) { coreIndex ->
            readCpuCoreIdleTime(coreIndex, bufferPool)
        }
    }

    private fun checkCpuCoreSpeed() {
        repeat(cpuCoreCount) { coreIndex ->
            readCpuCoreSpeed(coreIndex, bufferPool)
        }
    }

    companion object {

        // 512K
        private const val BUFFER_SIZE = 512 * 1024

        // 2.5M
        private const val BUFFER_POOL_SIZE = BUFFER_SIZE * 5L

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

        val oneJiffyInMillis: Long by lazy {
            (1000.0 / Os.sysconf(OsConstants._SC_CLK_TCK).toDouble()).toLong()
        }

        /**
         * https://docs.kernel.org/cpu-freq/cpufreq-stats.html
         *
         * first: Cpu speed, second: time in jiffies
         */
        private fun readCpuCoreTime(coreIndex: Int, bufferPool: LruByteArrayPool): List<Pair<Long, Long>> {
            val file = File("/sys/devices/system/cpu/cpu${coreIndex}/cpufreq/stats/time_in_state")
            val bufferValue = bufferPool.get(BUFFER_SIZE)
            val result = ArrayList<Pair<Long, Long>>()
            try {
                file.inputStream().use { inputStream ->
                    val buffer = bufferValue.value
                    val readSize = inputStream.read(buffer)
                    if (readSize < 0) {
                        error("read file: ${file.canonicalPath} fail")
                    }
                    if (readSize >= BUFFER_SIZE) {
                        error("read file: ${file.canonicalPath} fail, file size greater than $BUFFER_SIZE bytes.")
                    }
                    val lines = String(buffer, 0, readSize, Charsets.UTF_8).lines().filter { it.isNotBlank() }.map { it.trim() }
                    for (l in lines) {
                        result.add(
                            l.split(" ").let {
                                it[0].toLong() to it[1].toLong()
                            }
                        )
                    }
                }
            } finally {
                bufferPool.put(bufferValue)
            }
            return result
        }

        private val reCpuTime by lazy {
            "cpu([0-9]*)".toRegex()
        }

        private fun readProcessCpuCoreTime(pid: Int, bufferPool: LruByteArrayPool): Map<Int, List<Pair<Long, Long>>> {
            val file = File("/proc/$pid/time_in_state")
            val bufferValue = bufferPool.get(BUFFER_SIZE)
            val result = mutableMapOf<Int, List<Pair<Long, Long>>>()
            try {
                file.inputStream().use { inputStream ->
                    val buffer = bufferValue.value
                    val readSize = inputStream.read(buffer)
                    if (readSize < 0) {
                        error("read file: ${file.canonicalPath} fail")
                    }
                    if (readSize >= BUFFER_SIZE) {
                        error("read file: ${file.canonicalPath} fail, file size greater than $BUFFER_SIZE bytes.")
                    }
                    val lines = String(buffer, 0, readSize, Charsets.UTF_8).lines().filter { it.isNotBlank() }.map { it.trim() }
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
                }
            } finally {
                bufferPool.put(bufferValue)
            }
            return result
        }

        /**
         * https://android.googlesource.com/kernel/common/+/a7827a2a60218b25f222b54f77ed38f57aebe08b/Documentation/cpuidle/sysfs.txt
         */
        private fun readCpuCoreIdleTimeInMicroseconds(cpuCoreIndex: Int, bufferPool: LruByteArrayPool): Long {
            val baseDir = File("/sys/devices/system/cpu/cpu$cpuCoreIndex/cpuidle")
            val bufferValue = bufferPool.get(BUFFER_SIZE)
            var result = 0L
            val childrenFiles = baseDir.listFiles() ?: emptyArray()
            var isFoundIdleFile = false
            try {
                for (c in childrenFiles) {
                    if (c.name.startsWith("state")) {
                        val file = File(c, "time")
                        file.inputStream().use { inputStream ->
                            val buffer = bufferValue.value
                            val readSize = inputStream.read(buffer)
                            if (readSize < 0) {
                                error("read file: ${file.canonicalPath} fail")
                            }
                            if (readSize >= BUFFER_SIZE) {
                                error("read file: ${file.canonicalPath} fail, file size greater than $BUFFER_SIZE bytes.")
                            }
                            result += String(buffer, 0, readSize, Charsets.UTF_8).trim().toLong()
                        }
                        isFoundIdleFile = true
                    }
                }
            } finally {
                bufferPool.put(bufferValue)
            }
            if (!isFoundIdleFile) {
                error("Didn't found cpu idle file.")
            }
            return result
        }

        private fun readCpuCoreIdleTime(cpuCoreIndex: Int, bufferPool: LruByteArrayPool): Long {
            return readCpuCoreIdleTimeInMicroseconds(cpuCoreIndex, bufferPool) / (1000L * oneJiffyInMillis)
        }

        private fun readCpuCoreSpeed(cpuCoreIndex: Int, bufferPool: LruByteArrayPool): Long {
            val file = File("/sys/devices/system/cpu/cpu$cpuCoreIndex/cpufreq/scaling_cur_freq")
            val bufferValue = bufferPool.get(BUFFER_SIZE)
            try {
                file.inputStream().use { inputStream ->
                    val buffer = bufferValue.value
                    val readSize = inputStream.read(buffer)
                    if (readSize < 0) {
                        error("read file: ${file.canonicalPath} fail")
                    }
                    if (readSize >= BUFFER_SIZE) {
                        error("read file: ${file.canonicalPath} fail, file size greater than $BUFFER_SIZE bytes.")
                    }
                    return String(buffer, 0, readSize, Charsets.UTF_8).trim().toLong()
                }
            } finally {
                bufferPool.put(bufferValue)
            }
        }

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