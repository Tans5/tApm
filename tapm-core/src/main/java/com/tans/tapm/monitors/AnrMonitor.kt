package com.tans.tapm.monitors

import androidx.annotation.Keep
import com.tans.tapm.tApm

@Keep
class AnrMonitor : AbsMonitor<Unit>(Long.MAX_VALUE) {
    override val isSupport: Boolean
        get() = this.apm.get() != null

    override fun onInit(apm: tApm) {  }

    override fun onStart(apm: tApm) {
        // TODO:
    }

    override fun onStop(apm: tApm) {
        // TODO:
    }

    private external fun registerAnrMonitor(): Long

    private external fun unregisterAnrMonitor(nativePtr: Long)
}