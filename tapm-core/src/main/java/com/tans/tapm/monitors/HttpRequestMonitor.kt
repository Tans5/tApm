package com.tans.tapm.monitors

import com.tans.tapm.tApm
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicReference

class HttpRequestMonitor : AbsMonitor<Unit>(2000L) {

    override val isSupport: Boolean
        get() = this.apm.get() != null

    override fun onInit(apm: tApm) {

    }

    override fun onStart(apm: tApm) {
        attachMonitor(this)
    }

    override fun onStop(apm: tApm) {
        detachMonitor(this)
    }

    private fun httpRequestIntercept(chain: Interceptor.Chain): Response {
        // TODO:
        return chain.proceed(chain.request())
    }

    companion object : Interceptor {

        private val monitor: AtomicReference<HttpRequestMonitor?> by lazy {
            AtomicReference(null)
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val m = monitor.get()
            return m?.httpRequestIntercept(chain) ?: chain.proceed(chain.request())
        }

        private fun attachMonitor(m: HttpRequestMonitor) {
            monitor.compareAndSet(null, m)
        }

        private fun detachMonitor(m: HttpRequestMonitor) {
            monitor.compareAndSet(m, null)
        }
    }
}