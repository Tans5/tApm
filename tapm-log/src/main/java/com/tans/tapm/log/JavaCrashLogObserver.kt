package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.JavaCrash
import com.tans.tapm.tApm
import com.tans.tlog.tLog

object JavaCrashLogObserver : Monitor.MonitorDataObserver<JavaCrash> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.Common)

    override fun onMonitorDataUpdate(t: JavaCrash, apm: tApm) {
        log?.e(TAG, "JavaCrash: Thread=${t.thread.name}, TraceFile=${t.crashTraceFilePath}", t.error)
    }

    private const val TAG = "JavaCrash"
}