package com.tans.tapm.monitors

import android.app.Application
import com.tans.tapm.CpuStateSnapshotCapture
import com.tans.tapm.Executor
import com.tans.tapm.Monitor
import com.tans.tapm.PowerProfile
import com.tans.tapm.tApm
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

abstract class AbsMonitor<T : Any>(defaultMonitorIntervalInMillis: Long) : Monitor<T> {
    override val apm: AtomicReference<tApm?> = AtomicReference(null)
    override val application: Application by lazy {
        apm.get()!!.application
    }
    override val executor: Executor by lazy {
        apm.get()!!.executor
    }

    override val cacheBaseDir: File by lazy {
        apm.get()!!.cacheBaseDir
    }

    override val isRunning: AtomicBoolean = AtomicBoolean(false)
    override val monitorIntervalInMillis: AtomicLong = AtomicLong(defaultMonitorIntervalInMillis)
    override val monitorDataObservers: LinkedBlockingQueue<Monitor.MonitorDataObserver<T>> = LinkedBlockingQueue()

    val cpuStateSnapshotCapture: CpuStateSnapshotCapture?
        get() = apm.get()?.cpuStateSnapshotCapture

    val powerProfile: PowerProfile?
        get() = apm.get()?.powerProfile
}