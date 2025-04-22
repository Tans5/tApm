package com.tans.tapm.log

import com.tans.tapm.Monitor
import com.tans.tapm.formatDataTimeMs
import com.tans.tapm.model.CpuPowerCost
import com.tans.tapm.tApm
import com.tans.tapm.toHumanReadablePower
import com.tans.tlog.tLog

object CpuPowerCostLogObserver : Monitor.MonitorDataObserver<CpuPowerCost> {

    private val log: tLog?
        get() = tApmLogs.getLog(tApmLogs.LogType.Common)

    override fun onMonitorDataUpdate(t: CpuPowerCost, apm: tApm) {
        log?.d(TAG, "Start=${t.startTimeInMillis.formatDataTimeMs()}, End=${t.endTimeInMillis.formatDataTimeMs()}, PowerCost=${t.powerCostInMah.toHumanReadablePower()}")
    }

    private const val TAG = "CpuPowerCost"
}