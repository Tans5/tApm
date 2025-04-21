package com.tans.tapm.autoinit

import android.app.Application
import android.content.Context
import androidx.annotation.Keep
import androidx.startup.Initializer
import com.tans.tapm.InitCallback
import com.tans.tapm.tApm

@Keep
class AutoInit : Initializer<tApm> {

    override fun create(context: Context): tApm {
        val builder =  tApm.Companion.Builder(context.applicationContext as Application)
        synchronized(AutoInit::class.java) {
            val bs = builderInterceptors
            if (bs != null) {
                for (b in bs) {
                    b(builder)
                }
            }
            builderInterceptors = null
        }
        var apm: tApm? = null
        builder.setInitCallback(object : InitCallback {
            override fun onInitFinish() {
                synchronized(AutoInit::class.java) {
                    Companion.apm = apm
                    val ls = finishListeners
                    if (ls != null) {
                        for (l in ls) {
                            l(apm!!)
                        }
                    }
                    finishListeners = null
                }
            }
        })
        apm = builder.build()
        return apm
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()

    companion object {

        private var apm: tApm? = null

        private var builderInterceptors: ArrayList<((tApm.Companion.Builder) -> Unit)>? = ArrayList()

        private var finishListeners: ArrayList<(tApm) -> Unit>? = ArrayList()

        fun addBuilderInterceptor(builderInterceptor: (builder: tApm.Companion.Builder) -> Unit) {
            synchronized(AutoInit::class.java) {
                builderInterceptors?.add(builderInterceptor)
            }
        }

        fun addInitFinishListener(l: (apm: tApm) -> Unit) {
            synchronized(AutoInit::class.java) {
                if (finishListeners == null) {
                    l(apm!!)
                } else {
                    finishListeners?.add(l)
                }
            }
        }

        fun getApm(): tApm? = apm
    }
}