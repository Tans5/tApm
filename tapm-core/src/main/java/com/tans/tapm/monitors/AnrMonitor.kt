package com.tans.tapm.monitors

import androidx.annotation.Keep
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.tApm

@Keep
class AnrMonitor : AbsMonitor<Unit>(Long.MAX_VALUE) {
    override val isSupport: Boolean
        get() = this.apm.get() != null

    private var nativePtr: Long = -1

    override fun onInit(apm: tApm) {  }

    override fun onStart(apm: tApm) {
        // Register SIGQUIT need on MainThread.
        executor.executeOnMainThread {
            val ptr = registerAnrMonitorNative(application.cacheDir.toString())
            if (ptr != 0L) {
                this.nativePtr = ptr
                tApmLog.d(TAG, "AnrMonitor started.")
            } else {
                tApmLog.e(TAG, "Start AnrMonitor failed.")
            }
        }
    }

    override fun onStop(apm: tApm) {
        // Register SIGQUIT need on MainThread.
        executor.executeOnMainThread {
            val ptr = nativePtr
            if (ptr > 0L) {
                unregisterAnrMonitorNative(ptr)
                nativePtr = -1
                tApmLog.d(TAG, "AnrMonitor stopped.")
            } else {
                tApmLog.e(TAG, "Stop AnrMonitor failed.")
            }
        }
    }

    private external fun registerAnrMonitorNative(anrOutputDir: String): Long

    private external fun unregisterAnrMonitorNative(nativePtr: Long)

    companion object {
        const val TAG = "AnrMonitor"
    }
}