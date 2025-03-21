package com.tans.tapm

import android.app.Application
import android.os.Looper
import java.io.File
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

    val cacheBaseDir: File

    fun init(apm: tApm) {
        if (this.apm.compareAndSet(null, apm)) {
            onInit(apm)
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

    fun addMonitorObserver(o: MonitorDataObserver<T>) {
        this.monitorDataObservers.add(o)
    }

    fun removeMonitorObserver(o: MonitorDataObserver<T>) {
        this.monitorDataObservers.remove(o)
    }

    fun dispatchMonitorData(t: T, dispatchOnBackgroundThread: Boolean = true) {

        if (dispatchOnBackgroundThread && Looper.getMainLooper() === Looper.myLooper()) {
            executor.executeOnBackgroundThread {
                for (c in monitorDataObservers) {
                    c.onMonitorDataUpdate(t, apm.get()!!)
                }
            }
        } else {
            for (c in monitorDataObservers) {
                c.onMonitorDataUpdate(t, apm.get()!!)
            }
        }
    }

    interface MonitorDataObserver<T : Any> {
        fun onMonitorDataUpdate(t: T, apm: tApm)
    }
}