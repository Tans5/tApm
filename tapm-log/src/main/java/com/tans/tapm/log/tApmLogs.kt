package com.tans.tapm.log
import android.util.Log
import com.tans.tlog.tLog
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object tApmLogs {

    private val logs: ConcurrentHashMap<LogType, tLog?> by lazy {
        ConcurrentHashMap()
    }

    private val baseDir: AtomicReference<File?> by lazy {
        AtomicReference()
    }

    fun init(baseDir: File) {
        val logBaseDir = File(baseDir, "Log")
        if (!logBaseDir.exists()) {
            logBaseDir.mkdirs()
        }
        this.baseDir.set(logBaseDir)
        // Trigger CommonLog create.
        getLog(LogType.Common)
    }

    fun getLog(type: LogType): tLog? {
        val cache = logs[type]
        if (cache != null) {
            return cache
        }
        val baseDir = this.baseDir.get()
        if (baseDir == null) {
            Log.e("tApmLog", "Logs don't init can't create log")
            return null
        }
        val newLog = when (type) {
            LogType.Common -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "Common"))
                    .setMaxSize(commonLogSize)
                    .build()
            }
            LogType.CpuUsage -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "CpuUsage"))
                    .setMaxSize(cpuUsageLogSize)
                    .build()
            }
            LogType.MemoryUsage -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "MemoryUsage"))
                    .setMaxSize(memoryUsageLogSize)
                    .build()
            }
            LogType.HttpRequest -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "HttpRequest"))
                    .setMaxSize(httpRequestLogSize)
                    .build()
            }
            LogType.MainThreadLag -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "MainThreadLag"))
                    .setMaxSize(mainThreadLagLogSize)
                    .build()
            }
        }
        val old = logs.putIfAbsent(type, newLog)
        return old ?: newLog
    }


    enum class LogType {
        Common,
        CpuUsage,
        MemoryUsage,
        HttpRequest,
        MainThreadLag
    }

    // 50M
    @Volatile
    var commonLogSize = 1024 * 1024 * 50L

    // 50M
    @Volatile
    var cpuUsageLogSize = 1024 * 1024 * 50L

    // 50M
    @Volatile
    var memoryUsageLogSize = 1024 * 1024 * 50L

    // 200M
    @Volatile
    var httpRequestLogSize = 1024 * 1024 * 200L

    // 100M
    @Volatile
    var mainThreadLagLogSize = 1024 * 1024 * 100L

}