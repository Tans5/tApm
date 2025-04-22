package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.Anr
import com.tans.tapm.tApm
import com.tans.tlog.tLog

object AnrLogObserver : Monitor.MonitorDataObserver<Anr> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.Common)

    override fun onMonitorDataUpdate(t: Anr, apm: tApm) {
        log?.e(TAG, "Receive anr signal: IsFromMe=${t.isSigFromMe}, TraceFile=${t.anrTraceFile}")
    }

    private const val TAG = "Anr"
}