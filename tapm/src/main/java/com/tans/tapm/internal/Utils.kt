package com.tans.tapm.internal

import java.util.Locale

internal fun Long.toHumanReadableCpuSpeed(): String {
    return String.format(Locale.US,"%.2f GHz", this.toDouble() / 1_000_000.0)
}

internal fun Double.toHumanReadableCpuUsage(): String {
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