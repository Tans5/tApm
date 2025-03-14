package com.tans.tapm

import android.app.Application
import android.os.HandlerThread
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.internal.tApmLog
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
            for ((_, v) in monitors) {
                v.init(this)
            }
            for ((_, v) in monitors) {
                if (v.isSupport) {
                    v.start()
                }
            }
        }
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