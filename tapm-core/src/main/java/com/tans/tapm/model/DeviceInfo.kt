package com.tans.tapm.model

import com.tans.tapm.chipset.Chipset
import com.tans.tapm.toHumanReadableMemorySize

data class DeviceInfo(
    val deviceName: String,
    val apiLevel: Int,
    val androidName: String,
    val chipset: Chipset,
    val cpuCoreSize: Int,
    val cpuClusters: List<CpuCluster>,
    val cpuSupportAbi: String,
    val memorySizeInBytes: Long,
    val jvmMaxMemorySizeInBytes: Long
) {

    override fun toString(): String {
        return "DeviceName=$deviceName, ApiLevel=$apiLevel, AndroidName=$androidName, Chipset=${chipset.getChipsetName()}, CpuCoreSize=$cpuCoreSize, CpuSupportAbi=$cpuSupportAbi, MemorySize=${memorySizeInBytes.toHumanReadableMemorySize()}, JvmMemorySize=${jvmMaxMemorySizeInBytes.toHumanReadableMemorySize()}"
    }

    companion object {

        data class CpuCluster(
            val cpuCoreSize: Int,
            val minSpeedInKHz: Long,
            val maxSpeedInKHz: Long,
        )
    }
}
