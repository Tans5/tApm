package com.tans.tpowercalculator.internal

import java.util.Locale

fun Long.toHumanReadableCpuSpeed(): String {
    return String.format(Locale.US,"%.2f GHz", this.toDouble() / 1_000_000.0)
}

fun Double.toHumanReadableCpuUsage(): String {
    return String.format(Locale.US, "%.1f", this * 100.0) + " %"
}