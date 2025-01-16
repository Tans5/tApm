package com.tans.tpowercalculator.internal

import android.os.Process
import java.io.File
internal class CpuStateSnapshotCapture(
    private val powerProfile: PowerProfile
) {
    init {
        check()
        tPowerLog.d(TAG, "Init CpuStateSnapshotCapture success.")
    }


    private fun check() {

        for (cpuIndex in 0 until powerProfile.cpuCoreSize) {
            val cluster = powerProfile.getCpuClusterByCpuIndex(cpuIndex)
            val speedAndTime = readCpuCoreTime(cpuIndex)
            if (cluster.cpuCoreSpeed.size != speedAndTime.size) {
                error("Wrong core time size ${speedAndTime.size}，need ${cluster.cpuCoreSpeed.size}.")
            }
            for (i in speedAndTime.indices) {
                val clusterSpeed = cluster.cpuCoreSpeed[i]
                val readerSpeed = speedAndTime[i].first
                if (clusterSpeed != readerSpeed) {
                    error("Wrong cpu speed $readerSpeed, need $clusterSpeed.")
                }
            }
        }

        val currentProcessTime = readCurrentProcessCpuCoreTime()
        println(currentProcessTime)
        // TODO: Check
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