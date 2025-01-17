package com.tans.tpowercalculator.internal

import android.os.Process
import java.io.File
internal class CpuStateSnapshotCapture(
    private val powerProfile: PowerProfile
) {
    init {
        tPowerLog.d(TAG, "Init CpuStateSnapshotCapture success.")
    }

    companion object {

        private const val TAG = "CpuStateSnapshotCapture"

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

        private fun readCurrentProcessCpuCoreTime(): List<List<Pair<Long, Long>>> = readProcessCpuCoreTime(Process.myPid())

        private fun readProcessCpuCoreTime(pid: Int): List<List<Pair<Long, Long>>> {
            val f = File("/proc/$pid/time_in_state")
            val lines = f.inputStream().bufferedReader(Charsets.UTF_8).use {
                it.readLines()
            }
            val result = mutableListOf<List<Pair<Long, Long>>>()
            var parsingCpu: ArrayList<Pair<Long, Long>>? = null
            for (l in lines) {
                if (l.startsWith("cpu")) {
                    if (parsingCpu != null) {
                        result.add(parsingCpu)
                    }
                    parsingCpu = ArrayList()
                    continue
                }
                l.split(" ").let {
                    parsingCpu!!.add(it[0].toLong() to it[1].toLong())
                }
            }
            if (parsingCpu != null) {
                result.add(parsingCpu)
            }
            return result
        }
    }
}