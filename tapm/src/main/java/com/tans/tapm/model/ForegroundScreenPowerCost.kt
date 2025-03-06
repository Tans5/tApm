package com.tans.tapm.model

data class ForegroundScreenPowerCost(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val powerCostInMah: Double
)