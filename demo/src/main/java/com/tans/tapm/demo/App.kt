package com.tans.tapm.demo

import android.app.Application
import com.tans.tapm.InitCallback
import com.tans.tapm.Monitor
import com.tans.tapm.breakpad.BreakpadNativeCrashMonitor
import com.tans.tapm.breakpad.model.BreakpadNativeCrash
import com.tans.tapm.formatDataTime
import com.tans.tapm.model.Anr
import com.tans.tapm.model.CpuPowerCost
import com.tans.tapm.model.CpuUsage
import com.tans.tapm.model.ForegroundScreenPowerCost
import com.tans.tapm.model.JavaCrash
import com.tans.tapm.monitors.AnrMonitor
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.tApm
import com.tans.tapm.toHumanReadablePercent
import com.tans.tlrucache.disk.DiskLruCache
import com.tans.tuiutils.systembar.AutoApplySystemBarAnnotation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class App : Application() {

    val apm: tApm by lazy {
        tApm.Companion.Builder(this)
            // JavaCrash
            .addMonitorObserver(JavaCrashMonitor::class.java, object : Monitor.MonitorDataObserver<JavaCrash> {
                val TAG = "JavaCrash"
                override fun onMonitorDataUpdate(
                    t: JavaCrash,
                    apm: tApm
                ) {
                    AppLog.e(TAG, "Crashed: ${t.error.message}", t.error)
                    AppLog.flushLog()
                }
            })
            // CpuUsage
            .addMonitorObserver(CpuUsageMonitor::class.java, object : Monitor.MonitorDataObserver<CpuUsage> {
                val TAG = "CpuUsage"
                override fun onMonitorDataUpdate(
                    t: CpuUsage,
                    apm: tApm
                ) {
                    AppLog.d(TAG, "Start: ${t.startTimeInMillis.formatDataTime()}, End: ${t.endTimeInMillis.formatDataTime()} CpuUsage: ${t.avgCpuUsage.toHumanReadablePercent()}, CurrentProcessCpuUsage: ${t.currentProcessAvgCpuUsage.toHumanReadablePercent()}")
                }
            })
            // CpuPowerCost
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
            .addMonitorObserver(ForegroundScreenPowerCostMonitor::class.java, object : Monitor.MonitorDataObserver<ForegroundScreenPowerCost> {
                val TAG = "ForegroundScreenPowerCost"
                override fun onMonitorDataUpdate(
                    t: ForegroundScreenPowerCost,
                    apm: tApm
                ) {
                    AppLog.d(TAG, t.toString())
                }
            })
            // Anr
            .addMonitorObserver(AnrMonitor::class.java, object : Monitor.MonitorDataObserver<Anr> {
                val TAG = "Anr"
                override fun onMonitorDataUpdate(
                    t: Anr,
                    apm: tApm
                ) {
                    AppLog.e(TAG, "Receive anr signal, time=${t.time.formatDataTime()}, isSigFromMe: ${t.isSigFromMe}")
                    try {
                        AppLog.d(TAG, "Start write anr trace file.")
                        val diskCache = DiskLruCache.open(
                            directory = File(this@App.getExternalFilesDir(null), "Anr"),
                            appVersion = 1,
                            valueCount = 1,
                            maxSize = 1024 * 1024 * 30, // 30MB
                        )

                        diskCache.use {
                            val format = SimpleDateFormat("yyyy-MM-dd_hh:mm:ss.SSS", Locale.US).format(t.time)
                            val timeStr = format.format(t.time)
                            val key = if (!t.isSigFromMe) {
                                "${timeStr}_anr"
                            } else {
                                timeStr
                            }
                            val editor = it.edit(key)!!
                            editor.getFile(0).outputStream().use { os ->
                                os.write(t.anrTraceData.toByteArray(Charsets.UTF_8))
                            }
                        }

                        AppLog.d(TAG, "Write anr trace file success.")
                    } catch (e: Throwable) {
                        AppLog.e(TAG, "Write anr trace fail fail.", e)
                    }
                }
            })
            .addMonitor(BreakpadNativeCrashMonitor())
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