package com.tans.tpowercalculator

interface MonitorCallback<T : Any> {

    fun updateData(t: T)

}