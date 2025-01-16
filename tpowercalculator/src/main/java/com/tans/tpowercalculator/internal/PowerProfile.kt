package com.tans.tpowercalculator.internal

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import org.xmlpull.v1.XmlPullParser

internal class PowerProfile private constructor(
    val application: Application,
    val itemValues: Map<String, String>,
    val arrayValues: Map<String, List<String>>
) {

    val cpuClusters: List<CpuCluster>

    val cpuCoreSize: Int

    init {
        cpuClusters = initCpuClusters()
        for ((index, cluster) in cpuClusters.withIndex()) {
            Log.d(TAG, "Cluster$index=$cluster")
        }
        cpuCoreSize = cpuClusters.sumOf { it.cpuCoreSize }
        Log.d(TAG, "CpuCoreSize=$cpuCoreSize")
        Log.d(TAG, "Init PowerProfile success.")
    }

    fun getCpuClusterByCpuIndex(cpuIndex: Int): CpuCluster {
        return cpuClusters.find { cpuIndex in it.cpuIndexRange }!!
    }

    private fun initCpuClusters(): List<CpuCluster> {
        val result = mutableListOf<CpuCluster>()
        val cpuClusterCoreSizes = arrayValues["cpu.clusters.cores"]?.let { l ->
            // Multi Cpu Clusters
            l.map { it.toInt() }
        } ?: listOf(itemValues["cpu.clusters.cores"]!!.toInt()) // Single Cpu Cluster
        var cpuIndexStart = 0
        for ((index, cpuCoreSize) in cpuClusterCoreSizes.withIndex()) {
            val clusterPower = itemValues["cpu.cluster_power.cluster$index"]!!.toDouble() ?: 0.0
            val cpuCorePower = arrayValues["cpu.core_power.cluster$index"]!!.map { it.toDouble() }
            val cpuCoreSpeed = arrayValues["cpu.core_speeds.cluster$index"]!!.map { it.toLong() }
            if (cpuCoreSpeed.size != cpuCorePower.size || cpuCoreSpeed.isEmpty()) {
                error("CpuCluster$index, wrong CoreSpeedSize and CorePowerSize, CoreSpeedSize=${cpuCoreSpeed.size}, CorePowerSize=${cpuCorePower.size}")
            }
            val cpuIndexRange = cpuIndexStart until (cpuIndexStart + cpuCoreSize)
            cpuIndexStart += cpuCoreSize
            result.add(
                CpuCluster(
                    cpuCoreSize = cpuCoreSize,
                    clusterPower = clusterPower,
                    cpuCorePower = cpuCorePower,
                    cpuCoreSpeed = cpuCoreSpeed,
                    cpuIndexRange = cpuIndexRange
                )
            )
        }
        return result
    }

    data class CpuCluster(
        val cpuCoreSize: Int,
        val clusterPower: Double,
        val cpuCorePower: List<Double>,
        val cpuCoreSpeed: List<Long>,
        val cpuIndexRange: IntRange
    )

    companion object {
        private const val TAG = "PowerProfile"

        @SuppressLint("DiscouragedApi")
        fun parsePowerProfile(application: Application): PowerProfile {
            tPowerLog.d(TAG, "Do parse power profile.")
            val id = application.resources.getIdentifier("power_profile", "xml", "android")
            val parser = application.resources.getXml(id)
            val itemValues = HashMap<String, String>()
            val arrayValues = HashMap<String, List<String>>()
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
                var parsingArray: Pair<String, MutableList<String>>? = null
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
                            arrayValues[parsingArray.first] = parsingArray.second
                            Log.d(TAG, "</array>")
                            parsingArray = null
                        } else {
                            // <value>
                            val array = parsingArray.second
                            if (parser.next() != XmlPullParser.TEXT) {
                                Log.w(TAG, "Skip tag <value>, next tag not text")
                                continue
                            }
                            val t = parser.text
                            array.add(t)
                            Log.d(TAG, "    <value>$t</value")
                            continue
                        }
                    }

                    // <array>
                    if (tagName == "array") {
                        attrName = parser.getAttributeValue(null, "name")
                        if (attrName == null) {
                            Log.w(TAG, "Skip tag <array>, no name attr.")
                            continue
                        }
                        parsingArray = attrName to mutableListOf()
                        Log.d(TAG, "<array \"name\"=\"$attrName\">")
                        continue
                    }

                    // <item>
                    if (tagName == "item") {
                        attrName = parser.getAttributeValue(null, "name")
                        if (attrName == null) {
                            Log.w(TAG, "Skip tag <item>, no name attr.")
                            continue
                        }
                        if (parser.next() != XmlPullParser.TEXT) {
                            Log.w(TAG, "Skip tag <item>, next tag not text.")
                            continue
                        }
                        val text = parser.text
                        itemValues[attrName] = text
                        Log.d(TAG, "<item \"name\"=\"$attrName\">$text</item>")
                        continue
                    }

                    // Ignore
                    attrName = parser.getAttributeValue(null, "name")
                    val t: String? = if (parser.next() == XmlPullParser.TEXT) {
                        parser.text
                    } else {
                        null
                    }
                    Log.d(TAG, "Ignore tag=$tagName${if (attrName != null) ", name=$attrName" else ""}${if (t != null) ", value=$t" else ""}")
                }
                if (parsingArray != null) {
                    arrayValues[parsingArray.first] = parsingArray.second
                    Log.d(TAG, "</array>")
                }
            }
            return PowerProfile(
                application = application,
                itemValues = itemValues,
                arrayValues = arrayValues
            )
        }
    }
}