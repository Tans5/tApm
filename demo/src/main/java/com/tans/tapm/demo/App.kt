package com.tans.tapm.demo

import android.app.Application
import com.tans.tapm.tApm

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        tApm.Companion.Builder(this).build()
    }
}