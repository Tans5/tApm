package com.tans.tapm.internal

import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal object Executors {

    val bgHandlerThread: HandlerThread by lazy {
        val isPrepared = AtomicBoolean(false)
        val t = object : HandlerThread("tApm_BgThread", Thread.NORM_PRIORITY) {
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

    val bgExecutors: Executor by lazy {
        Executor { command -> bgHandler.post(command) }
    }

}