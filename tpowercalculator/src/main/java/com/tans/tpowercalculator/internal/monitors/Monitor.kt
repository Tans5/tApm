package com.tans.tpowercalculator.internal.monitors

import com.tans.tpowercalculator.MonitorCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

interface Monitor<T : Any> {

    val isSupport: Boolean

    val isRunning: AtomicBoolean

    val monitorIntervalInMillis: AtomicLong

    val monitorCallback: AtomicReference<MonitorCallback<T>?>

    fun start() {
        if (isSupport) {
            if (isRunning.compareAndSet(false, true)) {
                onStart()
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

fun <T: Any> Monitor(defaultMonitorIntervalInMillis: Long) : Monitor<T> {
    return object : Monitor<T> {
        override val isSupport: Boolean = false
        override val isRunning: AtomicBoolean = AtomicBoolean(false)
        override val monitorIntervalInMillis: AtomicLong = AtomicLong(defaultMonitorIntervalInMillis)
        override val monitorCallback: AtomicReference<MonitorCallback<T>?> = AtomicReference(null)

        override fun onStart() {
        }

        override fun onStop() {

        }
    }
}