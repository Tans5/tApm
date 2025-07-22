package com.tans.tapm.chipset

import java.io.File

fun readSystemProperty(key: String): String? {
    return try {
        val runtime = Runtime.getRuntime()
        runtime.exec("getprop $key").inputStream.use {
            it.readBytes().toString(Charsets.UTF_8).trim()
        }
    } catch (_: Throwable) {
        null
    }
}

fun readCpuInfo(): Map<String, List<String>> {
    return try {
        val lines = File("/proc/cpuinfo").inputStream().reader(Charsets.UTF_8).use {
            it.readLines()
        }
        val result = mutableMapOf<String, List<String>>()
        for (l in lines) {
            val splitIndex = l.indexOf(':')
            if (splitIndex == -1) {
                continue
            }
            val key = l.substring(0, splitIndex).trim()
            val value = l.substring(splitIndex + 1).trim()
            var values = result[key]
            if (values == null) {
                values = mutableListOf()
                result[key] = values
            }
            values as MutableList
            values.add(value)
        }
        return result
    } catch (_: Throwable) {
        emptyMap()
    }
}