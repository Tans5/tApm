package com.tans.tapm.model

import com.tans.tapm.formatDataTime
import com.tans.tapm.toHumanReadableCpuSpeed
import com.tans.tapm.toHumanReadableHours
import com.tans.tapm.toHumanReadablePower

data class CpuPowerCost(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val powerCostDetails: List<CpuClusterPowerCost>,
    val powerCostInMah: Double,
    val currentProcessPowerCostInMah: Double
) {
    override fun toString(): String {
        val s = StringBuilder()
        s.appendLine("------------------------------------------")
        s.appendLine("StartTime=${startTimeInMillis.formatDataTime()}, EndTime=${endTimeInMillis.formatDataTime()}")
        s.appendLine("PowerCost=${powerCostInMah.toHumanReadablePower()}, CurrentProcessPowerCost=${currentProcessPowerCostInMah.toHumanReadablePower()}")
        for (clusterCost in powerCostDetails) {
            s.appendLine("  Cluster=${clusterCost.coreIndexRange}, PowerCost=${clusterCost.powerCostInMah.toHumanReadablePower()}, CoreCount=${clusterCost.coreIndexRange.last - clusterCost.coreIndexRange.first + 1}, ActiveTime=${clusterCost.activeTimeInHour.toHumanReadableHours()}")
            for (speedCost in clusterCost.powerCostDetails) {
                s.appendLine("      Speed=${speedCost.speedInKhz.toHumanReadableCpuSpeed()}, PowerCost=${speedCost.powerCostInMah.toHumanReadablePower()}, ActiveTime=${speedCost.activeTimeInHour.toHumanReadableHours()}")
            }
        }
        s.appendLine("------------------------------------------")
        return s.toString()
    }
}

data class CpuClusterSpeedPowerCost(
    val speedInKhz: Long,
    val activeTimeInHour: Double,
    val powerCostInMah: Double
)

data class CpuClusterPowerCost(
    val coreIndexRange: IntRange,
    val activeTimeInHour: Double,
    val powerCostDetails: List<CpuClusterSpeedPowerCost>,
    val powerCostInMah: Double
)