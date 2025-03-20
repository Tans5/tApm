package com.tans.tapm

import java.text.SimpleDateFormat
import java.util.Locale

fun Long.toHumanReadableCpuSpeed(): String {
    return String.format(Locale.US,"%.2f GHz", this.toDouble() / 1_000_000.0)
}

fun Double.toHumanReadablePercent(): String {
    return String.format(Locale.US, "%.1f", this * 100.0) + " %"
}

fun Float.toHumanReadablePercent(): String {
    return String.format(Locale.US, "%.1f", this * 100.0) + " %"
}

fun Double.toHumanReadableHours(): String {
    return String.format(Locale.US, "%.2f H", this)
}

fun Double.toHumanReadablePower(): String {
    return String.format(Locale.US, "%.2f mAh", this)
}

fun Long.toHumanReadableMemorySize(): String {
    return when(this) {
        in Long.MIN_VALUE until 0L -> this.toString()
        in 0L until 1024L -> "$this B"
        in 1024L until 1024L * 1024L -> "${String.format(Locale.US, "%.2f", this.toDouble() / 1024.0)} KB"
        in 1024L * 1024L until 1024L * 1024L * 1024L -> "${String.format(Locale.US, "%.2f", this.toDouble() / (1024.0 * 1024.0))} MB"
        else -> "${String.format(Locale.US, "%.2f", this.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

fun Long.jiffiesToHours(): Double {
    return this * CpuStateSnapshotCapture.oneJiffyInMillis.toDouble() / (60.0 * 60.0 * 1000.0)
}

fun Long.millisToHours(): Double {
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

fun Long.formatDataTime(): String {
    return sdfDateTimeMs.format(this)
}