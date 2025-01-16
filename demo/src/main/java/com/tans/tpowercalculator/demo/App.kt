package com.tans.tpowercalculator.demo

import android.app.Application
import com.tans.tpowercalculator.SimpleCallback
import com.tans.tpowercalculator.tPowerCalculator

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        tPowerCalculator.init(this, object : SimpleCallback {

            override fun onSuccess() {
                AppLog.d(TAG, "Init tPowerCalculator success.")
            }

            override fun onFail(msg: String, throwable: Throwable?) {
                AppLog.e(TAG, "Init tPowerCalculator fail: $msg", throwable)
            }
        })

    }

    companion object {

        private const val TAG = "App"
    }
}