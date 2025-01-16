package com.tans.tpowercalculator

interface SimpleCallback : Callback<Unit> {

    fun onSuccess()

    override fun onSuccess(t: Unit) {
        onSuccess()
    }
}