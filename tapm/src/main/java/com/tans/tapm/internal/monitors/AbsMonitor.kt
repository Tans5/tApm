package com.tans.tapm.internal.monitors

import com.tans.tapm.MonitorCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

abstract class AbsMonitor<T : Any>(defaultMonitorIntervalInMillis: Long) : Monitor<T> {
    override val isRunning: AtomicBoolean = AtomicBoolean(false)
    override val monitorIntervalInMillis: AtomicLong = AtomicLong(defaultMonitorIntervalInMillis)
    override val monitorCallback: AtomicReference<MonitorCallback<T>?> = AtomicReference(null)
}