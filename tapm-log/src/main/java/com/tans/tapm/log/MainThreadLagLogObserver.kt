package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.Anr
import com.tans.tapm.model.MainThreadLag
import com.tans.tapm.tApm
import com.tans.tlog.tLog

object MainThreadLagLogObserver : Monitor.MonitorDataObserver<MainThreadLag> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.MainThreadLag)

    override fun onMonitorDataUpdate(t: MainThreadLag, apm: tApm) {
        log?.w(TAG, t.toString())
    }

    private const val TAG = "MainThreadLag"
}