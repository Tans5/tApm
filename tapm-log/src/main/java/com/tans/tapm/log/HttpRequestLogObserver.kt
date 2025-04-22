package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.HttpRequest
import com.tans.tapm.tApm
import com.tans.tlog.tLog

object HttpRequestLogObserver : Monitor.MonitorDataObserver<HttpRequest> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.HttpRequest)

    override fun onMonitorDataUpdate(t: HttpRequest, apm: tApm) {
        log?.d(TAG, t.toString())
    }

    private const val TAG = "HttpRequest"
}