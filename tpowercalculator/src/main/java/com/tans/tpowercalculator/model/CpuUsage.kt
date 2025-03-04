package com.tans.tpowercalculator.model

data class SingleCpuCoreUsage(
    val coreIndex: Int,
    val coreSpec: CpuSpec,
    val currentCoreSpeedInKHz: Long,
    val cpuUsage: Double,
    val allCpuTimeInJiffies: Long,
    val cpuIdleTimeInJiffies: Long,
    val cpuWorkTimeInJiffies: Long,
)

data class ProgressSingleCpuCoreUsage(
    val refCore: SingleCpuCoreUsage,
    val cpuWorkTimeInJiffies: Long,
    val cpuUsage: Double
)

data class CpuUsage(
    val durationInMillis: Long,
    val cpuCoresUsage: List<SingleCpuCoreUsage>,
    val avgCpuUsage: Double,
    val currentProcessCpuCoresUsage: List<ProgressSingleCpuCoreUsage>,
    val currentProcessAvgCpuUsage: Double
)

data class CpuSpec(
    val cpuCoreIndex: Int,
    val minSpeedInKHz: Long,
    val maxSpeedInKHz: Long,
    val availableSpeedsInKHz: List<Long>
)