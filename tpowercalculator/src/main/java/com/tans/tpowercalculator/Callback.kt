package com.tans.tpowercalculator

interface Callback<T : Any> {

    fun onSuccess(t: T)

    fun onFail(msg: String, throwable: Throwable? = null)
}