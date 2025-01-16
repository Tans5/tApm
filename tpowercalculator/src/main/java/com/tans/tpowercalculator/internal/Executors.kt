package com.tans.tpowercalculator.internal

import java.util.concurrent.Executors

internal object Executors {

    val bgExecutors by lazy {
        Executors.newSingleThreadExecutor { r ->
            val t = Thread(r, "tPower_BgThread")
            t
        }!!
    }

}