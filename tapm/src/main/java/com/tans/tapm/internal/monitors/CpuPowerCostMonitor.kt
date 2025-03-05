package com.tans.tapm.internal.monitors

import android.os.Handler
import android.os.Message
import android.os.SystemClock
import com.tans.tapm.internal.ComponentProfile
import com.tans.tapm.internal.CpuStateSnapshotCapture
import com.tans.tapm.internal.Executors
import com.tans.tapm.internal.PowerProfile
import com.tans.tapm.internal.jiffiesToHours
import com.tans.tapm.internal.millisToHours
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.CpuClusterPowerCost
import com.tans.tapm.model.CpuClusterSpeedPowerCost
import com.tans.tapm.model.CpuPowerCost

internal class CpuPowerCostMonitor(
    private val powerProfile: PowerProfile,
    private val cpuStateSnapshotCapture: CpuStateSnapshotCapture
) : AbsMonitor<CpuPowerCost>(CPU_POWER_COST_CHECK_INTERNAL) {


    override val isSupport: Boolean = if (cpuStateSnapshotCapture.isInitSuccess) {
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

    private val handler: Handler by lazy {
        object : Handler(Executors.bgHandlerThread.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CPU_POWER_COST_CHECK_MSG -> {
                        val buffer = cpuStateSnapshotCapture.readCpuStateSnapshotBuffer()!!
                        val cpuStateSnapshot = cpuStateSnapshotCapture.parseCpuStateSnapshotBuffer(buffer)
                        val powerCost = calculateCpuPowerCostFromUptime(cpuStateSnapshot)
                        // TODO:


                        handler.removeMessages(CPU_POWER_COST_CHECK_MSG)
                        handler.sendEmptyMessageDelayed(CPU_POWER_COST_CHECK_MSG, monitorIntervalInMillis.get())
                    }
                }
            }
        }
    }

    override fun onStart() {
        handler.removeMessages(CPU_POWER_COST_CHECK_MSG)
        handler.sendEmptyMessage(CPU_POWER_COST_CHECK_MSG)
        tApmLog.d(TAG, "CpuPowerCostMonitor started.")
    }

    override fun onStop() {
        handler.removeMessages(CPU_POWER_COST_CHECK_MSG)
        tApmLog.d(TAG, "CpuPowerCostMonitor stopped.")
    }

    private fun check() {
        val b = cpuStateSnapshotCapture.readCpuStateSnapshotBuffer()!!
        val cpuStateSnapshot = cpuStateSnapshotCapture.parseCpuStateSnapshotBuffer(b)
        val coreStates = cpuStateSnapshot.coreStates
        val cpuProfile = powerProfile.cpuProfile
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
            cluster: ComponentProfile.CpuProfile.Companion.Cluster
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
                        powerCodeInMah = p
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
        for (cluster in powerProfile.cpuProfile.cluster) {
            clusterPowerCosts.add(calculateClusterPowerCost(cluster))
        }

        // TODO: We don't known idle time，So can't calculate cpu idle power cost.
        val cpuProfile = powerProfile.cpuProfile
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

    companion object {


        private const val TAG = "CpuPowerCostMonitor"

        // 20 min
        private const val CPU_POWER_COST_CHECK_INTERNAL = 60L * 1000L * 20L

        private const val CPU_POWER_COST_CHECK_MSG = 0
    }
}