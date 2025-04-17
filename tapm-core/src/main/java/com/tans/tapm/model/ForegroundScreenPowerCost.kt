package com.tans.tapm.model

import com.tans.tapm.formatDataTimeMs
import com.tans.tapm.toHumanReadablePower

data class ForegroundScreenPowerCost(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val powerCostInMah: Double
) {

    override fun toString(): String {
        return "StartTime=${startTimeInMillis.formatDataTimeMs()}, EndTime=${endTimeInMillis.formatDataTimeMs()}, PowerCost=${powerCostInMah.toHumanReadablePower()}"
    }
}