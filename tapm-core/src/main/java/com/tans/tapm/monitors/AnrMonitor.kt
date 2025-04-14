package com.tans.tapm.monitors

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.Anr
import com.tans.tapm.tApm
import java.io.File

@Keep
class AnrMonitor : AbsMonitor<Anr>(Long.MAX_VALUE) {
    override val isSupport: Boolean
        get() = this.apm.get() != null

    private var nativePtr: Long = 0

    override fun onInit(apm: tApm) {  }

    override fun onStart(apm: tApm) {
        // Register SIGQUIT need on MainThread.
        val dir = File(cacheBaseDir, "Anr")
        val isDirInitSuccess = if (!dir.isDirectory) {
            try {
                dir.mkdirs()
            } catch (e: Throwable) {
                tApmLog.e(TAG, "Create dir fail: ${e.message}", e)
                false
            }
        } else {
            true
        }
        if (isDirInitSuccess) {
            executor.executeOnMainThread {
                val ptr = registerAnrMonitorNative(dir.canonicalPath)
                if (ptr != 0L) {
                    this.nativePtr = ptr
                    tApmLog.d(TAG, "AnrMonitor started.")
                } else {
                    tApmLog.e(TAG, "Start AnrMonitor failed.")
                }
            }
        }
    }

    override fun onStop(apm: tApm) {
        // Register SIGQUIT need on MainThread.
        executor.executeOnMainThread {
            val ptr = nativePtr
            if (ptr != 0L) {
                unregisterAnrMonitorNative(ptr)
                nativePtr = 0
                tApmLog.d(TAG, "AnrMonitor stopped.")
            } else {
                tApmLog.e(TAG, "Stop AnrMonitor failed.")
            }
        }
    }

    /**
     * Call by native code.
     */
    fun onAnr(time: Long, isSigFromMe: Boolean, anrTraceFile: String) {
        tApmLog.e(TAG, "Receive SIGQUIT signal, isFromMe=$isSigFromMe, anrTraceFilePath=$anrTraceFile")
        if (isSigFromMe) {
            dispatchMonitorData(
                Anr(time = time,
                    isSigFromMe = true,
                    anrTraceFile = anrTraceFile)
            )
        } else {
//            if (checkCurrentProcessInAnr()) {
//                dispatchMonitorData(
//                    Anr(time = time,
//                        isSigFromMe = false,
//                        anrTraceData = anrTraceData)
//                )
//            } else {
//                tApmLog.e(TAG, "Receive SIGQUIT signal, but current process not in anr.")
//            }
            dispatchMonitorData(
                Anr(time = time,
                    isSigFromMe = false,
                    anrTraceFile = anrTraceFile)
            )
        }
    }

    /**
     * // TODO: Adjust blow code.
     */
    private fun checkCurrentProcessInAnr(): Boolean {
        val am = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val errorProcesses = am.processesInErrorState
        if (errorProcesses == null) {
            return false
        }
        val myPid = Process.myPid()
        val myUid = Process.myUid()
        for (errorProc in errorProcesses) {
            if (
                errorProc.pid == myPid &&
                errorProc.uid == myUid &&
                errorProc.condition == ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING) {
                return true
            }
        }
        return false
    }

    private external fun registerAnrMonitorNative(anrFileDir: String): Long

    private external fun unregisterAnrMonitorNative(nativePtr: Long)

    companion object {
        const val TAG = "AnrMonitor"
    }
}