package com.tans.tapm.demo

import android.app.Application
import com.tans.tlog.tLog
import java.io.File

object AppLog {

    @Volatile
    private var log: tLog? = null

    fun init(application: Application) {
        val baseDir = File(application.getExternalFilesDir(null), "AppLog")
        log = tLog.Companion.Builder(baseDir).build()
    }

    fun d(tag: String, msg: String) {
        log?.d(tag, msg)
    }

    fun w(tag: String, msg: String) {
        log?.w(tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        log?.e(tag, msg, throwable)
    }

    fun flushLog() {
        log?.flush()
    }
}