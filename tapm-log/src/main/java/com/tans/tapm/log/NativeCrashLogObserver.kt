package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.NativeCrash
import com.tans.tapm.tApm
import com.tans.tlog.tLog

object NativeCrashLogObserver : Monitor.MonitorDataObserver<NativeCrash> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.Common)

    override fun onMonitorDataUpdate(t: NativeCrash, apm: tApm) {
        log?.e(TAG, "NativeCrash: Sig=${t.sig}, SigCode=${t.sigCode}, Pid=${t.crashPid}, Tid=${t.crashTid}, Uid=${t.crashUid}, TraceFile=${t.crashTraceFilePath}")
        t.crashSummary?.let {
            log?.e(TAG, it)
        }
    }

    private const val TAG = "NativeCrash"
}