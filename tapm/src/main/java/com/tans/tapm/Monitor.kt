package com.tans.tapm

import android.app.Application
import android.os.Looper
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

interface Monitor<T : Any> : AppLifecycleOwner.AppLifecycleObserver {

    val apm: AtomicReference<tApm?>

    val application: Application

    val executor: Executor

    val isSupport: Boolean

    val isRunning: AtomicBoolean

    val monitorIntervalInMillis: AtomicLong

    val monitorDataObservers: LinkedBlockingQueue<MonitorDataObserver<T>>

    fun init(apm: tApm) {
        if (this.apm.compareAndSet(null, apm)) {
            onInit(apm)
        } else {
            error("${this::class.java} already invoked init().")
        }
    }

    fun onInit(apm: tApm)

    fun start() {
        val apm = this.apm.get()
        if (isSupport && apm != null) {
            if (isRunning.compareAndSet(false, true)) {
                onStart(apm)
                AppLifecycleOwner.addLifecycleObserver(this)
            }
        }
    }

    fun onStart(apm: tApm)

    fun setMonitorInterval(timeInMillis: Long) {
        if (timeInMillis > 0L) {
            monitorIntervalInMillis.set(timeInMillis)
        }
    }

    fun stop() {
        val apm = this.apm.get()
        if (isSupport && apm != null) {
            if (isRunning.compareAndSet(true ,false)) {
                onStop(apm)
                AppLifecycleOwner.removeLifecycleObserver(this)
            }
        }
    }

    fun onStop(apm: tApm)

    fun addMonitorObserver(callback: MonitorDataObserver<T>) {
        this.monitorDataObservers.add(callback)
    }

    fun removeMonitorObserver(callback: MonitorDataObserver<T>) {
        this.monitorDataObservers.remove(callback)
    }

    fun dispatchMonitorData(t: T) {
        if (Looper.getMainLooper() === Looper.myLooper()) {
            executor.executeOnBackgroundThread {
                for (c in monitorDataObservers) {
                    c.onMonitorDataUpdate(t)
                }
            }
        } else {
            for (c in monitorDataObservers) {
                c.onMonitorDataUpdate(t)
            }
        }
    }

    interface MonitorDataObserver<T : Any> {
        fun onMonitorDataUpdate(t: T)
    }
}