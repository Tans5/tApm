package com.tans.tapm

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tans.tapm.internal.tApmLog

class Executor(backgroundThread: HandlerThread?) {

    private val backgroundThread: HandlerThread = backgroundThread ?: defaultBackgroundThread

    private val backgroundThreadHandler: Handler

    private val mainThreadHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    init {
        if (!this.backgroundThread.isAlive) {
            tApmLog.d(TAG, "BgThread is not start, do start.")
            this.backgroundThread.start()
        }
        while (this.backgroundThread.looper == null) { // Wait bg thread active.
            Thread.sleep(3)
        }
        backgroundThreadHandler = Handler(this.backgroundThread.looper)
    }

    fun executeOnBackgroundThread(r: Runnable) {
        backgroundThreadHandler.post(r)
    }

    fun executeOnMainThread(r: Runnable) {
        mainThreadHandler.post(r)
    }

    fun getBackgroundThreadLooper(): Looper = this@Executor.backgroundThread.looper

    companion object {

        private const val TAG = "Executor"

        private val defaultBackgroundThread: HandlerThread by lazy {
            object : HandlerThread("tapm-default") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    tApmLog.d(TAG, "default background thread prepared.")
                }
            }
        }
    }
}