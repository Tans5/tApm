package com.tans.tapm.model

import com.tans.tapm.formatDataTimeMs
import com.tans.tapm.toHumanReadableCpuSpeed
import com.tans.tapm.toHumanReadablePercent

data class SingleCpuCoreUsage(
    val coreIndex: Int,
    val coreSpec: CpuSpec,
    val currentCoreSpeedInKHz: Long,
    val cpuUsage: Double,
    val allCpuTimeInJiffies: Long,
    val cpuIdleTimeInJiffies: Long,
    val cpuActiveTimeInJiffies: Long,
)

data class ProgressSingleCpuCoreUsage(
    val refCore: SingleCpuCoreUsage,
    val cpuWorkTimeInJiffies: Long,
    val cpuUsage: Double
)

data class CpuUsage(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val cpuCoresUsage: List<SingleCpuCoreUsage>,
    val avgCpuUsage: Double,
    val currentProcessCpuCoresUsage: List<ProgressSingleCpuCoreUsage>,
    val currentProcessAvgCpuUsage: Double
) {
    override fun toString(): String {
        val s = StringBuilder()
        s.appendLine("------------------------------------------")
        s.appendLine("StartTime=${startTimeInMillis.formatDataTimeMs()}, EndTime=${endTimeInMillis.formatDataTimeMs()}")
        s.appendLine("CpuAvgUsage=${avgCpuUsage.toHumanReadablePercent()}, CurrentProcessCpuAvgUsage=${currentProcessAvgCpuUsage.toHumanReadablePercent()}")
        for (usage in cpuCoresUsage) {
            s.appendLine("  CpuIndex=${usage.coreIndex}, Usage=${usage.cpuUsage.toHumanReadablePercent()}, ActiveTimeInJiffies=${usage.cpuActiveTimeInJiffies}, CurrentSpeed=${usage.currentCoreSpeedInKHz.toHumanReadableCpuSpeed()}, MinSpeed=${usage.coreSpec.minSpeedInKHz.toHumanReadableCpuSpeed()}, MaxSpeed=${usage.coreSpec.maxSpeedInKHz.toHumanReadableCpuSpeed()}")
        }
        s.appendLine("------------------------------------------")
        return s.toString()
    }
}

data class CpuSpec(
    val cpuCoreIndex: Int,
    val minSpeedInKHz: Long,
    val maxSpeedInKHz: Long,
    val availableSpeedsInKHz: List<Long>
)