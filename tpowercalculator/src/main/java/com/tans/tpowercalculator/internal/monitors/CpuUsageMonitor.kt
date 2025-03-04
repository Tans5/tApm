package com.tans.tpowercalculator.internal.monitors

import android.os.Handler
import android.os.Message
import com.tans.tpowercalculator.internal.CpuStateSnapshotCapture
import com.tans.tpowercalculator.internal.CpuStateSnapshotCapture.Companion.CpuStateSnapshot
import com.tans.tpowercalculator.internal.Executors
import com.tans.tpowercalculator.internal.tPowerLog
import com.tans.tpowercalculator.internal.toHumanReadableCpuUsage
import com.tans.tpowercalculator.internal.toHumanReadableCpuSpeed
import com.tans.tpowercalculator.model.CpuUsage
import com.tans.tpowercalculator.model.ProgressSingleCpuCoreUsage
import com.tans.tpowercalculator.model.SingleCpuCoreUsage
import java.util.concurrent.atomic.AtomicReference

internal class CpuUsageMonitor(
    private val cpuStateSnapshotCapture: CpuStateSnapshotCapture
) : Monitor<CpuUsage> by Monitor(CPU_USAGE_CHECK_INTERNAL) {

    override val isSupport: Boolean
        get() {
            return cpuStateSnapshotCapture.isInitSuccess
        }

    private val lastCpuStateSnapshot: AtomicReference<CpuStateSnapshot?> = AtomicReference(null)

    private val handler: Handler by lazy {
        object : Handler(Executors.bgHandlerThread.looper) {

            override fun handleMessage(msg: Message) {
                val lastCpuState = lastCpuStateSnapshot.get()
                val currentCpuStateBuffer = cpuStateSnapshotCapture.readCpuStateSnapshotBuffer()!!
                val currentCpuState =
                    cpuStateSnapshotCapture.parseCpuStateSnapshotBuffer(currentCpuStateBuffer)
                if (lastCpuState == null) {
                    lastCpuStateSnapshot.set(currentCpuState)
                    sendNextTimeCheckTask()
                    return
                }

                val cpuUsage = calculateCpuUsage(lastCpuState, currentCpuState)
                tPowerLog.d(TAG, "------------------------------------------")
                tPowerLog.d(
                    TAG,
                    "CpuAvgUsage: ${cpuUsage.avgCpuUsage.toHumanReadableCpuUsage()}, CurrentProcessCpuAvgUsage: ${cpuUsage.currentProcessAvgCpuUsage.toHumanReadableCpuUsage()}"
                )
                for (usage in cpuUsage.cpuCoresUsage) {
                    tPowerLog.d(
                        TAG,
                        "CpuIndex: ${usage.coreIndex}, Usage=${usage.cpuUsage.toHumanReadableCpuUsage()}, CurrentSpeed=${usage.currentCoreSpeedInKHz.toHumanReadableCpuSpeed()}, MaxSpeed=${usage.coreSpec.maxSpeedInKHz.toHumanReadableCpuSpeed()}, MinSpeed=${usage.coreSpec.minSpeedInKHz.toHumanReadableCpuSpeed()}"
                    )
                }
                tPowerLog.d(TAG, "------------------------------------------")
                lastCpuStateSnapshot.set(currentCpuState)
                sendNextTimeCheckTask()
                updateMonitorData(cpuUsage)
            }


            fun sendNextTimeCheckTask() {
                removeMessages(CPU_USAGE_CHECK_MSG)
                sendEmptyMessageDelayed(CPU_USAGE_CHECK_MSG, monitorIntervalInMillis.get())
            }

        }
    }


    override fun onStart() {
        handler.removeMessages(CPU_USAGE_CHECK_MSG)
        handler.sendEmptyMessage(CPU_USAGE_CHECK_MSG)
        tPowerLog.d(TAG, "CpuUsageMonitor started.")
    }

    override fun onStop() {
        handler.removeMessages(CPU_USAGE_CHECK_MSG)
        tPowerLog.d(TAG, "CpuUsageMonitor stopped.")
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
            val coreSpec = cpuStateSnapshotCapture.cpuSpeedSpecs[ns.coreIndex]
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
        val maxCpuSpeed =
            cpuStateSnapshotCapture.cpuSpeedSpecs.maxBy { it.maxSpeedInKHz }.maxSpeedInKHz
        var avgCpuUsageNum = 0.0
        var avgCpuUsageDen = 0.0
        for (usage in cpuCoreUsages) {
            val coreSpec = cpuStateSnapshotCapture.cpuSpeedSpecs[usage.coreIndex]
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

    companion object {
        private const val TAG = "CpuUsageMonitor"

        // 2s
        private const val CPU_USAGE_CHECK_INTERNAL = 2000L

        private const val CPU_USAGE_CHECK_MSG = 0
    }
}