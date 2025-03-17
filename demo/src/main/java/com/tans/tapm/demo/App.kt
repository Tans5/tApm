package com.tans.tapm.demo

import android.app.Application
import com.tans.tapm.InitCallback
import com.tans.tapm.Monitor
import com.tans.tapm.model.JavaCrash
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.tApm
import com.tans.tuiutils.systembar.AutoApplySystemBarAnnotation

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AutoApplySystemBarAnnotation.init(this)
        tApm.Companion.Builder(this)
            .addMonitorObserver(JavaCrashMonitor::class.java, object : Monitor.MonitorDataObserver<JavaCrash> {
                override fun onMonitorDataUpdate(
                    t: JavaCrash,
                    apm: tApm
                ) {
                    AppLog.e(TAG, "Crashed: ${t.error.message}", t.error)
                }
            })
            .setInitCallback(object : InitCallback {
                override fun onSupportMonitor(monitor: Monitor<*>) {
                    AppLog.d(TAG, "Support: ${monitor::class.java}")
                }

                override fun onUnsupportMonitor(monitor: Monitor<*>) {
                    AppLog.e(TAG, "UnSupport: ${monitor::class.java}")
                }

                override fun onInitFinish() {
                    AppLog.d(TAG, "Init tApm finished.")
                }
            })
            .build()
    }

    companion object {
        private const val TAG = "App"
    }
}