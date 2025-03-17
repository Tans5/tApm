package com.tans.tapm

import android.annotation.SuppressLint
import android.app.Application
import com.tans.tapm.internal.tApmLog
import org.xmlpull.v1.XmlPullParser

/**
 * ambient.on	Additional power used when screen is in doze/ambient/always-on mode instead of off.	around 100 mA	-
 * screen.on	Additional power used when screen is turned on at minimum brightness.	200 mA	Includes touch controller and display backlight. At 0 brightness, not the Android minimum which tends to be 10 or 20%.
 * screen.full	Additional power used when screen is at maximum brightness, compared to screen at minimum brightness.	100 mA-300 mA	A fraction of this value (based on screen brightness) is added to the screen.on value to compute the power usage of the screen.
 * wifi.on	Additional power used when Wi-Fi is turned on but not receiving, transmitting, or scanning.	2 mA	-
 * wifi.active	Additional power used when transmitting or receiving over Wi-Fi.	31 mA	-
 * wifi.scan	Additional power used when Wi-Fi is scanning for access points.	100 mA	-
 * audio	Additional power used when audio decoding/encoding via DSP.	around 10 mA	Used for DSP audio.
 * video	Additional power used when video decoding via DSP.	around 50 mA	Used for DSP video.
 * camera.avg	Average power use by the camera subsystem for a typical camera app.	600 mA	Intended as a rough estimate for an app running a preview and capturing approximately 10 full-resolution pictures per minute.
 * camera.flashlight	Average power used by the camera flash module when on.	200 mA	-
 * gps.signalqualitybased	Additional power used by GPS based on signal strength. This is a multi-value entry, one per signal strength, from weakest to strongest.	30 mA, 10 mA	-
 * gps.on	Additional power used when GPS is acquiring a signal.	50 mA	-
 * radio.active	Additional power used when cellular radio is transmitting/receiving.	100 mA-300 mA	-
 * radio.scanning	Additional power used when cellular radio is paging the tower.	1.2 mA	-
 * radio.on	Additional power used when the cellular radio is on. This is a multi-value entry, one per signal strength (no signal, weak, moderate, strong).	1.2 mA	Some radios boost power when they search for a cell tower and don't detect a signal. Values can be the same or decrease with increasing signal strength. If you provide only one value, the same value is used for all strengths. If you provide two values, the first is used for no-signal, the second value is used for all other strengths, and so on.
 * bluetooth.controller.idle	Average current draw (mA) of the Bluetooth controller when idle.	-	These values aren't estimated, but taken from the data sheet of the controller. If there are multiple receive or transmit states, the average of those states is taken. In addition, the system now collects data for Low Energy (LE) and Bluetooth scans.
 *
 * Android 7.0 and later no longer use the Bluetooth power values for bluetooth.active (used when playing audio via Bluetooth A2DP) and bluetooth.on (used when Bluetooth is on but idle).
 * bluetooth.controller.rx	Average current draw (mA) of the Bluetooth controller when receiving.	-
 * bluetooth.controller.tx	Average current draw (mA) of the Bluetooth controller when transmitting.	-
 * bluetooth.controller.voltage	Average operating voltage (mV) of the Bluetooth controller.	-
 * modem.controller.sleep	Average current draw (mA) of the modem controller when asleep.	0 mA	These values aren't estimated, but taken from the data sheet of the controller. If there are multiple receive states, the average of those states is taken. If there are multiple transmit states, specifying a value for each transmit state is supported starting in Android 9.
 * modem.controller.idle	Average current draw (mA) of the modem controller when idle.	-
 * modem.controller.rx	Average current draw (mA) of the modem controller when receiving.	-
 * modem.controller.tx	Average current draw (mA) of the modem controller when transmitting at different RF power levels. This is a multi-value entry with one value per transmit power level.	100 mA, 200 mA, 300 mA, 400 mA, 500 mA
 * modem.controller.voltage	Average operating voltage (mV) of the modem controller.	-
 * wifi.controller.idle	Average current draw (mA) of the Wi-Fi controller when idle.	-	These values aren't estimated, but taken from the data sheet of the controller. If there are multiple receive or transmit states, the average of those states is taken.
 * wifi.controller.rx	Average current draw (mA) of the Wi-Fi controller when receiving.	-
 * wifi.controller.tx	Average current draw (mA) of the Wi-Fi controller when transmitting.	-
 * wifi.controller.voltage	Average operating voltage (mV) of the Wi-Fi controller.	-
 * cpu.speeds	This is a multi-value entry that lists each possible CPU speed in KHz.	125000 KHz, 250000 KHz, 500000 KHz, 1000000 KHz, 1500000 KHz	The number and order of entries must correspond to the mA entries in cpu.active.
 * cpu.idle	Total power drawn by the system when CPUs (and the SoC) are in system suspend state.	3 mA	-
 * cpu.awake	Additional power used when CPUs are in scheduling idle state (kernel idle loop); system isn't in system suspend state.	50 mA	Your platform might have more than one idle state in use with differing levels of power consumption; choose a representative idle state for longer periods of scheduler idle (several milliseconds). Examine the power graph on your measurement equipment and choose samples where the CPU is at its lowest consumption, discarding higher samples where the CPU exited idle.
 * cpu.active	Additional power used by CPUs when running at different speeds.	100 mA, 120 mA, 140 mA, 160 mA, 200 mA	Value represents the power used by the CPU rails when running at different speeds. Set the max speed in the kernel to each of the allowed speeds and peg the CPU at that speed. The number and order of entries correspond to the number and order of entries in cpu.speeds.
 * cpu.clusters.cores	Number of cores each CPU cluster contains.	4, 2	Required only for devices with heterogeneous CPU architectures. Number of entries and order should match the number of cluster entries for the cpu.active and cpu.speeds. The first entry represents the number of CPU cores in cluster0, the second entry represents the number of CPU cores in cluster1, and so on.
 * battery.capacity	Total battery capacity in mAh.	3000 mAh	-
 */

class PowerProfile private constructor(
    val cpuProfile: ComponentProfile.CpuProfile,
    val screenProfile: ComponentProfile.ScreenProfile,
    val audioProfile: ComponentProfile.AudioProfile,
    val bluetoothProfile: ComponentProfile.BluetoothProfile,
    val cameraProfile: ComponentProfile.CameraProfile,
    val flashlightProfile: ComponentProfile.FlashlightProfile,
    val gpsProfile: ComponentProfile.GpsProfile,
    val modemProfile: ComponentProfile.ModemProfile,
    val videoProfile: ComponentProfile.VideoProfile,
    val wifiProfile: ComponentProfile.WifiProfile,
    val batteryCapacity: Int
) {

    init {
        tApmLog.d(TAG, "CpuProfile: $cpuProfile")
        tApmLog.d(TAG, "ScreenProfile: $screenProfile")
        tApmLog.d(TAG, "AudioProfile: $audioProfile")
        tApmLog.d(TAG, "BluetoothProfile: $bluetoothProfile")
        tApmLog.d(TAG, "CameraProfile: $cameraProfile")
        tApmLog.d(TAG, "FlashlightProfile: $flashlightProfile")
        tApmLog.d(TAG, "GpsProfile: $gpsProfile")
        tApmLog.d(TAG, "ModemProfile: $modemProfile")
        tApmLog.d(TAG, "VideoProfile: $videoProfile")
        tApmLog.d(TAG, "WifiProfile: $wifiProfile")
        tApmLog.d(TAG, "BatteryCapacity: $batteryCapacity")
    }

    companion object {
        private const val TAG = "PowerProfile"

        sealed class ComponentProfile {

            /**
             * CPU Power Equation (assume two clusters):
             * Total power = POWER_CPU_SUSPEND  (always added)
             *               + POWER_CPU_IDLE   (skip this and below if in power collapse mode)
             *               + POWER_CPU_ACTIVE (skip this and below if CPU is not running, but a wakelock
             *                                   is held)
             *               + cluster_power.cluster0 + cluster_power.cluster1 (skip cluster not running)
             *               + core_power.cluster0 * num running cores in cluster 0
             *               + core_power.cluster1 * num running cores in cluster 1
             */
            data class CpuProfile(
                val suspendMa: Float,
                val idleMa: Float,
                val activeMa: Float,
                val cluster: List<Cluster>,
                val coreCount: Int,
                // cpu.active,
                val activeMaList: List<Float>
            ) : ComponentProfile() {

                companion object {

                    data class Cluster(
                        val coreCount: Int,
                        val onMa: Float,
                        val frequencies: List<Frequency>,
                        val coreIndexRange: IntRange
                    )

                    data class Frequency(
                        val speedKhz: Long,
                        val onMa: Float
                    )

                    class Builder {
                        var suspendMa: Float = 0.0f
                        var idleMa: Float = 0.0f
                        var activeMa: Float = 0.0f
                        val coreCount: MutableList<Int> = mutableListOf()
                        val clusterOnPower: MutableMap<Int, Float> = mutableMapOf()
                        val coreSpeeds: MutableMap<Int, List<Long>> = mutableMapOf()
                        val corePower: MutableMap<Int, List<Float>> = mutableMapOf()

                        var clusterMa: List<Float> = emptyList()
                        val clusterSpeeds: MutableMap<Int, List<Long>> = mutableMapOf()
                        val clusterActiveMa: MutableMap<Int, List<Float>> = mutableMapOf()

                        fun build(): CpuProfile {
                            val clusters = mutableListOf<Cluster>()
                            var clusterCoreIndexStart = 0
                            for ((clusterIndex, clusterCoreCount) in coreCount.withIndex()) {
                                val clusterPower = clusterOnPower[clusterIndex] ?: (clusterMa.getOrNull(clusterIndex) ?: 0.0f)
                                val speeds = coreSpeeds[clusterIndex] ?: clusterSpeeds[clusterIndex]
                                val power = corePower[clusterIndex] ?: clusterActiveMa[clusterIndex]
                                if (speeds != null && power != null && speeds.size != power.size) {
                                    error("Wrong Cpu speeds count: ${speeds.size}, power count: ${power.size}")
                                }
                                val frequencies = mutableListOf<Frequency>()
                                val fRange = speeds?.indices ?: (power?.indices ?: (0 until 0))
                                for (i in fRange) {
                                    val s = speeds?.get(i) ?: 0
                                    val p = power?.get(i) ?: 0.0f
                                    frequencies.add(
                                        Frequency(
                                            speedKhz = s,
                                            onMa = p
                                        )
                                    )
                                }
                                val coreIndexRange = clusterCoreIndexStart until (clusterCoreIndexStart + clusterCoreCount)
                                clusterCoreIndexStart += clusterCoreCount
                                clusters.add(
                                    Cluster(
                                        coreCount = clusterCoreCount,
                                        onMa = clusterPower,
                                        frequencies = frequencies,
                                        coreIndexRange = coreIndexRange
                                    )
                                )
                            }
                            return CpuProfile(
                                suspendMa = suspendMa,
                                idleMa = idleMa,
                                activeMa = activeMa,
                                cluster = clusters,
                                coreCount = clusters.sumOf { it.coreCount },
                                activeMaList = clusterMa
                            )

                        }
                    }
                }
            }

            data class ScreenProfile(
                val ambientMa: Float,
                val onMa: Float,
                val fullMa: Float,
                val screensAmbientMa: Map<Int, Float>,
                val screensOnMa: Map<Int, Float>,
                val screensFullMa: Map<Int, Float>,
            ) : ComponentProfile() {

                companion object {
                    class Builder {
                        var ambientMa: Float = 0.0f
                        var onMa: Float = 0.0f
                        var fullMa: Float = 0.0f
                        val screensAmbientMa: MutableMap<Int, Float> = mutableMapOf()
                        val screensOnMa: MutableMap<Int, Float> = mutableMapOf()
                        val screensFullMa: MutableMap<Int, Float> = mutableMapOf()

                        fun build(): ScreenProfile {
                            return ScreenProfile(
                                ambientMa = ambientMa,
                                onMa = onMa,
                                fullMa = fullMa,
                                screensAmbientMa = screensAmbientMa,
                                screensOnMa = screensOnMa,
                                screensFullMa = screensFullMa
                            )
                        }
                    }
                }
            }

            data class AudioProfile(
                val onMa: Float
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f

                        fun build(): AudioProfile {
                            return AudioProfile(
                                onMa = onMa,
                            )
                        }
                    }
                }
            }

            data class BluetoothProfile(
                val onMa: Float,
                val activeMa: Float,
                val idleMa: Float,
                val rxMa: Float,
                val txMa: Float
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f
                        var activeMa: Float = 0.0f
                        var idleMa: Float = 0.0f
                        var rxMa: Float = 0.0f
                        var txMa: Float = 0.0f

                        fun build(): BluetoothProfile {
                            return BluetoothProfile(
                                onMa = onMa,
                                activeMa = activeMa,
                                idleMa = idleMa,
                                rxMa = rxMa,
                                txMa = txMa
                            )
                        }
                    }
                }
            }

            data class CameraProfile(
                val onMa: Float
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f

                        fun build(): CameraProfile {
                            return CameraProfile(
                                onMa = onMa,
                            )
                        }
                    }
                }
            }

            data class FlashlightProfile(
                val onMa: Float
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f

                        fun build(): FlashlightProfile {
                            return FlashlightProfile(
                                onMa = onMa,
                            )
                        }
                    }
                }
            }

            data class GpsProfile(
                val onMa: Float,
                val signalMa: List<Float>
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f
                        val signalMa: MutableList<Float> = mutableListOf()

                        fun build(): GpsProfile {
                            return GpsProfile(
                                onMa = onMa,
                                signalMa = signalMa
                            )
                        }
                    }
                }
            }

            data class ModemProfile(
                val onMa: List<Float>,
                val activeMa: Float,
                val sleepMa: Float,
                val idleMa: Float,
                val rxMa: Float,
                val scanningMa: Float,
                val txMa: List<Float>
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: List<Float> = emptyList()
                        var activeMa: Float = 0.0f
                        var sleepMa: Float = 0.0f
                        var idleMa: Float = 0.0f
                        var rxMa: Float = 0.0f
                        var scanningMa: Float = 0.0f
                        val txMa: MutableList<Float> = mutableListOf()

                        fun build(): ModemProfile {
                            return ModemProfile(
                                onMa = onMa,
                                activeMa = activeMa,
                                sleepMa = sleepMa,
                                idleMa = idleMa,
                                rxMa = rxMa,
                                scanningMa = scanningMa,
                                txMa = txMa
                            )
                        }
                    }
                }
            }

            data class VideoProfile(
                val onMa: Float
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f

                        fun build(): VideoProfile {
                            return VideoProfile(
                                onMa = onMa,
                            )
                        }
                    }
                }
            }

            data class WifiProfile(
                val onMa: Float,
                val activeMa: Float,
                val scanMa: Float,
                val idleMa: Float,
                val rxMa: Float,
                val txMa: Float
            ) : ComponentProfile() {

                companion object {

                    class Builder {
                        var onMa: Float = 0.0f
                        var activeMa: Float = 0.0f
                        var scanMa: Float = 0.0f
                        var idleMa: Float = 0.0f
                        var rxMa: Float = 0.0f
                        var txMa: Float = 0.0f

                        fun build(): WifiProfile {
                            return WifiProfile(
                                onMa = onMa,
                                activeMa = activeMa,
                                scanMa = scanMa,
                                idleMa = idleMa,
                                rxMa = rxMa,
                                txMa = txMa
                            )
                        }
                    }
                }
            }

        }

        @SuppressLint("DiscouragedApi")
        fun parsePowerProfile(application: Application): PowerProfile? {
            tApmLog.d(TAG, "Do parse power profile.")

            return try {
                val id = application.resources.getIdentifier("power_profile", "xml", "android")
                val parser = application.resources.getXml(id)
                val cpuProfileBuilder = ComponentProfile.CpuProfile.Companion.Builder()
                val screenProfileBuilder = ComponentProfile.ScreenProfile.Companion.Builder()
                val audioProfileBuilder = ComponentProfile.AudioProfile.Companion.Builder()
                val bluetoothProfileBuilder = ComponentProfile.BluetoothProfile.Companion.Builder()
                val cameraProfileBuilder = ComponentProfile.CameraProfile.Companion.Builder()
                val flashlightProfileBuilder = ComponentProfile.FlashlightProfile.Companion.Builder()
                val gpsProfileBuilder = ComponentProfile.GpsProfile.Companion.Builder()
                val modemProfileBuilder = ComponentProfile.ModemProfile.Companion.Builder()
                val videoProfileBuilder = ComponentProfile.VideoProfile.Companion.Builder()
                val wifiProfileBuilder = ComponentProfile.WifiProfile.Companion.Builder()
                var batteryCapacity = 0


                val reClusterPower = "cpu.cluster_power.cluster([0-9]*)".toRegex()
                val reCoreSpeeds = "cpu.core_speeds.cluster([0-9]*)".toRegex()
                val reCorePower = "cpu.core_power.cluster([0-9]*)".toRegex()


                val reScreenAmbient = "ambient.on.display([0-9]*)".toRegex()
                val reScreenOn = "screen.on.display([0-9]*)".toRegex()
                val reScreenFull = "screen.full.display([0-9]*)".toRegex()

                val reCpuClusterSpeed = "cpu.speeds.cluster([0-9]*)".toRegex()
                val reCpuClusterActive = "cpu.active.cluster([0-9]*)".toRegex()

                fun Regex.index(name: String): Int {
                    return this.find(name)!!.groupValues[1].toInt()
                }

                fun onItem(name: String, value: Float) {
                    when  {
                        name == "cpu.suspend" -> {
                            cpuProfileBuilder.suspendMa = value
                        }
                        name == "cpu.idle" -> {
                            cpuProfileBuilder.idleMa = value
                        }
                        name == "cpu.active" -> {
                            cpuProfileBuilder.activeMa = value
                        }
                        reClusterPower.matches(name) -> {
                            val index = reClusterPower.index(name)
                            cpuProfileBuilder.clusterOnPower[index] = value
                        }
                        name == "ambient.on" -> {
                            screenProfileBuilder.ambientMa = value
                        }
                        name == "screen.on" -> {
                            screenProfileBuilder.onMa = value
                        }
                        name == "screen.full" -> {
                            screenProfileBuilder.fullMa = value
                        }
                        reScreenAmbient.matches(name) -> {
                            val index = reScreenAmbient.index(name)
                            screenProfileBuilder.screensAmbientMa[index] = value
                        }
                        reScreenOn.matches(name) -> {
                            val index = reScreenOn.index(name)
                            screenProfileBuilder.screensOnMa[index] = value
                        }
                        reScreenFull.matches(name) -> {
                            val index = reScreenFull.index(name)
                            screenProfileBuilder.screensFullMa[index] = value
                        }
                        name == "audio" -> {
                            audioProfileBuilder.onMa = value
                        }
                        name == "bluetooth.active" -> {
                            bluetoothProfileBuilder.activeMa = value
                        }
                        name == "bluetooth.on" -> {
                            bluetoothProfileBuilder.onMa = value
                        }
                        name == "bluetooth.controller.idle" -> {
                            bluetoothProfileBuilder.idleMa = value
                        }
                        name == "bluetooth.controller.rx" -> {
                            bluetoothProfileBuilder.rxMa = value
                        }
                        name == "bluetooth.controller.tx" -> {
                            bluetoothProfileBuilder.txMa = value
                        }
                        name == "camera.avg" -> {
                            cameraProfileBuilder.onMa = value
                        }
                        name == "camera.flashlight" -> {
                            flashlightProfileBuilder.onMa = value
                        }
                        name == "gps.on" -> {
                            gpsProfileBuilder.onMa = value
                        }
                        name == "radio.active" -> {
                            modemProfileBuilder.activeMa = value
                        }
                        name == "modem.controller.sleep" -> {
                            modemProfileBuilder.sleepMa = value
                        }
                        name == "modem.controller.idle" -> {
                            modemProfileBuilder.idleMa = value
                        }
                        name == "modem.controller.rx" -> {
                            modemProfileBuilder.rxMa = value
                        }
                        name == "radio.scanning" -> {
                            modemProfileBuilder.scanningMa = value
                        }
                        name == "video" -> {
                            videoProfileBuilder.onMa = value
                        }
                        name == "wifi.on" -> {
                            wifiProfileBuilder.onMa = value
                        }
                        name == "wifi.active" -> {
                            wifiProfileBuilder.activeMa = value
                        }
                        name == "wifi.scan" -> {
                            wifiProfileBuilder.scanMa = value
                        }
                        name == "wifi.controller.idle" -> {
                            wifiProfileBuilder.idleMa = value
                        }
                        name == "wifi.controller.rx" -> {
                            wifiProfileBuilder.rxMa = value
                        }
                        name == "wifi.controller.tx" -> {
                            wifiProfileBuilder.txMa = value
                        }
                        name == "battery.capacity" -> {
                            batteryCapacity = value.toInt()
                        }
                        else -> {
                            tApmLog.w(TAG, "Unknown: $name -> $value")
                        }
                    }
                }

                fun onArray(name: String, value: List<Float>) {
                    when {
                        name == "cpu.clusters.cores" -> {
                            cpuProfileBuilder.coreCount.clear()
                            for (i in value) {
                                cpuProfileBuilder.coreCount.add(i.toInt())
                            }
                        }
                        reCoreSpeeds.matches(name) -> {
                            val index = reCoreSpeeds.index(name)
                            cpuProfileBuilder.coreSpeeds[index] = value.map { it.toLong() }
                        }
                        reCorePower.matches(name) -> {
                            val index = reCorePower.index(name)
                            cpuProfileBuilder.corePower[index] = value
                        }
                        name == "gps.signalqualitybased" -> {
                            gpsProfileBuilder.signalMa.clear()
                            gpsProfileBuilder.signalMa.addAll(value)
                        }
                        name == "modem.controller.tx" -> {
                            modemProfileBuilder.txMa.clear()
                            modemProfileBuilder.txMa.addAll(value)
                        }
                        name == "radio.on" -> {
                            modemProfileBuilder.onMa = value
                        }
                        name == "cpu.active" -> {
                            cpuProfileBuilder.clusterMa = value
                        }
                        reCpuClusterSpeed.matches(name) -> {
                            val index = reCpuClusterSpeed.index(name)
                            cpuProfileBuilder.clusterSpeeds[index] = value.map { it.toLong() }
                        }
                        reCpuClusterActive.matches(name) -> {
                            val index = reCpuClusterActive.index(name)
                            cpuProfileBuilder.clusterActiveMa[index] = value
                        }
                        else -> {
                            tApmLog.w(TAG, "Unknown: $name -> $value")
                        }
                    }
                }


                parser.use {

                    fun nextStartTag(): String? {
                        var type: Int
                        do {
                            type = parser.next()
                        } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_TAG && type != XmlPullParser.END_DOCUMENT)
                        return if (type == XmlPullParser.START_TAG) {
                            parser.name
                        } else {
                            null
                        }
                    }

                    // <device>
                    var tagName = nextStartTag()
                    if (tagName != "device") {
                        error("First tag is not <device>")
                    }
                    var attrName: String?
                    var parsingArray: Pair<String, MutableList<Float>>? = null
                    while (true) {
                        tagName = nextStartTag()
                        if (tagName == null) {
                            if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                                break
                            } else {
                                continue
                            }
                        }

                        // array value
                        if (parsingArray != null) {
                            if (tagName != "value") {
                                onArray(name = parsingArray.first, value = parsingArray.second)
                                tApmLog.d(TAG, "</array>")
                                parsingArray = null
                            } else {
                                // <value>
                                val array = parsingArray.second
                                if (parser.next() != XmlPullParser.TEXT) {
                                    tApmLog.w(TAG, "Skip tag <value>, next tag not text")
                                    continue
                                }
                                val t = try {
                                    parser.text.toFloat()
                                } catch (e: Throwable) {
                                    tApmLog.e(TAG, "Wrong text ${parser.text} can't parse to float value.", e)
                                    0.0f
                                }
                                array.add(t)
                                tApmLog.d(TAG, "    <value>${parser.text}</value>")
                                continue
                            }
                        }

                        // <array>
                        if (tagName == "array") {
                            attrName = parser.getAttributeValue(null, "name")
                            if (attrName == null) {
                                tApmLog.w(TAG, "Skip tag <array>, no name attr.")
                                continue
                            }
                            parsingArray = attrName to mutableListOf()
                            tApmLog.d(TAG, "<array name=\"$attrName\">")
                            continue
                        }

                        // <item>
                        if (tagName == "item") {
                            attrName = parser.getAttributeValue(null, "name")
                            if (attrName == null) {
                                tApmLog.w(TAG, "Skip tag <item>, no name attr.")
                                continue
                            }
                            if (parser.next() != XmlPullParser.TEXT) {
                                tApmLog.w(TAG, "Skip tag <item>, next tag not text.")
                                continue
                            }
                            val t = try {
                                parser.text.toFloat()
                            } catch (e: Throwable) {
                                tApmLog.e(TAG, "Wrong text ${parser.text} can't parse to float value.", e)
                                0.0f
                            }
                            tApmLog.d(TAG, "<item name=\"$attrName\">${parser.text}</item>")
                            onItem(name = attrName, value = t)
                            continue
                        }

                        // Ignore
                        attrName = parser.getAttributeValue(null, "name")
                        val t: String? = if (parser.next() == XmlPullParser.TEXT) {
                            parser.text
                        } else {
                            null
                        }
                        tApmLog.d(TAG, "Ignore tag=$tagName${if (attrName != null) ", name=$attrName" else ""}${if (t != null) ", value=$t" else ""}")
                    }
                    if (parsingArray != null) {
                        onArray(name = parsingArray.first, value = parsingArray.second)
                        tApmLog.d(TAG, "</array>")
                    }
                }

                PowerProfile(
                    cpuProfile = cpuProfileBuilder.build(),
                    screenProfile = screenProfileBuilder.build(),
                    audioProfile = audioProfileBuilder.build(),
                    bluetoothProfile = bluetoothProfileBuilder.build(),
                    cameraProfile = cameraProfileBuilder.build(),
                    flashlightProfile = flashlightProfileBuilder.build(),
                    gpsProfile = gpsProfileBuilder.build(),
                    modemProfile = modemProfileBuilder.build(),
                    videoProfile = videoProfileBuilder.build(),
                    wifiProfile = wifiProfileBuilder.build(),
                    batteryCapacity = batteryCapacity
                )
            } catch (e: Throwable) {
                tApmLog.e(TAG, "Parse profile fail: ${e.message}", e)
                null
            }
        }

    }
}