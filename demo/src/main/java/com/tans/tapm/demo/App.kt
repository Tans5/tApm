package com.tans.tapm.demo

import android.app.Application
import android.content.Context
import com.tans.tapm.autoinit.tApmAutoInit
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.monitors.HttpRequestMonitor
import com.tans.tapm.monitors.MainThreadLagMonitor
import com.tans.tapm.monitors.MemoryUsageMonitor
import com.tans.tuiutils.systembar.AutoApplySystemBarAnnotation
import okhttp3.OkHttpClient

class App : Application() {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpRequestMonitor)
            // .addNetworkInterceptor(HttpRequestMonitor)
            .build()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        tApmAutoInit.addBuilderInterceptor { builder ->
            builder
                // CpuUsage
                .addMonitor(CpuUsageMonitor().apply { setMonitorInterval(1000L * 10) })
                // CpuPowerCost
                .addMonitor(CpuPowerCostMonitor())
                // ForegroundScreenPowerCost
                .addMonitor(ForegroundScreenPowerCostMonitor())
                // Http
                .addMonitor(HttpRequestMonitor())
                // MainThreadLag
                .addMonitor(MainThreadLagMonitor())
                // MemoryUsage
                .addMonitor(MemoryUsageMonitor().apply { setMonitorInterval(5000L) })
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AutoApplySystemBarAnnotation.init(this)
    }
}