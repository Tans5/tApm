package com.tans.tapm.internal

internal sealed class ComponentProfile {

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