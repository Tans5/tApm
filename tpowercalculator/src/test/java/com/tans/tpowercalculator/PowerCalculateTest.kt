package com.tans.tpowercalculator

import org.junit.Test
import java.util.Locale
import kotlin.math.max

/**
 * power_profile:
 * <device>
 *     <item name="battery.capacity">4410.0</item>
 *     <array name="cpu.clusters.cores">
 *         <value>4.0</value>
 *         <value>2.0</value>
 *         <value>2.0</value>
 *     </array>
 *     <item name="cpu.suspend">10.0</item>
 *     <item name="cpu.idle">28.6</item>
 *     <item name="cpu.active">12.37</item>
 *     <item name="cpu.cluster_power.cluster0">0.24</item>
 *     <item name="cpu.cluster_power.cluster1">3.23</item>
 *     <item name="cpu.cluster_power.cluster2">5.94</item>
 *     <array name="cpu.core_speeds.cluster0">
 *         <value>300000.0</value>
 *         <value>574000.0</value>
 *         <value>738000.0</value>
 *         <value>930000.0</value>
 *         <value>1098000.0</value>
 *         <value>1197000.0</value>
 *         <value>1328000.0</value>
 *         <value>1401000.0</value>
 *         <value>1598000.0</value>
 *         <value>1704000.0</value>
 *         <value>1803000.0</value>
 *     </array>
 *     <array name="cpu.core_speeds.cluster1">
 *         <value>400000.0</value>
 *         <value>553000.0</value>
 *         <value>696000.0</value>
 *         <value>799000.0</value>
 *         <value>910000.0</value>
 *         <value>1024000.0</value>
 *         <value>1197000.0</value>
 *         <value>1328000.0</value>
 *         <value>1491000.0</value>
 *         <value>1663000.0</value>
 *         <value>1836000.0</value>
 *         <value>1999000.0</value>
 *         <value>2130000.0</value>
 *         <value>2253000.0</value>
 *     </array>
 *     <array name="cpu.core_speeds.cluster2">
 *         <value>500000.0</value>
 *         <value>851000.0</value>
 *         <value>984000.0</value>
 *         <value>1106000.0</value>
 *         <value>1277000.0</value>
 *         <value>1426000.0</value>
 *         <value>1582000.0</value>
 *         <value>1745000.0</value>
 *         <value>1826000.0</value>
 *         <value>2048000.0</value>
 *         <value>2188000.0</value>
 *         <value>2252000.0</value>
 *         <value>2401000.0</value>
 *         <value>2507000.0</value>
 *         <value>2630000.0</value>
 *         <value>2704000.0</value>
 *         <value>2802000.0</value>
 *     </array>
 *     <array name="cpu.core_power.cluster0">
 *         <value>1.89</value>
 *         <value>6.15</value>
 *         <value>9.34</value>
 *         <value>14.22</value>
 *         <value>18.94</value>
 *         <value>21.98</value>
 *         <value>26.83</value>
 *         <value>30.17</value>
 *         <value>41.55</value>
 *         <value>48.36</value>
 *         <value>58.45</value>
 *     </array>
 *     <array name="cpu.core_power.cluster1">
 *         <value>3.71</value>
 *         <value>6.16</value>
 *         <value>8.0</value>
 *         <value>10.94</value>
 *         <value>12.73</value>
 *         <value>14.4</value>
 *         <value>21.39</value>
 *         <value>24.1</value>
 *         <value>30.42</value>
 *         <value>42.49</value>
 *         <value>49.37</value>
 *         <value>58.09</value>
 *         <value>67.54</value>
 *         <value>79.04</value>
 *     </array>
 *     <array name="cpu.core_power.cluster2">
 *         <value>8.36</value>
 *         <value>16.33</value>
 *         <value>19.44</value>
 *         <value>36.71</value>
 *         <value>41.42</value>
 *         <value>48.24</value>
 *         <value>54.77</value>
 *         <value>65.32</value>
 *         <value>69.58</value>
 *         <value>128.49</value>
 *         <value>142.15</value>
 *         <value>149.74</value>
 *         <value>164.78</value>
 *         <value>188.68</value>
 *         <value>193.15</value>
 *         <value>227.98</value>
 *         <value>254.25</value>
 *     </array>
 *     <item name="ambient.on">32.0</item>
 *     <item name="screen.on">98.0</item>
 *     <item name="screen.full">470.0</item>
 *     <item name="camera.flashlight">240.47</item>
 *     <item name="camera.avg">900.0</item>
 *     <item name="video">25.0</item>
 *     <item name="audio">75.0</item>
 *     <item name="modem.controller.sleep">0.0</item>
 *     <item name="modem.controller.idle">156.0</item>
 *     <item name="modem.controller.rx">145.0</item>
 *     <array name="modem.controller.tx">
 *         <value>153.0</value>
 *         <value>212.0</value>
 *         <value>292.0</value>
 *         <value>359.0</value>
 *         <value>471.0</value>
 *     </array>
 *     <item name="modem.controller.voltage">3700.0</item>
 *     <array name="gps.signalqualitybased">
 *         <value>14.33</value>
 *         <value>12.79</value>
 *     </array>
 *     <item name="gps.voltage">3700.0</item>
 *     <item name="wifi.controller.idle">38.0</item>
 *     <item name="wifi.controller.rx">98.0</item>
 *     <item name="wifi.controller.tx">470.0</item>
 *     <item name="wifi.controller.voltage">3700.0</item>
 *     <item name="bluetooth.controller.idle">2.2</item>
 *     <item name="bluetooth.controller.rx">5.8</item>
 *     <item name="bluetooth.controller.tx">20.0</item>
 *     <item name="bluetooth.controller.voltage">3850.0</item>
 * </device>
 */
class PowerCalculateTest {

    private val cpuSuspend: Double = 10.0
    private val cpusIdle: Double = 28.6
    private val cpuActive: Double = 12.37
    private val cpuClusterPowerCluster0 = 0.24
    private val cpuClusterPowerCluster1 = 3.23
    private val cpuClusterPowerCluster2 = 5.94
    private val cpuCoreSpeedsCluster0 = longArrayOf(
        300000,
        574000,
        738000,
        930000,
        1098000,
        1197000,
        1328000,
        1401000,
        1598000,
        1704000,
        1803000,
    )
    private val cpuCorePowerCluster0 = doubleArrayOf(
        1.89,
        6.15,
        9.34,
        14.22,
        18.94,
        21.98,
        26.83,
        30.17,
        41.55,
        48.36,
        58.45,
    )

    private val cpuCoreSpeedsCluster1 = longArrayOf(
        400000,
        553000,
        696000,
        799000,
        910000,
        1024000,
        1197000,
        1328000,
        1491000,
        1663000,
        1836000,
        1999000,
        2130000,
        2253000,
    )
    private val cpuCorePowerCluster1 = doubleArrayOf(
        3.71,
        6.16,
        8.0,
        10.94,
        12.73,
        14.4,
        21.39,
        24.1,
        30.42,
        42.49,
        49.37,
        58.09,
        67.54,
        79.04,
    )

    private val cpuCoreSpeedsCluster2 = longArrayOf(
        500000,
        851000,
        984000,
        1106000,
        1277000,
        1426000,
        1582000,
        1745000,
        1826000,
        2048000,
        2188000,
        2252000,
        2401000,
        2507000,
        2630000,
        2704000,
        2802000
    )
    private val cpuCorePowerCluster2 = doubleArrayOf(
        8.36,
        16.33,
        19.44,
        36.71,
        41.42,
        48.24,
        54.77,
        65.32,
        69.58,
        128.49,
        142.15,
        149.74,
        164.78,
        188.68,
        193.15,
        227.98,
        254.25
    )


    /**
     * Cluster0 (4 cores):
     * 300000 7095890
     * 574000 289404
     * 738000 236020
     * 930000 98347
     * 1098000 31118
     * 1197000 48818
     * 1328000 25985
     * 1401000 9260
     * 1598000 11101
     * 1704000 5630
     * 1803000 72130
     *
     * Cluster1 (2 cores):
     * 400000 7788706
     * 553000 20455
     * 696000 9286
     * 799000 6210
     * 910000 6652
     * 1024000 5272
     * 1197000 6069
     * 1328000 3539
     * 1491000 37000
     * 1663000 2858
     * 1836000 2006
     * 1999000 2188
     * 2130000 1573
     * 2253000 31893
     *
     * Cluster2 (2 cores):
     * 500000 7851333
     * 851000 5342
     * 984000 2268
     * 1106000 1534
     * 1277000 2005
     * 1426000 2723
     * 1582000 1322
     * 1745000 5086
     * 1826000 35781
     * 2048000 2665
     * 2188000 1078
     * 2252000 508
     * 2401000 758
     * 2507000 638
     * 2630000 1097
     * 2704000 862
     * 2802000 8706
     *
     * Uptime:
     * 328926.48 620128.41
     *
     */
    @Test
    fun calculateCpuPower() {
        fun Long.jiffiesToHours(): Double {
            return this * 10.0 / (60.0 * 60.0 * 1000.0)
        }
        fun Double.secondsToHours(): Double {
            return this / (60.0 * 60.0)
        }
        fun Double.toHoursString(): String {
            return String.format(Locale.US, "%.2f H", this)
        }
        fun Long.toCpuSpeedString(): String {
            return String.format(Locale.US, "%.2f GHz", this / 1_000_000.0)
        }
        fun Double.toPowerString(): String {
            return String.format(Locale.US, "%.2f mAh", this)
        }
        val uptimeInHour = (328926.48).secondsToHours()
        println("Uptime: ${uptimeInHour.toHoursString()}")
        val cluster0SpeedAndTimeCostInJiffies = mapOf(
            300000L to 7095890L,
            574000L to 289404L,
            738000L to 236020L,
            930000L to 98347L,
            1098000L to 31118L,
            1197000L to 48818L,
            1328000L to 25985L,
            1401000L to 9260L,
            1598000L to 11101L,
            1704000L to 5630L,
            1803000L to 72130L,
        )
        val cluster1SpeedAndTimeCostInJiffies = mapOf(
            400000L to 7788706L,
            553000L to 20455L,
            696000L to 9286L,
            799000L to 6210L,
            910000L to 6652L,
            1024000L to 5272L,
            1197000L to 6069L,
            1328000L to 3539L,
            1491000L to 37000L,
            1663000L to 2858L,
            1836000L to 2006L,
            1999000L to 2188L,
            2130000L to 1573L,
            2253000L to 31893L,
        )
        val cluster2SpeedAndTimeCostInJiffies = mapOf(
            500000L to  7851333L,
            851000L to  5342L,
            984000L to  2268L,
            1106000L to  1534L,
            1277000L to  2005L,
            1426000L to  2723L,
            1582000L to  1322L,
            1745000L to  5086L,
            1826000L to  35781L,
            2048000L to  2665L,
            2188000L to  1078L,
            2252000L to  508L,
            2401000L to  758L,
            2507000L to  638L,
            2630000L to  1097L,
            2704000L to  862L,
            2802000L to  8706L,
        )

        fun calculateClusterPowerCost(
            clusterName: String,
            clusterCoreCount: Int,
            clusterPower: Double,
            clusterSpeeds: LongArray,
            clusterSpeedExtraPower: DoubleArray,
            clusterSpeedAndTimeCostInJiffies: Map<Long, Long>
        ): Double {
            val activeTime = clusterSpeedAndTimeCostInJiffies.values.sum().jiffiesToHours()
            println("----------------------------------------------------------------")
            println("Cluster: $clusterName, ActiveTime: ${activeTime.toHoursString()}, CoreCount: $clusterCoreCount")
            var powerCost = clusterPower * activeTime
            for ((speed, timeCostInJiffies) in clusterSpeedAndTimeCostInJiffies) {
                val timeCostInHour = timeCostInJiffies.jiffiesToHours()
                val speedExtraPower = clusterSpeedExtraPower[clusterSpeeds.indexOf(speed)]
                val c = (timeCostInHour * speedExtraPower) * clusterCoreCount
                println("Speed: ${speed.toCpuSpeedString()}, PowerCost: ${c.toPowerString()}, TimeCost: ${timeCostInHour.toHoursString()}")
                powerCost += c
            }
            println("PowerCost: ${powerCost.toPowerString()}")
            println("----------------------------------------------------------------")
            return powerCost
        }

        val cluster0PowerCost = calculateClusterPowerCost(
            clusterName = "Cluster0",
            clusterCoreCount = 4,
            clusterPower = cpuClusterPowerCluster0,
            clusterSpeeds = cpuCoreSpeedsCluster0,
            clusterSpeedExtraPower = cpuCorePowerCluster0,
            clusterSpeedAndTimeCostInJiffies = cluster0SpeedAndTimeCostInJiffies
        )

        val cluster1PowerCost = calculateClusterPowerCost(
            clusterName = "Cluster1",
            clusterCoreCount = 2,
            clusterPower = cpuClusterPowerCluster1,
            clusterSpeeds = cpuCoreSpeedsCluster1,
            clusterSpeedExtraPower = cpuCorePowerCluster1,
            clusterSpeedAndTimeCostInJiffies = cluster1SpeedAndTimeCostInJiffies
        )

        val cluster2PowerCost = calculateClusterPowerCost(
            clusterName = "Cluster2",
            clusterCoreCount = 2,
            clusterPower = cpuClusterPowerCluster2,
            clusterSpeeds = cpuCoreSpeedsCluster2,
            clusterSpeedExtraPower = cpuCorePowerCluster2,
            clusterSpeedAndTimeCostInJiffies = cluster2SpeedAndTimeCostInJiffies
        )

        // TODO: We don't known idle time，So can't calculate cpu idle power cost.
        // val cpuPower = cpuSuspend * uptimeInHour  + cpuActive * cpuActiveTimeInHour + cpusIdle * cpuIdleTimeInHour
        val cpuPower = cpuSuspend * uptimeInHour + cpuActive * (max(max(cluster0SpeedAndTimeCostInJiffies.values.sum(), cluster1SpeedAndTimeCostInJiffies.values.sum()), cluster2SpeedAndTimeCostInJiffies.values.sum()).jiffiesToHours())

        println("CpuPowerCost: ${(cpuPower + cluster0PowerCost + cluster1PowerCost + cluster2PowerCost).toPowerString()}")
    }
}