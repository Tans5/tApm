package com.tans.tpowercalculator

import android.app.Application
import com.tans.tpowercalculator.internal.CpuStateSnapshotCapture
import com.tans.tpowercalculator.internal.monitors.CpuUsageMonitor
import com.tans.tpowercalculator.internal.Executors
import com.tans.tpowercalculator.internal.PowerProfile
import com.tans.tpowercalculator.internal.tPowerLog
import java.util.concurrent.atomic.AtomicReference

@Suppress("ClassName")
object tPowerCalculator {

    private val application: AtomicReference<Application?> = AtomicReference(null)

    private val powerProfile: AtomicReference<PowerProfile?> = AtomicReference(null)

    private val cpuStateSnapshotCapture: AtomicReference<CpuStateSnapshotCapture?> = AtomicReference(null)

    private val cpuUsageMonitor: AtomicReference<CpuUsageMonitor?> = AtomicReference(null)

    fun init(application: Application) {
        if (this.application.compareAndSet(null, application)) {
            Executors.bgExecutors.execute {

                tPowerLog.d(TAG, "Do init.")
                val powerProfile = PowerProfile.parsePowerProfile(application)
                this.powerProfile.set(powerProfile)
                val cpuStateSnapshotCapture = CpuStateSnapshotCapture(powerProfile)
                if (cpuStateSnapshotCapture.isInitSuccess) {
                    tPowerLog.d(TAG, "CpuStateSnapshotCapture init success.")
                } else {
                    tPowerLog.e(TAG, "CpuStateSnapshotCapture init fail.")
                }
                this.cpuStateSnapshotCapture.set(cpuStateSnapshotCapture)
                val cpuUsageMonitor = CpuUsageMonitor(cpuStateSnapshotCapture)
                if (cpuUsageMonitor.isSupport) {
                    cpuUsageMonitor.start()
                    tPowerLog.d(TAG, "CpuUsageMonitor init success.")
                } else {
                    tPowerLog.e(TAG, "CpuUsageMonitor not support.")
                }
                this.cpuUsageMonitor.set(cpuUsageMonitor)
            }
        } else {
            val msg = "Already init."
            tPowerLog.w(TAG, msg)
        }
    }

    internal fun getApplication(): Application {
        return application.get() ?: error("Do not init.")
    }

    private const val TAG = "tPowerCalculator"
}