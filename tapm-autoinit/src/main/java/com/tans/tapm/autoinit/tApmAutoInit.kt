package com.tans.tapm.autoinit

import android.app.Application
import android.content.Context
import androidx.annotation.Keep
import androidx.startup.Initializer
import com.tans.tapm.InitCallback
import com.tans.tapm.tApm

@Suppress("ClassName")
@Keep
class tApmAutoInit : Initializer<tApm> {

    override fun create(context: Context): tApm {
        val builder =  tApm.Companion.Builder(context.applicationContext as Application)
        synchronized(tApmAutoInit::class.java) {
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
                synchronized(tApmAutoInit::class.java) {
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

        private var builderInterceptors: ArrayList<((tApm.Companion.Builder) -> Unit)>? = ArrayList()

        private var finishListeners: ArrayList<(tApm) -> Unit>? = ArrayList()

        fun addBuilderInterceptor(builderInterceptor: (builder: tApm.Companion.Builder) -> Unit) {
            synchronized(tApmAutoInit::class.java) {
                builderInterceptors?.add(builderInterceptor)
            }
        }

        fun addInitFinishListener(l: (apm: tApm) -> Unit) {
            synchronized(tApmAutoInit::class.java) {
                if (finishListeners == null) {
                    l(tApm.Companion.getApm()!!)
                } else {
                    finishListeners?.add(l)
                }
            }
        }
    }
}