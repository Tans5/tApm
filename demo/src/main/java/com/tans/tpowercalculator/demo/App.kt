package com.tans.tpowercalculator.demo

import android.app.Application
import com.tans.tpowercalculator.tPowerCalculator

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        tPowerCalculator.init(this)

    }
}