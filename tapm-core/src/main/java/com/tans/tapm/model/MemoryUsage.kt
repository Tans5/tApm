package com.tans.tapm.model

import com.tans.tapm.toHumanReadableMemorySize
import com.tans.tapm.toHumanReadablePercent

data class MemoryUsage(
    val jvmAlloc: Long,
    val jvmAllocInPercent: Double,
    val jvmFree: Long,
    val jvmTotal: Long,
    val jvmMax: Long,

    val totalMemory: Long,

    val rss: Long,
    val rssInPercent: Double,
    val rssAnon: Long,
    val rssAnonInPercent: Double,
    val rssFile: Long,
    val rssShare: Long,

    // Only debug can get pss
    val pss: Long,
    val pssInPercent: Double,
) {
    override fun toString(): String {
        val s = StringBuilder()
        s.appendLine("JvmAlloc=${jvmAlloc.toHumanReadableMemorySize()}(${jvmAllocInPercent.toHumanReadablePercent()}), JvmFree=${jvmFree.toHumanReadableMemorySize()}, JvmTotal=${jvmTotal.toHumanReadableMemorySize()}, JvmMax=${jvmMax.toHumanReadableMemorySize()}")
        s.append("Rss=${rss.toHumanReadableMemorySize()}(${rssInPercent.toHumanReadablePercent()}), RssAnon=${rssAnon.toHumanReadableMemorySize()}(${rssAnonInPercent.toHumanReadablePercent()}), RssFile=${rssFile.toHumanReadableMemorySize()}, RssShare=${rssShare.toHumanReadableMemorySize()}")
        if (pss > 0) {
            s.append(", Pss=${pss.toHumanReadableMemorySize()}(${pssInPercent.toHumanReadablePercent()})")
        }
        s.append(", Total=${totalMemory.toHumanReadableMemorySize()}")
        return s.toString()
    }
}