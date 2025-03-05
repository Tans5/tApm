package com.tans.tapm

interface MonitorCallback<T : Any> {

    fun updateData(t: T)

}