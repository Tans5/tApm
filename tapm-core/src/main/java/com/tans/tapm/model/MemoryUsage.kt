package com.tans.tapm.model

import com.tans.tapm.toHumanReadableMemorySize
import com.tans.tapm.toHumanReadablePercent

data class MemoryUsage(
    val jvmAllocMemory: Long,
    val jvmFreeMemory: Long,
    val jvmTotalMemory: Long,
    val jvmMaxMemory: Long,
    val jvmUseInPercent: Double,

    val pss: Long,
    val pssUseInPercent: Double,
    val rss: Long,
    val rssUseInPercent: Double,
    val maxMemory: Long
) {
    override fun toString(): String {
        val s = StringBuilder()
        s.appendLine("JvmAlloc=${jvmAllocMemory.toHumanReadableMemorySize()}, JvmFree=${jvmFreeMemory.toHumanReadableMemorySize()}, JvmTotal=${jvmTotalMemory.toHumanReadableMemorySize()}, JvmMax=${jvmMaxMemory.toHumanReadableMemorySize()}, JvmUseInPercent=${jvmUseInPercent.toHumanReadablePercent()}")
        s.append("Pss=${pss.toHumanReadableMemorySize()}, PssUseInPercent=${pssUseInPercent.toHumanReadablePercent()}, Rss=${rss.toHumanReadableMemorySize()}, RssUseInPercent=${rssUseInPercent.toHumanReadablePercent()}, Max=${maxMemory.toHumanReadableMemorySize()}")
        return s.toString()
    }
}