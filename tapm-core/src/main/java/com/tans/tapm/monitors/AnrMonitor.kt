package com.tans.tapm.monitors

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.Anr
import com.tans.tapm.tApm

@Keep
class AnrMonitor : AbsMonitor<Anr>(Long.MAX_VALUE) {
    override val isSupport: Boolean
        get() = this.apm.get() != null

    private var nativePtr: Long = -1

    override fun onInit(apm: tApm) {  }

    override fun onStart(apm: tApm) {
        // Register SIGQUIT need on MainThread.
        executor.executeOnMainThread {
            val ptr = registerAnrMonitorNative()
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

    /**
     * Call by native code.
     */
    fun onAnr(time: Long, isSigFromMe: Boolean, anrTraceData: String) {
        tApmLog.e(TAG, "Receive SIGQUIT signal, isFromMe=$isSigFromMe")
        if (isSigFromMe) {
            dispatchMonitorData(
                Anr(time = time,
                    isSigFromMe = true,
                    anrTraceData = anrTraceData)
            )
        } else {
            if (checkCurrentProcessInAnr()) {
                dispatchMonitorData(
                    Anr(time = time,
                        isSigFromMe = false,
                        anrTraceData = anrTraceData)
                )
            } else {
                tApmLog.e(TAG, "Receive SIGQUIT signal, but current process not in anr.")
            }
        }
    }

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

    private external fun registerAnrMonitorNative(): Long

    private external fun unregisterAnrMonitorNative(nativePtr: Long)

    companion object {
        const val TAG = "AnrMonitor"
    }
}