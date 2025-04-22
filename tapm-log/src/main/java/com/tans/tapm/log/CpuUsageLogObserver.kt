package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.model.CpuUsage
import com.tans.tapm.tApm
import com.tans.tapm.toHumanReadablePercent
import com.tans.tlog.tLog

object CpuUsageLogObserver : Monitor.MonitorDataObserver<CpuUsage> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.CpuUsage)

    override fun onMonitorDataUpdate(t: CpuUsage, apm: tApm) {
        log?.d(TAG, "CpuUsage=${t.avgCpuUsage.toHumanReadablePercent()}, CurrentProcessCpuUsage=${t.currentProcessAvgCpuUsage.toHumanReadablePercent()}")
    }

    private const val TAG = "CpuUsage"
}