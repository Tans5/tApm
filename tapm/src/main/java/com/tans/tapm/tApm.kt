package com.tans.tapm

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.HandlerThread
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.DeviceInfo
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("ClassName")
class tApm private constructor(
    val application: Application,
    val monitors: Map<Class<out Monitor<*>>, Monitor<*>>,
    val executor: Executor
) {
    @Volatile
    var powerProfile: PowerProfile? = null
        private set

    @Volatile
    var cpuStateSnapshotCapture: CpuStateSnapshotCapture? = null
        private set

    @Volatile
    var deviceInfo: DeviceInfo? = null

    init {
        AppLifecycleOwner.init(this)
        executor.executeOnBackgroundThread {
            powerProfile = PowerProfile.parsePowerProfile(application)
            if (powerProfile == null) {
                tApmLog.e(TAG, "Parse power profile fail.")
            }
            CpuStateSnapshotCapture(powerProfile).apply {
                if (this.isInitSuccess) {
                    this@tApm.cpuStateSnapshotCapture = this
                } else {
                    tApmLog.e(TAG, "Do not support read cpu snapshot state.")
                }
            }
            deviceInfo = obtainDeviceInfo()
            tApmLog.d(TAG, deviceInfo.toString())
            for ((_, v) in monitors) {
                v.init(this)
            }
            for ((_, v) in monitors) {
                if (v.isSupport) {
                    v.start()
                }
            }
            tApmLog.d(TAG, "tApm inited.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Monitor<D>, D: Any> addMonitorObserver(monitorClass: Class<M>, observer: Monitor.MonitorDataObserver<D>) {
        (monitors[monitorClass] as? M)?.addMonitorObserver(observer)
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Monitor<D>, D: Any> removeMonitorObserver(monitorClass: Class<M>, observer: Monitor.MonitorDataObserver<D>) {
        (monitors[monitorClass] as? M)?.removeMonitorObserver(observer)
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Monitor<D>, D: Any> getMonitor(monitorClass: Class<M>): M? {
        return (monitors[monitorClass] as? M)
    }

    private fun obtainDeviceInfo(): DeviceInfo {
         return DeviceInfo(
            deviceName = "${Build.BRAND}-${Build.MODEL}",
             apiLevel = Build.VERSION.SDK_INT,
             androidName = when (Build.VERSION.SDK_INT) {
                 24 -> "Android 7.0"
                 25 -> "Android 7.1"
                 26 -> "Android 8.0"
                 27 -> "Android 8.1"
                 28 -> "Android 9"
                 29 -> "Android 10"
                 30 -> "Android 11"
                 31 -> "Android 12.0"
                 32 -> "Android 12.1"
                 33 -> "Android 13"
                 34 -> "Android 14"
                 35 -> "Android 15"
                 else -> "Unknown"
             },
             cpuCoreSize = powerProfile?.cpuProfile?.coreCount ?: Runtime.getRuntime().availableProcessors(),
             cpuClusters = powerProfile?.cpuProfile?.cluster?.map {
                 DeviceInfo.Companion.CpuCluster(
                     cpuCoreSize = it.coreCount,
                     minSpeedInKHz = it.frequencies.getOrNull(0)?.speedKhz ?: 0,
                     maxSpeedInKHz = it.frequencies.lastOrNull()?.speedKhz ?: 0,
                 )
             } ?: emptyList(),
             cpuSupportAbi = Build.SUPPORTED_ABIS.joinToString(prefix = "[", postfix = "]", separator = ","),
             memorySizeInBytes = application.let {
                 val memInfo = ActivityManager.MemoryInfo()
                 try {
                     val activityManager = it.getSystemService(ActivityManager::class.java)
                     activityManager.getMemoryInfo(memInfo)
                     memInfo.totalMem
                 } catch (_: Throwable) {
                     0L
                 }
             },
             jvmMaxMemorySizeInBytes = Runtime.getRuntime().maxMemory()
        )
    }

    companion object {

        private const val TAG = "tApm"

        private val isCreatedApmInstance: AtomicBoolean = AtomicBoolean(false)

        class Builder(private val application: Application) {

            private val monitors: MutableMap<Class<out Monitor<*>>, Monitor<*>> = mutableMapOf(
                CpuUsageMonitor::class.java to CpuUsageMonitor(),
                CpuPowerCostMonitor::class.java to CpuPowerCostMonitor(),
                ForegroundScreenPowerCostMonitor::class.java to ForegroundScreenPowerCostMonitor()
            )

            private var backgroundThread: HandlerThread? = null

            fun addMonitor(monitor: Monitor<*>): Builder {
                monitors[monitor::class.java] = monitor
                return this
            }

            fun removeMonitor(monitorClass: Class<out Monitor<*>>): Builder {
                monitors.remove(monitorClass)
                return this
            }

            @Suppress("UNCHECKED_CAST")
            fun <M : Monitor<D>, D: Any> addMonitorObserver(monitorClass: Class<M>, observer: Monitor.MonitorDataObserver<D>) {
                (monitors[monitorClass] as? M)?.addMonitorObserver(observer)
            }

            @Suppress("UNCHECKED_CAST")
            fun <M : Monitor<D>, D: Any> removeMonitorObserver(monitorClass: Class<M>, observer: Monitor.MonitorDataObserver<D>) {
                (monitors[monitorClass] as? M)?.removeMonitorObserver(observer)
            }

            fun setBackgroundThread(backgroundThread: HandlerThread): Builder {
                this.backgroundThread = backgroundThread
                return this
            }

            fun build(): tApm {
                return if (isCreatedApmInstance.compareAndSet(false, true)) {
                    tApm(
                        application = application,
                        monitors = monitors,
                        executor = Executor(backgroundThread = backgroundThread)
                    )
                } else {
                    error("Already created tApm instance.")
                }

            }
        }
    }
}