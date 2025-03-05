package com.tans.tapm

import android.app.Application
import com.tans.tapm.internal.CpuStateSnapshotCapture
import com.tans.tapm.internal.monitors.CpuUsageMonitor
import com.tans.tapm.internal.Executors
import com.tans.tapm.internal.PowerProfile
import com.tans.tapm.internal.monitors.CpuPowerCostMonitor
import com.tans.tapm.internal.tApmLog
import java.util.concurrent.atomic.AtomicReference

@Suppress("ClassName")
object tApm {

    private val application: AtomicReference<Application?> = AtomicReference(null)

    private val powerProfile: AtomicReference<PowerProfile?> = AtomicReference(null)

    private val cpuStateSnapshotCapture: AtomicReference<CpuStateSnapshotCapture?> = AtomicReference(null)

    private val cpuUsageMonitor: AtomicReference<CpuUsageMonitor?> = AtomicReference(null)

    private val cpuPowerCostMonitor: AtomicReference<CpuPowerCostMonitor?> = AtomicReference(null)

    fun init(application: Application) {
        if (this.application.compareAndSet(null, application)) {
            Executors.bgExecutors.execute {

                tApmLog.d(TAG, "Do init.")
                val powerProfile = PowerProfile.parsePowerProfile(application)
                if (powerProfile == null) {
                    tApmLog.e(TAG, "Init tPowerCalculator fail, can't parse power profile.")
                    return@execute
                }
                this.powerProfile.set(powerProfile)
                val cpuStateSnapshotCapture = CpuStateSnapshotCapture(powerProfile)
                if (cpuStateSnapshotCapture.isInitSuccess) {
                    tApmLog.d(TAG, "CpuStateSnapshotCapture init success.")
                } else {
                    tApmLog.e(TAG, "CpuStateSnapshotCapture init fail.")
                }
                this.cpuStateSnapshotCapture.set(cpuStateSnapshotCapture)
                val cpuUsageMonitor = CpuUsageMonitor(cpuStateSnapshotCapture)
                if (cpuUsageMonitor.isSupport) {
                    cpuUsageMonitor.start()
                    tApmLog.d(TAG, "CpuUsageMonitor init success.")
                } else {
                    tApmLog.e(TAG, "CpuUsageMonitor not support.")
                }
                this.cpuUsageMonitor.set(cpuUsageMonitor)

                val cpuPowerCostMonitor = CpuPowerCostMonitor(
                    powerProfile = powerProfile,
                    cpuStateSnapshotCapture = cpuStateSnapshotCapture
                )
                if (cpuPowerCostMonitor.isSupport) {
                    cpuPowerCostMonitor.start()
                    tApmLog.d(TAG, "CpuPowerCostMonitor init success.")
                } else {
                    tApmLog.e(TAG, "CpuPowerCostMonitor not support.")
                }
                this.cpuPowerCostMonitor.set(cpuPowerCostMonitor)
            }
        } else {
            val msg = "Already init."
            tApmLog.w(TAG, msg)
        }
    }

    internal fun getApplication(): Application {
        return application.get() ?: error("Do not init.")
    }

    private const val TAG = "tPowerCalculator"
}