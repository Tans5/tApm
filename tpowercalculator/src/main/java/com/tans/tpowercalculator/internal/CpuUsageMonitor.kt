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
            val currentCpuStateBuffer = cpuStateSnapshotCapture.readCpuStateSnapshotBuffer()!!
            val currentCpuState = cpuStateSnapshotCapture.parseCpuStateSnapshotBuffer(currentCpuStateBuffer)
            if (lastCpuState == null) {
                lastCpuStateSnapshot.set(currentCpuState)
                sendNextTimeCheckTask()
                return
            }

            val cpuUsage = cpuStateSnapshotCapture.calculateCpuUsage(lastCpuState, currentCpuState)
            tPowerLog.d(TAG, "------------------------------------------")
            tPowerLog.d(TAG, "CpuAvgUsage: ${cpuUsage.avgCpuUsage.toHumanReadableCpuUsage()}, CurrentProcessCpuAvgUsage: ${cpuUsage.currentProcessAvgCpuUsage.toHumanReadableCpuUsage()}")
            for (usage in cpuUsage.cpuCoresUsage) {
                tPowerLog.d(TAG, "CpuIndex: ${usage.coreIndex}, Usage=${usage.cpuUsage.toHumanReadableCpuUsage()}, CurrentSpeed=${usage.currentCoreSpeedInKHz.toHumanReadableCpuSpeed()}, MaxSpeed=${usage.coreSpec.maxSpeedInKHz.toHumanReadableCpuSpeed()}, MinSpeed=${usage.coreSpec.minSpeedInKHz.toHumanReadableCpuSpeed()}")
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

        // 2s
        private const val CPU_USAGE_CHECK_INTERNAL = 2000L

        private const val CPU_USAGE_CHECK_MSG = 0

        fun Long.toHumanReadableCpuSpeed(): String {
            return String.format(Locale.US,"%.2f GHz", this.toDouble() / 1_000_000.0)
        }

        fun Double.toHumanReadableCpuUsage(): String {
            return String.format(Locale.US, "%.1f", this * 100.0) + " %"
        }
    }
}