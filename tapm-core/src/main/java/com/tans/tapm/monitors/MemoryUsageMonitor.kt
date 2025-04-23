package com.tans.tapm.monitors

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Message
import android.os.Process
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.MemoryUsage
import com.tans.tapm.tApm
import java.io.File

class MemoryUsageMonitor : AbsMonitor<MemoryUsage>(DEFAULT_MEMORY_USAGE_CHECK_INTERVAL) {

    override val isSupport: Boolean
        get() = apm.get() != null

    private val activityManager: ActivityManager? by lazy {
        application.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    private val handler: Handler by lazy {
        object : Handler(executor.getBackgroundThreadLooper()) {
            private val memoryInfo: ActivityManager.MemoryInfo by lazy {
                ActivityManager.MemoryInfo()
            }
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MEMORY_USAGE_CHECK_MSG -> {
                        val jvm = Runtime.getRuntime()
                        val jvmFree = jvm.freeMemory()
                        val jvmTotal = jvm.totalMemory()
                        val jvmMax = jvm.maxMemory()
                        val jvmAlloc = jvmTotal - jvmFree
                        val jvmUseInPercent = jvmAlloc.toDouble() / jvmMax.toDouble()

                        activityManager?.getMemoryInfo(memoryInfo)
                        val totalMem = memoryInfo.totalMem


                        val rssArray = readRss()
                        val rss = rssArray[0]
                        val rssInPercent = if (totalMem > 0) {
                            rss.toDouble() / totalMem.toDouble()
                        } else {
                            0.0
                        }
                        val rssAnon = rssArray[1]
                        val rssAnonInPercent = if (totalMem > 0) {
                            rssAnon.toDouble() / totalMem.toDouble()
                        } else {
                            0.0
                        }
                        val rssFile = rssArray[2]
                        val rssShare = rssArray[3]


                        val pss = if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                            val debugMemoryInfo = activityManager?.getProcessMemoryInfo(intArrayOf(Process.myPid()))?.getOrNull(0)
                            (debugMemoryInfo?.totalPss ?: 0) * 1024L
                        } else {
                            0L
                        }
                        val pssUseInPercent = if (totalMem > 0) {
                            pss.toDouble() / totalMem.toDouble()
                        } else {
                            0.0
                        }


                        dispatchMonitorData(
                            MemoryUsage(
                                jvmAlloc = jvmAlloc,
                                jvmFree = jvmFree,
                                jvmTotal = jvmTotal,
                                jvmMax = jvmMax,
                                jvmAllocInPercent = jvmUseInPercent,

                                rss = rss,
                                rssInPercent = rssInPercent,
                                rssAnon = rssAnon,
                                rssAnonInPercent = rssAnonInPercent,
                                rssFile = rssFile,
                                rssShare = rssShare,

                                totalMemory = totalMem,

                                pss = pss,
                                pssInPercent = pssUseInPercent
                            )
                        )

                        sendNextTimeCheckTask()
                    }
                }
            }

            fun sendNextTimeCheckTask() {
                removeMessages(MEMORY_USAGE_CHECK_MSG)
                sendEmptyMessageDelayed(MEMORY_USAGE_CHECK_MSG, monitorIntervalInMillis.get())
            }
        }
    }

    override fun onInit(apm: tApm) {  }

    override fun onStart(apm: tApm) {
        handler.removeMessages(MEMORY_USAGE_CHECK_MSG)
        handler.sendEmptyMessageDelayed(MEMORY_USAGE_CHECK_MSG, monitorIntervalInMillis.get())
        tApmLog.d(TAG, "MemoryUsageMonitor started.")
    }

    override fun onStop(apm: tApm) {
        handler.removeMessages(MEMORY_USAGE_CHECK_MSG)
        tApmLog.d(TAG, "MemoryUsageMonitor stopped.")
    }

//    private val pssRegex: Regex by lazy {
//        "^Pss:\\s*([0-9]+) kB$".toRegex()
//    }
//
//    private val rssRegex: Regex by lazy {
//        "^Rss:\\s*([0-9]+) kB$".toRegex()
//    }
//
//    // TODO: Read pss and rss will use huge memory.
//    private fun readPssAndRss(): Pair<Long, Long> {
//        return try {
//            val f = File("/proc/${Process.myPid()}/smaps")
//            var pssInKb: Long = 0
//            var rssInKb: Long = 0
//            f.bufferedReader(Charsets.UTF_8).use {
//                var l = it.readLine()
//                while (l != null) {
//                    if (pssRegex.matches(l)) {
//                        pssInKb += pssRegex.find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0
//                    } else if (rssRegex.matches(l)) {
//                        rssInKb += rssRegex.find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0
//                    }
//                    l = it.readLine()
//                }
//            }
//            (pssInKb * 1024L) to (rssInKb * 1024L)
//        } catch (e: Throwable) {
//            tApmLog.e(TAG, "Parse pss and rss fail: ${e.message}", e)
//            0L to 0L
//        }
//    }


    private val rssRegex: Regex by lazy {
        "^VmRSS:\\s*([0-9]+) kB$".toRegex()
    }
    private val rssAnonRegex: Regex by lazy {
        "^RssAnon:\\s*([0-9]+) kB$".toRegex()
    }
    private val rssFileRegex: Regex by lazy {
        "^RssFile:\\s*([0-9]+) kB$".toRegex()
    }
    private val rssShmemRegex: Regex by lazy {
        "^RssShmem:\\s*([0-9]+) kB$".toRegex()
    }
    private fun readRss(): LongArray {
        val result = LongArray(4)
        try {
            val f = File("/proc/${Process.myPid()}/status")
            f.bufferedReader(Charsets.UTF_8).use {
                var l = it.readLine()
                while (l != null) {
                    if (result[0] == 0L && rssRegex.matches(l)) {
                        result[0] = ((rssRegex.find(l)?.groupValues?.getOrNull(1))?.toLongOrNull() ?: 0L) * 1024L
                    }
                    if (result[1] == 0L && rssAnonRegex.matches(l)) {
                        result[1] = ((rssAnonRegex.find(l)?.groupValues?.getOrNull(1))?.toLongOrNull() ?: 0L) * 1024L
                    }
                    if (result[2] == 0L && rssFileRegex.matches(l)) {
                        result[2] = ((rssFileRegex.find(l)?.groupValues?.getOrNull(1))?.toLongOrNull() ?: 0L) * 1024L
                    }
                    if (result[3] == 0L && rssShmemRegex.matches(l)) {
                        result[3] = ((rssShmemRegex.find(l)?.groupValues?.getOrNull(1))?.toLongOrNull() ?: 0L) * 1024L
                        break
                    }
                    l = it.readLine()
                }
            }
        } catch (e: Throwable) {
            tApmLog.e(TAG, "Read pss fail: ${e.message}", e)
        }
        return result
    }

    companion object {
        // 30s
        private const val DEFAULT_MEMORY_USAGE_CHECK_INTERVAL = 1000L * 30L

        private const val MEMORY_USAGE_CHECK_MSG = 0

        private const val TAG = "MemoryUsageMonitor"
    }
}