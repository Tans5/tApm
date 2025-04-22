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
                    .setMaxSize(1024 * 1024 * 150) // 150M
                    .build()
            }
            LogType.CpuUsage -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "CpuUsage"))
                    .setMaxSize(1024 * 1024 * 50) // 50M
                    .build()
            }
            LogType.MemoryUsage -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "MemoryUsage"))
                    .setMaxSize(1024 * 1024 * 50) // 50M
                    .build()
            }
            LogType.HttpRequest -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "HttpRequest"))
                    .setMaxSize(1024 * 1024 * 200) // 200M
                    .build()
            }
            LogType.MainThreadLag -> {
                tLog.Companion.Builder(baseDir = File(baseDir, "MainThreadLag"))
                    .setMaxSize(1024 * 1024 * 100) // 100M
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
}