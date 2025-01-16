package com.tans.tpowercalculator

import android.app.Application
import com.tans.tpowercalculator.internal.CpuStateSnapshotCapture
import com.tans.tpowercalculator.internal.Executors
import com.tans.tpowercalculator.internal.PowerProfile
import com.tans.tpowercalculator.internal.tPowerLog
import java.util.concurrent.atomic.AtomicReference

object tPowerCalculator {

    private val application: AtomicReference<Application?> = AtomicReference()

    fun init(application: Application, callback: SimpleCallback? = null) {
        if (this.application.compareAndSet(null, application)) {
            Executors.bgExecutors.execute {
                tPowerLog.d(TAG, "Do init.")
                runCatching {
                    val powerProfile = PowerProfile.parsePowerProfile(application)
                    val cpuStateSnapshotCapture = CpuStateSnapshotCapture(powerProfile)
                }.onSuccess {
                    tPowerLog.d(TAG, "Init success!!")
                    callback?.onSuccess()
                }.onFailure {
                    tPowerLog.e(TAG, "Init fail.", it)
                    callback?.onFail(it.message ?: "", it)
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