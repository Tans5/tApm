package com.tans.tpowercalculator

import android.app.Application
import com.tans.tpowercalculator.internal.CpuStateSnapshotCapture
import com.tans.tpowercalculator.internal.CpuUsageMonitor
import com.tans.tpowercalculator.internal.Executors
import com.tans.tpowercalculator.internal.PowerProfile
import com.tans.tpowercalculator.internal.tPowerLog
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Suppress("ClassName")
object tPowerCalculator {

    private val application: AtomicReference<Application?> = AtomicReference(null)

    private val isInitSuccess: AtomicBoolean = AtomicBoolean(false)

    private val powerProfile: AtomicReference<PowerProfile?> = AtomicReference(null)

    private val cpuStateSnapshotCapture: AtomicReference<CpuStateSnapshotCapture?> = AtomicReference(null)

    private val cpuUsageMonitor: AtomicReference<CpuUsageMonitor?> = AtomicReference(null)

    fun init(application: Application, callback: SimpleCallback? = null) {
        if (this.application.compareAndSet(null, application)) {
            Executors.bgExecutors.execute {
                fun success() {
                    tPowerLog.d(TAG, "Init success!!")
                    isInitSuccess.set(true)
                    callback?.onSuccess()
                }

                fun fail(msg: String) {
                    tPowerLog.e(TAG, msg)
                    isInitSuccess.set(false)
                    callback?.onFail(msg)
                }

                tPowerLog.d(TAG, "Do init.")
                val powerProfile = PowerProfile.parsePowerProfile(application)
                this.powerProfile.set(powerProfile)
                val cpuStateSnapshotCapture = CpuStateSnapshotCapture(powerProfile)
                if (cpuStateSnapshotCapture.isInitSuccess) {
                    this.cpuStateSnapshotCapture.set(cpuStateSnapshotCapture)
                    val cpuUsageMonitor = CpuUsageMonitor(cpuStateSnapshotCapture)
                    this.cpuUsageMonitor.set(cpuUsageMonitor)
                    success()
                } else {
                    fail("CpuStateSnapshotCapture init fail.")
                }
            }
        } else {
            val msg = "Already init."
            tPowerLog.w(TAG, msg)
            callback?.onFail(msg)
        }
    }

    private const val TAG = "tPowerCalculator"
}