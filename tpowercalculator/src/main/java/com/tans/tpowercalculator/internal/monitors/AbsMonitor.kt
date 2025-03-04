package com.tans.tpowercalculator.internal.monitors

import com.tans.tpowercalculator.MonitorCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

abstract class AbsMonitor<T : Any>(defaultMonitorIntervalInMillis: Long) : Monitor<T> {
    override val isSupport: Boolean = false
    override val isRunning: AtomicBoolean = AtomicBoolean(false)
    override val monitorIntervalInMillis: AtomicLong = AtomicLong(defaultMonitorIntervalInMillis)
    override val monitorCallback: AtomicReference<MonitorCallback<T>?> = AtomicReference(null)
}