package com.tans.tapm

interface InitCallback {

    fun onSupportMonitor(monitor: Monitor<*>) {

    }

    fun onUnsupportMonitor(monitor: Monitor<*>) {

    }

    fun onInitFinish() {

    }
}