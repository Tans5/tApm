package com.tans.tapm.internal

import com.tans.tapm.CpuStateSnapshotCapture
import java.text.SimpleDateFormat
import java.util.Locale

internal fun Long.toHumanReadableCpuSpeed(): String {
    return String.format(Locale.US,"%.2f GHz", this.toDouble() / 1_000_000.0)
}

internal fun Double.toHumanReadablePercent(): String {
    return String.format(Locale.US, "%.1f", this * 100.0) + " %"
}

internal fun Float.toHumanReadablePercent(): String {
    return String.format(Locale.US, "%.1f", this * 100.0) + " %"
}

internal fun Double.toHumanReadableHours(): String {
    return String.format(Locale.US, "%.2f H", this)
}

internal fun Double.toHumanReadablePower(): String {
    return String.format(Locale.US, "%.2f mAh", this)
}

internal fun Long.jiffiesToHours(): Double {
    return this * CpuStateSnapshotCapture.oneJiffyInMillis.toDouble() / (60.0 * 60.0 * 1000.0)
}

internal fun Long.millisToHours(): Double {
    return this / (60.0 * 60.0 * 1000.0)
}

private val sdfDateTimeMsThreadLocal: ThreadLocal<SimpleDateFormat> by lazy {
    ThreadLocal()
}

private val sdfDateTimeMs: SimpleDateFormat
    get() {
        return sdfDateTimeMsThreadLocal.get().let {
            if (it == null) {
                val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                sdfDateTimeMsThreadLocal.set(f)
                f
            } else {
                it
            }
        }
    }

internal fun Long.formatDataTime(): String {
    return sdfDateTimeMs.format(this)
}