package com.tans.tpowercalculator.internal

import android.os.Handler
import android.os.Message
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

internal class CpuUsageMonitor(
    private val cpuStateSnapshotCapture: CpuStateSnapshotCapture
) {

    private val lastCpuStateSnapshot: AtomicReference<CpuStateSnapshotCapture.Companion.CpuStateSnapshot?> = AtomicReference(null)

    private val handler: Handler = object : Handler(Executors.bgHandlerThread.looper) {

        override fun handleMessage(msg: Message) {
            val lastCpuState = lastCpuStateSnapshot.get()
            val currentCpuState = cpuStateSnapshotCapture.createCpuStateSnapshot()
            if (lastCpuState == null) {
                lastCpuStateSnapshot.set(currentCpuState)
                sendNextTimeCheckTask()
                return
            }
            val stateDurationInMillis = currentCpuState!!.createTime - lastCpuState.createTime

            // Check if cpu idle time.
            for ((index, cc) in currentCpuState.coreStates.withIndex()) {
                val lc = lastCpuState.coreStates[index]
                if (lc.cpuIdleTime == cc.cpuIdleTime && cc.cpuSpeed.currentSpeedInHz < cc.cpuSpeed.maxSpeedInHz) {
                    tPowerLog.e(TAG, "Skip cpu usage calculate, cpuCoreIndex=${cc.coreIndex}, idle time do not update, and currentCpuSpeed=${cc.cpuSpeed.currentSpeedInHz.toHumanReadableCpuSpeed()} maxCpuSpeed=${cc.cpuSpeed.maxSpeedInHz.toHumanReadableCpuSpeed()}")
                    sendNextTimeCheckTask()
                    return
                }
                val idleDurationInMillis = (cc.cpuIdleTime - lc.cpuIdleTime) * CpuStateSnapshotCapture.oneJiffyInMillis
                if (idleDurationInMillis > stateDurationInMillis) {
                    tPowerLog.e(TAG, "Skip cpu usage calculate, cpuCoreIndex=${cc.coreIndex}, idleDuration=${idleDurationInMillis}, stateDuration=${stateDurationInMillis}")
                    lastCpuStateSnapshot.set(currentCpuState)
                    sendNextTimeCheckTask()
                    return
                }
            }
            val cpuUsage = cpuStateSnapshotCapture.calculateCpuUsage(lastCpuState, currentCpuState)
            tPowerLog.d(TAG, "------------------------------------------")
            tPowerLog.d(TAG, "CpuAvgUsage: ${cpuUsage.avgCpuUsage.toHumanReadableCpuUsage()}, CurrentProcessCpuAvgUsage: ${cpuUsage.currentProcessAvgCpuUsage.toHumanReadableCpuUsage()}")
            for (usage in cpuUsage.cpuCoresUsage) {
                tPowerLog.d(TAG, "CpuIndex: ${usage.coreIndex}, Usage=${usage.cpuUsage.toHumanReadableCpuUsage()}, CurrentSpeed=${usage.speed.currentSpeedInHz.toHumanReadableCpuSpeed()}, MaxSpeed=${usage.speed.maxSpeedInHz.toHumanReadableCpuSpeed()}, MinSpeed=${usage.speed.minSpeedInHz.toHumanReadableCpuSpeed()}")
            }
            tPowerLog.d(TAG, "------------------------------------------")
            lastCpuStateSnapshot.set(currentCpuState)
            sendNextTimeCheckTask()
        }

        fun sendNextTimeCheckTask() {
            removeMessages(CPU_USAGE_CHECK_MSG)
            sendEmptyMessageDelayed(CPU_USAGE_CHECK_MSG, CPU_USAGE_CHECK_INTERNAL)
        }

    }

    init {
        handler.sendEmptyMessage(CPU_USAGE_CHECK_MSG)
        tPowerLog.d(TAG, "Init CpuUsageMonitor success.")
    }


    companion object {
        private const val TAG = "CpuUsageMonitor"
        // 1.5s
        private const val CPU_USAGE_CHECK_INTERNAL = 1500L

        private const val CPU_USAGE_CHECK_MSG = 0

        fun Long.toHumanReadableCpuSpeed(): String {
            return String.format(Locale.US,"%.1f GHz", this.toDouble() / 1_000_000.0)
        }

        fun Double.toHumanReadableCpuUsage(): String {
            return String.format(Locale.US, "%.1f", this * 100.0)
        }
    }
}