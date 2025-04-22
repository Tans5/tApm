package com.tans.tapm.monitors

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

    private val handler: Handler by lazy {
        object : Handler(executor.getBackgroundThreadLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MEMORY_USAGE_CHECK_MSG -> {
                        val jvm = Runtime.getRuntime()
                        val jvmFree = jvm.freeMemory()
                        val jvmTotal = jvm.totalMemory()
                        val jvmMax = jvm.maxMemory()
                        val jvmAlloc = jvmTotal - jvmFree
                        val jvmUseInPercent = jvmAlloc.toDouble() / jvmMax.toDouble()

                        val (pss, rss) = readCurrentProcessPssAndRss()
                        val maxMemory = apm.get()?.deviceInfo?.memorySizeInBytes ?: 0L
                        val pssUseInPercent = if (maxMemory > 0) {
                            pss.toDouble() / maxMemory.toDouble()
                        } else {
                            0.0
                        }
                        val rssUseInPercent = if (maxMemory > 0) {
                            rss.toDouble() / maxMemory.toDouble()
                        } else {
                            0.0
                        }

                        dispatchMonitorData(
                            MemoryUsage(
                                jvmAllocMemory = jvmAlloc,
                                jvmFreeMemory = jvmFree,
                                jvmTotalMemory = jvmTotal,
                                jvmMaxMemory = jvmMax,
                                jvmUseInPercent = jvmUseInPercent,

                                pss = pss,
                                pssUseInPercent = pssUseInPercent,
                                rss = rss,
                                rssUseInPercent = rssUseInPercent,
                                maxMemory = maxMemory
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

    private val pssRegex: Regex by lazy {
        "^Pss:\\s*([0-9]+) kB$".toRegex()
    }

    private val rssRegex: Regex by lazy {
        "^Rss:\\s*([0-9]+) kB$".toRegex()
    }

    private fun readCurrentProcessPssAndRss(): Pair<Long, Long> {
        return try {
            val f = File("/proc/${Process.myPid()}/smaps")
            var pssInKb: Long = 0
            var rssInKb: Long = 0
            f.bufferedReader(Charsets.UTF_8).use {
                var l = it.readLine()
                while (l != null) {
                    if (pssRegex.matches(l)) {
                        pssInKb += pssRegex.find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0
                    } else if (rssRegex.matches(l)) {
                        rssInKb += rssRegex.find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0
                    }
                    l = it.readLine()
                }
            }
            (pssInKb * 1024L) to (rssInKb * 1024L)
        } catch (e: Throwable) {
            tApmLog.e(TAG, "Parse pss and rss fail: ${e.message}", e)
            0L to 0L
        }
    }

    companion object {
        // 30s
        private const val DEFAULT_MEMORY_USAGE_CHECK_INTERVAL = 1000L * 30L

        private const val MEMORY_USAGE_CHECK_MSG = 0

        private const val TAG = "MemoryUsageMonitor"
    }
}