package com.tans.tpowercalculator

import android.app.Application
import com.tans.tpowercalculator.internal.CpuStateSnapshotCapture
import com.tans.tpowercalculator.internal.Executors
import com.tans.tpowercalculator.internal.PowerProfile
import com.tans.tpowercalculator.internal.tPowerLog
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object tPowerCalculator {

    private val application: AtomicReference<Application?> = AtomicReference()

    private val isInitSuccess: AtomicBoolean = AtomicBoolean(false)

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
                val cpuStateSnapshotCapture = CpuStateSnapshotCapture(powerProfile)
                if (cpuStateSnapshotCapture.isInitSuccess) {
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