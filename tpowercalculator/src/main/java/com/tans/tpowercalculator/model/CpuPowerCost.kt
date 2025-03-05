package com.tans.tpowercalculator.model

data class CpuPowerCost(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val powerCostDetails: List<CpuClusterPowerCost>,
    val powerCostInMah: Double,
    val currentProcessPowerCostInMah: Double
)

data class CpuClusterSpeedPowerCost(
    val speedInKhz: Long,
    val activeTimeInHour: Double,
    val powerCodeInMah: Double
)

data class CpuClusterPowerCost(
    val coreIndexRange: IntRange,
    val activeTimeInHour: Double,
    val powerCostDetails: List<CpuClusterSpeedPowerCost>,
    val powerCostInMah: Double
)