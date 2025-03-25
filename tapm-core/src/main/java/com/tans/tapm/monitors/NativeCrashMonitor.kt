package com.tans.tapm.monitors

import android.util.Log
import androidx.annotation.Keep
import com.tans.tapm.model.NativeCrash
import com.tans.tapm.tApm
import java.io.File

@Keep
class NativeCrashMonitor : AbsMonitor<NativeCrash>(Long.MAX_VALUE) {
    override val isSupport: Boolean
        get() = apm.get() != null

    private var nativePtr: Long = 0L

    override fun onInit(apm: tApm) {

    }

    override fun onStart(apm: tApm) {
        val dir = File(cacheBaseDir, "NativeCrash")
        if (!dir.isDirectory) {
            dir.mkdirs()
        }
        executor.executeOnMainThread {
            val ptr = registerNativeCrashMonitorNative(dir.canonicalPath)
            if (ptr != 0L) {
                nativePtr = ptr
                Log.d(TAG, "NativeCrashMonitor started.")
            } else {
                Log.e(TAG, "Start NativeCrashMonitor failed.")
            }
        }
    }

    override fun onStop(apm: tApm) {
        executor.executeOnMainThread {
            val ptr = nativePtr
            if (ptr != 0L) {
                unregisterNativeCrashMonitorNative(ptr)
                nativePtr = 0L
                Log.d(TAG, "NativeCrashMonitor stopped.")
            } else {
                Log.e(TAG, "Stop NativeCrashMonitor failed.")
            }
        }
    }

    external fun testNativeCrash()

    private external fun registerNativeCrashMonitorNative(crashFileDir: String): Long

    private external fun unregisterNativeCrashMonitorNative(nativePtr: Long)

    companion object {

        private const val TAG = "NativeCrashMonitor"
    }
}