package com.tans.tapm.internal.monitors

import com.tans.tapm.MonitorCallback
import com.tans.tapm.internal.AppLifecycleOwner
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal interface Monitor<T : Any> : AppLifecycleOwner.AppLifecycleObserver {

    val isSupport: Boolean

    val isRunning: AtomicBoolean

    val monitorIntervalInMillis: AtomicLong

    val monitorCallback: AtomicReference<MonitorCallback<T>?>

    fun start() {
        if (isSupport) {
            if (isRunning.compareAndSet(false, true)) {
                onStart()
                AppLifecycleOwner.addLifecycleObserver(this)
            }
        }
    }

    fun onStart()

    fun setMonitorInterval(timeInMillis: Long) {
        if (timeInMillis > 0L) {
            monitorIntervalInMillis.set(timeInMillis)
        }
    }

    fun stop() {
        if (isSupport) {
            if (isRunning.compareAndSet(true ,false)) {
                onStop()
                AppLifecycleOwner.removeLifecycleObserver(this)
            }
        }
    }

    fun onStop()

    fun setMonitorCallback(callback: MonitorCallback<T>?) {
        this.monitorCallback.set(callback)
    }

    fun updateMonitorData(t: T) {
        this.monitorCallback.get()?.updateData(t)
    }
}