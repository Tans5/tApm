package com.tans.tapm.monitors

import android.os.Process
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.JavaCrash
import com.tans.tapm.tApm
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class JavaCrashMonitor : AbsMonitor<JavaCrash>(Long.MAX_VALUE) {

    override val isSupport: Boolean
        get() = apm.get() != null

    @Volatile
    private var originUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private val hasDispatchedError: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }

    private val uncaughtExceptionHandler: Thread.UncaughtExceptionHandler by lazy {
        object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {
                tApmLog.e(TAG, "Error msg=${e.message}, threadName=${t.name}", e)
                val time = System.currentTimeMillis()
                if (hasDispatchedError.compareAndSet(false, true)) {
                    val otherThreadsStacks = Thread.getAllStackTraces().filter { it.key !== t }
                    dispatchMonitorData(
                        t = JavaCrash(
                            time = time,
                            thread = t,
                            error = e,
                            otherThreadsStacks = otherThreadsStacks
                        ),
                        dispatchOnBackgroundThread = false
                    )
                    originUncaughtExceptionHandler?.uncaughtException(t, e)
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                } else {
                    tApmLog.e(TAG, "Skip handle error: ${e.message}.")
                    originUncaughtExceptionHandler?.uncaughtException(t, e)
                }
            }
        }
    }

    override fun onInit(apm: tApm) {
        tApmLog.d(TAG, "Init JavaCrashMonitor success.")
    }

    override fun onStart(apm: tApm) {
        originUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        tApmLog.d(TAG, "OriginUncaughtExceptionHandler: $originUncaughtExceptionHandler")
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        tApmLog.d(TAG, "JavaCrashMonitor started.")
    }

    override fun onStop(apm: tApm) {
        Thread.setDefaultUncaughtExceptionHandler(originUncaughtExceptionHandler)
        tApmLog.e(TAG, "JavaCrashMonitor stopped.")
    }

    companion object {
        private const val TAG = "JavaCrashMonitor"
    }

}