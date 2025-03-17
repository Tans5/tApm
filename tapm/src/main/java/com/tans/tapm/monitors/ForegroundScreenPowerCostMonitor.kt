package com.tans.tapm.monitors

import android.database.ContentObserver
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import com.tans.tapm.AppLifecycleOwner
import com.tans.tapm.internal.millisToHours
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.internal.toHumanReadablePercent
import com.tans.tapm.model.ForegroundScreenPowerCost
import com.tans.tapm.tApm
import java.util.concurrent.atomic.AtomicReference

class ForegroundScreenPowerCostMonitor : AbsMonitor<ForegroundScreenPowerCost>(FOREGROUND_SCREEN_POWER_COST_CHECK_INTERNAL) {

    override val isSupport: Boolean
        get() {
            val powerProfile = this.apm.get()?.powerProfile
            return if (powerProfile == null) {
                false
            } else {
                screenOnMa != 0.0f || screenFullMa != 0.0f
            }
        }

    private val screenOnMa: Float by lazy {
        powerProfile!!.screenProfile.let {
            if (it.onMa > 0.0f) {
                it.onMa
            } else {
                it.screensOnMa[0] ?: 0.0f
            }
        }
    }

    private val screenFullMa: Float by lazy {
        powerProfile!!.screenProfile.let {
            if (it.fullMa > 0.0f) {
                it.onMa
            } else {
                it.screensFullMa[0] ?: 0.0f
            }
        }
    }

    private val state: AtomicReference<State> = AtomicReference(State.Stopped)

    private val brightness: AtomicReference<Float?> = AtomicReference(null)

    // first: uptime; second: system time
    private val lastUpdateTime: AtomicReference<Pair<Long, Long>?> = AtomicReference(null)

    private val brightnessObserver: ContentObserver by lazy {
        object : ContentObserver(executor.backgroundThreadHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val brightness = getBrightness()
                tApmLog.d(TAG, "Screen brightness update: ${brightness.toHumanReadablePercent()}")
                calculateScreenPowerCostAndDispatch()
                this@ForegroundScreenPowerCostMonitor.brightness.set(brightness)
            }
        }
    }

    private val handler: Handler by lazy {
        object : Handler(executor.getBackgroundThreadLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    FOREGROUND_SCREEN_POWER_COST_CHECK_MSG -> {
                        calculateScreenPowerCostAndDispatch()
                    }
                }
            }
        }
    }

    override fun onInit(apm: tApm) {
        if (isSupport) {
            tApmLog.d(TAG, "Init ForegroundScreenPowerCostMonitor success.")
        } else {
            tApmLog.e(TAG, "Init ForegroundScreenPowerCostMonitor fail.")
        }
    }

    override fun onStart(apm: tApm) {
        if (hasBrightObserverPermission()) {
            if (AppLifecycleOwner.lifecycleState == AppLifecycleOwner.LifecycleState.Foreground) {
                if (state.compareAndSet(State.Stopped, State.Running)) {
                    registerTasks()
                    tApmLog.d(TAG, "ForegroundScreenPowerCostMonitor started.")
                }
            } else {
                if (state.compareAndSet(State.Stopped, State.Paused)) {
                    tApmLog.d(TAG, "ForegroundScreenPowerCostMonitor waiting app foreground.")
                }
            }
        } else {
            state.compareAndSet(State.Stopped, State.NoPermission)
            tApmLog.w(TAG, "ForegroundScreenPowerCostMonitor no permission.")
        }
    }

    override fun onStop(apm: tApm) {
        state.set(State.Stopped)
        unregisterTasks()
        tApmLog.d(TAG, "ForegroundScreenPowerCostMonitor stopped.")
    }

    override fun onAppForeground() {
        when (val state = this.state.get()) {
            State.NoPermission -> {
                if (hasBrightObserverPermission()) {
                    if (this.state.compareAndSet(State.NoPermission, State.Running)) {
                        tApmLog.d(TAG, "Got settings permission and running task.")
                        registerTasks()
                    }
                }
            }

            State.Paused -> {
                if (this.state.compareAndSet(State.Paused, State.Running)) {
                    registerTasks()
                }
            }

            State.Running, State.Stopped -> {
                tApmLog.e(TAG, "Wrong state: $state for onAppForeground() ")
            }
        }
    }

    override fun onAppBackground() {
        when (this.state.get()!!) {
            State.Running -> {
                if (this.state.compareAndSet(State.Running, State.Paused)) {
                    calculateScreenPowerCostAndDispatch()
                    unregisterTasks()
                }
            }

            else -> {}
        }
    }

    private fun hasBrightObserverPermission(): Boolean {
        return Settings.System.canWrite(application)
    }

    private fun registerTasks() {

        handler.removeMessages(FOREGROUND_SCREEN_POWER_COST_CHECK_MSG)
        handler.sendEmptyMessageDelayed(
            FOREGROUND_SCREEN_POWER_COST_CHECK_MSG,
            monitorIntervalInMillis.get()
        )

        application.contentResolver.unregisterContentObserver(brightnessObserver)
        application.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            brightnessObserver
        )
        lastUpdateTime.set(SystemClock.uptimeMillis() to System.currentTimeMillis())
        brightness.set(getBrightness())
    }

    private fun unregisterTasks() {
        handler.removeMessages(FOREGROUND_SCREEN_POWER_COST_CHECK_MSG)
        application.contentResolver.unregisterContentObserver(brightnessObserver)
        lastUpdateTime.set(null)
        brightness.set(null)
    }

    private fun getBrightness(): Float {
        // between 0 and 255
        val brightness = Settings.System.getInt(
            application.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        return brightness.toFloat() / 255.0f
    }

    private fun calculateScreenPowerCostAndDispatch() {
        val brightness = brightness.get()
        val lastUpdateTime = lastUpdateTime.get()
        if (brightness != null && lastUpdateTime != null) {
            val (lastUpdateUptime, lastUpdateSystemTime) = lastUpdateTime
            val currentUptime = SystemClock.uptimeMillis()
            val currentSystemTime = System.currentTimeMillis()
            val durationInHour = (currentUptime - lastUpdateUptime).millisToHours()
            val powerCostInMah = (screenOnMa + brightness * screenFullMa) * durationInHour
            this.lastUpdateTime.set(currentUptime to currentSystemTime)
            val powerCost = ForegroundScreenPowerCost(
                startTimeInMillis = lastUpdateSystemTime,
                endTimeInMillis = currentSystemTime,
                powerCostInMah = powerCostInMah
            )
            tApmLog.d(TAG, powerCost.toString())
            dispatchMonitorData(powerCost)
            handler.removeMessages(FOREGROUND_SCREEN_POWER_COST_CHECK_MSG)
            handler.sendEmptyMessageDelayed(
                FOREGROUND_SCREEN_POWER_COST_CHECK_MSG,
                monitorIntervalInMillis.get()
            )
        }
    }

    companion object {
        // 20 min
        private const val FOREGROUND_SCREEN_POWER_COST_CHECK_INTERNAL = 60L * 1000L * 20L

        private const val FOREGROUND_SCREEN_POWER_COST_CHECK_MSG = 0

        enum class State {
            Running,
            NoPermission,
            Paused,
            Stopped
        }

        private const val TAG = "ForegroundScreenPowerCostMonitor"
    }
}