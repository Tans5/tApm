package com.tans.tpowercalculator.internal

sealed class ComponentProfile {

    data class CpuProfile(
        val suspendMa: Float,
        val idleMa: Float,
        val activeMa: Float,
        val cluster: List<Cluster>,
        val coreCount: Int
    ) : ComponentProfile() {

        companion object {

            data class Cluster(
                val coreCount: Int,
                val onMa: Float,
                val frequencies: List<Frequency>,
                val coreIndexRange: IntRange
            )

            data class Frequency(
                val speedHz: Int,
                val onMa: Float
            )

            class Builder {
                var suspendMa: Float = 0.0f
                var idleMa: Float = 0.0f
                var activeMa: Float = 0.0f
                val coreCount: MutableList<Int> = mutableListOf()
                val clusterOnPower: MutableMap<Int, Float> = mutableMapOf()
                val coreSpeeds: MutableMap<Int, List<Int>> = mutableMapOf()
                val corePower: MutableMap<Int, List<Float>> = mutableMapOf()

                fun build(): CpuProfile {
                    val clusters = mutableListOf<Cluster>()
                    var clusterCoreIndexStart = 0
                    for ((clusterIndex, clusterCoreCount) in coreCount.withIndex()) {
                        val clusterPower = clusterOnPower[clusterIndex] ?: 0.0f
                        val speeds = coreSpeeds[clusterIndex]
                        val power = corePower[clusterIndex]
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
                                    speedHz = s,
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
                        coreCount = clusters.sumOf { it.coreCount }
                    )

                }
            }
        }
    }

    data class ScreenProfile(
        val ambientMa: Float,
        val onMa: Float,
        val fullMa: Float
    ) : ComponentProfile() {

        companion object {
            class Builder {
                var ambientMa: Float = 0.0f
                var onMa: Float = 0.0f
                var fullMa: Float = 0.0f

                fun build(): ScreenProfile {
                    return ScreenProfile(
                        ambientMa = ambientMa,
                        onMa = onMa,
                        fullMa = fullMa
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
        val idleMa: Float,
        val rxMa: Float,
        val txMa: Float
    ) : ComponentProfile() {

        companion object {

            class Builder {
                var idleMa: Float = 0.0f
                var rxMa: Float = 0.0f
                var txMa: Float = 0.0f

                fun build(): BluetoothProfile {
                    return BluetoothProfile(
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
        val sleepMa: Float,
        val idleMa: Float,
        val rxMa: Float,
        val scanningMa: Float,
        val txMa: List<Float>
    ) : ComponentProfile() {

        companion object {

            class Builder {
                var sleepMa: Float = 0.0f
                var idleMa: Float = 0.0f
                var rxMa: Float = 0.0f
                var scanningMa: Float = 0.0f
                val txMa: MutableList<Float> = mutableListOf()

                fun build(): ModemProfile {
                    return ModemProfile(
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
        val idleMa: Float,
        val rxMa: Float,
        val txMa: Float
    ) : ComponentProfile() {

        companion object {

            class Builder {
                var idleMa: Float = 0.0f
                var rxMa: Float = 0.0f
                var txMa: Float = 0.0f

                fun build(): WifiProfile {
                    return WifiProfile(
                        idleMa = idleMa,
                        rxMa = rxMa,
                        txMa = txMa
                    )
                }
            }
        }
    }

}