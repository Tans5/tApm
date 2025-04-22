package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.CpuUsage
import com.tans.tapm.model.MemoryUsage
import com.tans.tapm.tApm
import com.tans.tapm.toHumanReadablePercent
import com.tans.tlog.tLog

object MemoryUsageLogObserver : Monitor.MonitorDataObserver<MemoryUsage> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.MemoryUsage)

    override fun onMonitorDataUpdate(t: MemoryUsage, apm: tApm) {
        log?.d(TAG, t.toString())
    }

    private const val TAG = "MemoryUsage"
}