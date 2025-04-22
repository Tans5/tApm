package com.tans.tapm.log

import android.content.Context
import androidx.annotation.Keep
import androidx.startup.Initializer
import com.tans.tapm.autoinit.tApmAutoInit
import com.tans.tapm.monitors.AnrMonitor
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.monitors.HttpRequestMonitor
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.monitors.MainThreadLagMonitor
import com.tans.tapm.monitors.NativeCrashMonitor

@Suppress("ClassName")
@Keep
class tApmLogAutoInit : Initializer<Unit> {

    override fun create(context: Context) {
        tApmAutoInit.addInitFinishListener { apm ->
            tApmLogs.init(apm.cacheBaseDir)
            apm.getMonitor(JavaCrashMonitor::class.java)?.addMonitorObserver(JavaCrashLogObserver)
            apm.getMonitor(NativeCrashMonitor::class.java)?.addMonitorObserver(NativeCrashLogObserver)
            apm.getMonitor(AnrMonitor::class.java)?.addMonitorObserver(AnrLogObserver)
            apm.getMonitor(HttpRequestMonitor::class.java)?.addMonitorObserver(HttpRequestLogObserver)
            apm.getMonitor(CpuUsageMonitor::class.java)?.addMonitorObserver(CpuUsageLogObserver)
            apm.getMonitor(CpuPowerCostMonitor::class.java)?.addMonitorObserver(CpuPowerCostLogObserver)
            apm.getMonitor(ForegroundScreenPowerCostMonitor::class.java)?.addMonitorObserver(ForegroundScreenPowerCostLogObserver)
            apm.getMonitor(MainThreadLagMonitor::class.java)?.addMonitorObserver(MainThreadLagLogObserver)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()
}