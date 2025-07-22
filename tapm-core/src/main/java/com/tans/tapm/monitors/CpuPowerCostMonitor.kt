package com.tans.tapm.monitors

import android.os.Handler
import android.os.Message
import android.os.SystemClock
import com.tans.tapm.CpuStateSnapshotCapture
import com.tans.tapm.PowerProfile.Companion.ComponentProfile.CpuProfile
import com.tans.tapm.jiffiesToHours
import com.tans.tapm.millisToHours
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.CpuClusterPowerCost
import com.tans.tapm.model.CpuClusterSpeedPowerCost
import com.tans.tapm.model.CpuPowerCost
import com.tans.tapm.tApm
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.iterator

class CpuPowerCostMonitor : AbsMonitor<CpuPowerCost>(CPU_POWER_COST_CHECK_INTERNAL) {

    @Volatile
    private var isSupportPrivate: Boolean = false

    override val isSupport: Boolean
        get() = isSupportPrivate

    private val lastPowerCostFromUptime: AtomicReference<CpuPowerCost?> by lazy {
        AtomicReference(null)
    }

    private val handler: Handler by lazy {
        object : Handler(executor.getBackgroundThreadLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CPU_POWER_COST_CHECK_MSG -> {
                        val cpuStateSnapshot = cpuStateSnapshotCapture!!.readCpuStateSnapshot()
                        val powerCost = calculateCpuPowerCostFromUptime(cpuStateSnapshot)
                        val lastPowerCost = lastPowerCostFromUptime.get()
                        if (lastPowerCost != null) {
                            val durationCpuPowerCost = calculateCpuPowerCostBetweenTwoPoint(lastPowerCost, powerCost)
                            // tApmLog.d(TAG, durationCpuPowerCost.toString())
                            dispatchMonitorData(durationCpuPowerCost)
                        }
                        lastPowerCostFromUptime.set(powerCost)
                        handler.removeMessages(CPU_POWER_COST_CHECK_MSG)
                        handler.sendEmptyMessageDelayed(CPU_POWER_COST_CHECK_MSG, monitorIntervalInMillis.get())
                    }
                }
            }
        }
    }

    override fun onInit(apm: tApm) {
        this.isSupportPrivate = if (cpuStateSnapshotCapture != null && powerProfile != null) {
            try {
                check()
                tApmLog.d(TAG, "Init CpuPowerCostMonitor success.")
                true
            } catch (e: Throwable) {
                tApmLog.e(TAG, "Init CpuPowerMonitor fail.", e)
                false
            }
        } else {
            tApmLog.e(TAG, "Init CpuPowerMonitor fail.")
            false
        }
    }

    override fun onStart(apm: tApm) {
        handler.removeMessages(CPU_POWER_COST_CHECK_MSG)
        handler.sendEmptyMessage(CPU_POWER_COST_CHECK_MSG)
        tApmLog.d(TAG, "CpuPowerCostMonitor started.")
    }

    override fun onStop(apm: tApm) {
        handler.removeMessages(CPU_POWER_COST_CHECK_MSG)
        cpuStateSnapshotCapture?.clearBufferPoolMemory()
        tApmLog.d(TAG, "CpuPowerCostMonitor stopped.")
    }

    private fun check() {
        val cpuStateSnapshot = cpuStateSnapshotCapture!!.readCpuStateSnapshot()
        val coreStates = cpuStateSnapshot.coreStates
        val cpuProfile = powerProfile!!.cpuProfile
        for (s in coreStates) {
            val cluster = cpuProfile.cluster.find { s.coreIndex in it.coreIndexRange } ?: error("Don't find cluster for cpuIndex: ${s.coreIndex}")
            for ((speed, _) in s.cpuSpeedToTime) {
                cluster.frequencies.find { it.speedKhz == speed } ?: error("Don't find coreIndex=${s.coreIndex} speed=${speed} in power profile.")
            }
        }

        for ((coreIndex, cpuSpeedToTime) in cpuStateSnapshot.currentProcessCpuSpeedToTime) {
            val cluster = cpuProfile.cluster.find { coreIndex in it.coreIndexRange } ?: error("Don't find cluster for cpuIndex: $coreIndex")
            for ((speed, _) in cpuSpeedToTime) {
                cluster.frequencies.find { it.speedKhz == speed } ?: error("Don't find coreIndex=$coreIndex speed=$speed in power profile.")
            }
        }
    }

    private fun calculateCpuPowerCostFromUptime(cpuStateSnapshot: CpuStateSnapshotCapture.Companion.CpuStateSnapshot): CpuPowerCost {
        val uptimeInHour = SystemClock.uptimeMillis().millisToHours()

        fun calculateClusterPowerCost(
            cluster: CpuProfile.Companion.Cluster
        ): CpuClusterPowerCost {
            val coreState = cpuStateSnapshot.coreStates.find { it.coreIndex == cluster.coreIndexRange.first }!!
            val activeTimeInHour = coreState.cpuSpeedToTime.sumOf { it.second }.jiffiesToHours()
            var powerCost = cluster.onMa * activeTimeInHour
            val clusterSpeedPowerCosts: MutableList<CpuClusterSpeedPowerCost> = mutableListOf()
            for ((speed, timeCostInJiffies) in coreState.cpuSpeedToTime) {
                val timeCostInHour = timeCostInJiffies.jiffiesToHours()
                val speedExtraPower = cluster.frequencies.find { it.speedKhz == speed }!!.onMa
                val p = timeCostInHour * speedExtraPower * cluster.coreCount.toDouble()
                powerCost += p
                clusterSpeedPowerCosts.add(
                    CpuClusterSpeedPowerCost(
                        speedInKhz = speed,
                        activeTimeInHour = timeCostInHour,
                        powerCostInMah = p
                    )
                )
            }
            return CpuClusterPowerCost(
                coreIndexRange = cluster.coreIndexRange,
                activeTimeInHour = activeTimeInHour,
                powerCostDetails = clusterSpeedPowerCosts,
                powerCostInMah = powerCost
            )
        }
        val clusterPowerCosts = mutableListOf<CpuClusterPowerCost>()
        for (cluster in powerProfile!!.cpuProfile.cluster) {
            clusterPowerCosts.add(calculateClusterPowerCost(cluster))
        }

        // TODO: We don't known idle time，So can't calculate cpu idle power cost.
        val cpuProfile = powerProfile!!.cpuProfile
        var cpuPowerInMha = cpuProfile.suspendMa * uptimeInHour + cpuProfile.activeMa * clusterPowerCosts.maxBy { it.activeTimeInHour }.activeTimeInHour
        for (clusterPowerCost in clusterPowerCosts) {
            cpuPowerInMha += clusterPowerCost.powerCostInMah
        }

        var currentProcessPowerCost = 0.0
        for ((coreIndex, speedAndTimes) in cpuStateSnapshot.currentProcessCpuSpeedToTime) {
            val cluster = cpuProfile.cluster.find { coreIndex in it.coreIndexRange }!!
            val activeTimeInJiffies = speedAndTimes.sumOf { it.second }
            var power = cluster.onMa * activeTimeInJiffies.jiffiesToHours()
            for ((speedInKhz, timeInJiffies) in speedAndTimes) {
                val freq = cluster.frequencies.find { it.speedKhz == speedInKhz }!!
                power += freq.onMa * timeInJiffies.jiffiesToHours()
            }
            currentProcessPowerCost += power
        }

        return CpuPowerCost(
            startTimeInMillis = System.currentTimeMillis(),
            endTimeInMillis = 0L,
            powerCostDetails = clusterPowerCosts,
            powerCostInMah = cpuPowerInMha,
            currentProcessPowerCostInMah = currentProcessPowerCost
        )
    }


    private fun calculateCpuPowerCostBetweenTwoPoint(start: CpuPowerCost, end: CpuPowerCost): CpuPowerCost {
        return CpuPowerCost(
            startTimeInMillis = start.startTimeInMillis,
            endTimeInMillis = end.startTimeInMillis,
            powerCostInMah = end.powerCostInMah - start.powerCostInMah,
            currentProcessPowerCostInMah = end.currentProcessPowerCostInMah - start.currentProcessPowerCostInMah,
            powerCostDetails = Array(end.powerCostDetails.size) { index ->
                val endCluster = end.powerCostDetails[index]
                val startCluster = start.powerCostDetails[index]
                CpuClusterPowerCost(
                    coreIndexRange = endCluster.coreIndexRange,
                    activeTimeInHour = endCluster.activeTimeInHour - startCluster.activeTimeInHour,
                    powerCostInMah = endCluster.powerCostInMah - startCluster.powerCostInMah,
                    powerCostDetails = Array(endCluster.powerCostDetails.size) { speedIndex ->
                        val startSpeed = startCluster.powerCostDetails.getOrNull(speedIndex)
                        val endSpeed = endCluster.powerCostDetails[speedIndex]
                        CpuClusterSpeedPowerCost(
                            speedInKhz = endSpeed.speedInKhz,
                            activeTimeInHour = endSpeed.activeTimeInHour - (startSpeed?.activeTimeInHour ?: 0.0),
                            powerCostInMah = endSpeed.powerCostInMah - (startSpeed?.powerCostInMah ?: 0.0)
                        )
                    }.toList()
                )
            }.toList()
        )
    }

    companion object {


        private const val TAG = "CpuPowerCostMonitor"

        // 20 min
        private const val CPU_POWER_COST_CHECK_INTERNAL = 60L * 1000L * 20L

        private const val CPU_POWER_COST_CHECK_MSG = 0
    }
}