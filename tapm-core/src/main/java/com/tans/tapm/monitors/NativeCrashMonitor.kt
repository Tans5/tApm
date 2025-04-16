package com.tans.tapm.monitors

import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.NativeCrash
import com.tans.tapm.tApm
import java.io.File
import java.lang.StringBuilder

@Keep
class NativeCrashMonitor : AbsMonitor<NativeCrash>(Long.MAX_VALUE) {
    override val isSupport: Boolean
        get() = apm.get() != null

    private var nativePtr: Long = 0L

    override fun onInit(apm: tApm) {

    }

    override fun onStart(apm: tApm) {
        val dir = File(cacheBaseDir, "NativeCrash")
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
                val ptr = registerNativeCrashMonitorNative(dir.canonicalPath,  Build.FINGERPRINT)
                if (ptr != 0L) {
                    nativePtr = ptr
                    Log.d(TAG, "NativeCrashMonitor started.")
                } else {
                    Log.e(TAG, "Start NativeCrashMonitor failed.")
                }
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

    /**
     * Call by native code.
     */
    fun onNativeCrash(
        sig: Int,
        sigCode: Int,
        crashPid: Int,
        crashTid: Int,
        crashUid: Int,
        startTime: Long,
        crashTime: Long,
        crashTraceFilePath: String,
        writeTraceFileResult: Int) {
        tApmLog.e(TAG, "NativeCrash: Sig=$sig, SigCode=$sigCode, CrashPid=$crashPid, CrashTid=$crashTid, CrashUid=$crashUid, ProcessUptime=${crashTime - startTime}ms, CrashTraceFile=$crashTraceFilePath, WriteCrashTraceFileRet=$writeTraceFileResult")
        val traceFile = File(crashTraceFilePath)
        val summary: String? = if (traceFile.exists()) {
            val sb = StringBuilder()
            try {
                traceFile.inputStream().bufferedReader(Charsets.UTF_8).use {
                    do {
                        val l = it.readLine()
                        if (l == null || l.startsWith("---")) {
                            break
                        }
                        sb.appendLine(l)
                    } while (true)
                }
            } catch (e: Throwable) {
                tApmLog.e(TAG, "Read trace file fail: ${e.message}", e)
            }
            sb.toString()
        } else {
            null
        }
        if (summary != null) {
            tApmLog.e(TAG, summary)
        }
        dispatchMonitorData(
            NativeCrash(
                sig = sig,
                sigCode = sigCode,
                crashPid = crashPid,
                crashTid = crashTid,
                crashUid = crashUid,
                startTime = startTime,
                crashTime = crashTime,
                crashSummary = summary,
                crashTraceFilePath = if (writeTraceFileResult == 0) crashTraceFilePath else null
            )
        )

    }

    private external fun registerNativeCrashMonitorNative(crashFileDir: String, fingerprint: String): Long

    private external fun unregisterNativeCrashMonitorNative(nativePtr: Long)

    companion object {
        private const val TAG = "NativeCrashMonitor"
    }
}