package com.tans.tapm

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

object Executors {

    val bgHandlerThread: HandlerThread by lazy {
        val isPrepared = AtomicBoolean(false)
        val t = object : HandlerThread("tApm_BgThread", NORM_PRIORITY) {
            override fun onLooperPrepared() {
                isPrepared.set(true)
            }
        }.apply { this.start() }
        while (!isPrepared.get()) {}
        t
    }

    val bgHandler: Handler by lazy {
        Handler(bgHandlerThread.looper)
    }

    val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    val bgExecutor: Executor by lazy {
        Executor { command -> bgHandler.post(command) }
    }

    val mainExecutor: Executor by lazy {
        Executor { command ->  mainHandler.post(command) }
    }
}