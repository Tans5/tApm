package com.tans.tapm.demo

import android.app.Application
import com.tans.tapm.InitCallback
import com.tans.tapm.Monitor
import com.tans.tapm.formatDataTimeMs
import com.tans.tapm.model.Anr
import com.tans.tapm.model.CpuPowerCost
import com.tans.tapm.model.CpuUsage
import com.tans.tapm.model.ForegroundScreenPowerCost
import com.tans.tapm.model.JavaCrash
import com.tans.tapm.model.NativeCrash
import com.tans.tapm.monitors.AnrMonitor
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.monitors.HttpRequestMonitor
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.monitors.NativeCrashMonitor
import com.tans.tapm.tApm
import com.tans.tapm.toHumanReadablePercent
import com.tans.tuiutils.systembar.AutoApplySystemBarAnnotation
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class App : Application() {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor { s ->
                AppLog.d("HttpLogging", s)
            }.apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
            .addNetworkInterceptor(HttpRequestMonitor)
            .build()
    }

    val apm: tApm by lazy {
        tApm.Companion.Builder(this)
            // JavaCrash
            .addMonitorObserver(JavaCrashMonitor::class.java, object : Monitor.MonitorDataObserver<JavaCrash> {
                val TAG = "JavaCrash"
                override fun onMonitorDataUpdate(
                    t: JavaCrash,
                    apm: tApm
                ) {
                    AppLog.e(TAG, "JavaCrashed: ${t.error.message}, TraceFile: ${t.crashTraceFilePath}")
                }
            })
            // NativeCrash
            .addMonitorObserver(NativeCrashMonitor::class.java, object : Monitor.MonitorDataObserver<NativeCrash> {
                val TAG = "NativeCrash"
                override fun onMonitorDataUpdate(
                    t: NativeCrash,
                    apm: tApm
                ) {
                    AppLog.e(TAG, "NativeCrash, TraceFile: ${t.crashTraceFilePath}")
                }
            })
            // Anr
            .addMonitorObserver(AnrMonitor::class.java, object : Monitor.MonitorDataObserver<Anr> {
                val TAG = "Anr"
                override fun onMonitorDataUpdate(
                    t: Anr,
                    apm: tApm
                ) {
                    AppLog.e(TAG, "Receive anr signal, isSigFromMe: ${t.isSigFromMe}, anrTraceFile: ${t.anrTraceFile}")
                }
            })
            // CpuUsage
            .addMonitor(CpuUsageMonitor())
            .addMonitorObserver(CpuUsageMonitor::class.java, object : Monitor.MonitorDataObserver<CpuUsage> {
                val TAG = "CpuUsage"
                override fun onMonitorDataUpdate(
                    t: CpuUsage,
                    apm: tApm
                ) {
                    AppLog.d(TAG, "Start: ${t.startTimeInMillis.formatDataTimeMs()}, End: ${t.endTimeInMillis.formatDataTimeMs()} CpuUsage: ${t.avgCpuUsage.toHumanReadablePercent()}, CurrentProcessCpuUsage: ${t.currentProcessAvgCpuUsage.toHumanReadablePercent()}")
                }
            })
            // CpuPowerCost
            .addMonitor(CpuPowerCostMonitor())
            .addMonitorObserver(CpuPowerCostMonitor::class.java, object : Monitor.MonitorDataObserver<CpuPowerCost> {
                val TAG = "CpuPower"
                override fun onMonitorDataUpdate(
                    t: CpuPowerCost,
                    apm: tApm
                ) {
                    AppLog.d(TAG, t.toString())
                }
            })
            // ForegroundScreenPowerCost
            .addMonitor(ForegroundScreenPowerCostMonitor())
            .addMonitorObserver(ForegroundScreenPowerCostMonitor::class.java, object : Monitor.MonitorDataObserver<ForegroundScreenPowerCost> {
                val TAG = "ForegroundScreenPowerCost"
                override fun onMonitorDataUpdate(
                    t: ForegroundScreenPowerCost,
                    apm: tApm
                ) {
                    AppLog.d(TAG, t.toString())
                }
            })
            // Http Monitor
            .addMonitor(HttpRequestMonitor())
            .setInitCallback(object : InitCallback {
                val TAG = "ApmInit"

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
            .apply {
                getMonitor(CpuUsageMonitor::class.java)?.setMonitorInterval(1000L * 60L * 5L)
            }
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AutoApplySystemBarAnnotation.init(this)
        apm
    }
}