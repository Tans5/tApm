package com.tans.tapm.monitors

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Printer
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.MainThreadLag
import com.tans.tapm.tApm

class MainThreadLagMonitor : AbsMonitor<MainThreadLag>(DEFAULT_LAG_TIME) {

    override val isSupport: Boolean
        get() = apm.get() != null

    private val lagHandleHandler: Handler by lazy {
        object : Handler(executor.getBackgroundThreadLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    LAG_MSG -> {
                        val thread = msg.obj as? Thread
                        if (thread != null) {
                            val lagTime = monitorIntervalInMillis.get()
                            val stackTrace = thread.stackTrace ?: emptyArray()
                            dispatchMonitorData(
                                MainThreadLag(
                                    lagTime = lagTime,
                                    lagStackTrace = stackTrace
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private val mainThreadLooperPrinter: Printer by lazy {
        object : Printer {
            override fun println(x: String?) {
                if (x != null) {
                    if (x.startsWith(">>>>>")) {
                        onTaskStart()
                    } else {
                        onTaskEnd()
                    }
                }
            }

            fun onTaskStart() {
                val msg = Message.obtain()
                msg.what = LAG_MSG
                msg.obj = Thread.currentThread()
                lagHandleHandler.sendMessageDelayed(msg, monitorIntervalInMillis.get())
            }

            fun onTaskEnd() {
                lagHandleHandler.removeMessages(LAG_MSG)
            }
        }
    }

    override fun onInit(apm: tApm) {  }

    override fun onStart(apm: tApm) {
        Looper.getMainLooper().setMessageLogging(mainThreadLooperPrinter)
        tApmLog.d(TAG, "MainThreadLagMonitor started.")
    }

    override fun onStop(apm: tApm) {
        Looper.getMainLooper().setMessageLogging(null)
        tApmLog.d(TAG, "MainThreadLagMonitor stopped.")
    }

    companion object {
        private const val TAG = "MainThreadLagMonitor"
        // 300 ms
        private const val DEFAULT_LAG_TIME = 300L

        private const val LAG_MSG = 0
    }
}