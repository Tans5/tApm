package com.tans.tapm.demo

import android.view.View
import com.tans.tapm.demo.databinding.MainActivityBinding
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.monitors.NativeCrashMonitor
import com.tans.tapm.tApm
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.ContentViewFitSystemWindow
import com.tans.tuiutils.systembar.annotation.SystemBarStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import java.io.IOException

@SystemBarStyle
@ContentViewFitSystemWindow
class MainActivity : BaseCoroutineStateActivity<Unit>(Unit) {

    override val layoutId: Int = R.layout.main_activity

    override fun CoroutineScope.firstLaunchInitDataCoroutine() { }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = MainActivityBinding.bind(contentView)
        viewBinding.testJavaCrashBt.clicks(this) {
            tApm.Companion.getApm()?.getMonitor(JavaCrashMonitor::class.java)?.testJavaCrash()
        }
        viewBinding.blockMainThreadBt.clicks(this) {
            Thread.sleep(8_000)
        }
        viewBinding.testNativeCrashBt.clicks(this) {
            tApm.Companion.getApm()?.getMonitor(NativeCrashMonitor::class.java)?.testNativeCrash()
        }
        viewBinding.testNativeCrashNewThreadBt.clicks(this) {
            Thread {
                tApm.Companion.getApm()?.getMonitor(NativeCrashMonitor::class.java)?.testNativeCrash()
            }.start()
        }
        viewBinding.httpGetBt.clicks(this, clickWorkOn = Dispatchers.IO) {
            try {
                val client = (application as App).okHttpClient
                val request = Request.Builder()
                    .get()
                    .url("https://api.github.com/repos/tans5/tapm?name=Tans5")
                    .build()
                val call = client.newCall(request)
                val response = call.execute()
                response.use {
                    val responseString = response.body?.string()
                    AppLog.d(TAG, "Http Get resp: $responseString")
                }
            } catch (e: Throwable) {
                AppLog.e(TAG, "Http Get fail: ${e.message}", e)
            }

        }
        viewBinding.httpPostBt.clicks(this, clickWorkOn = Dispatchers.IO) {
            try {
                val client = (application as App).okHttpClient
                val buffer = Buffer()
                buffer.write("{ \"name\": \"Tans5\" }".toByteArray())
                val body = buffer.snapshot().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .addHeader("Content-Encoding", "gzip")
                    .post(gzip(body))
                    .url("https://api.github.com/repos/tans5/tapm?name=Tans5")
                    .build()
                val call = client.newCall(request)
                val response = call.execute()
                response.use {
                    val responseString = response.body?.string()
                    AppLog.d(TAG, "Http Post resp: $responseString")
                }
            } catch (e: Throwable) {
                AppLog.e(TAG, "Http Post fail: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private fun gzip(body: RequestBody): RequestBody {
            return object : RequestBody() {
                override fun contentType(): MediaType? {
                    return body.contentType()
                }

                override fun contentLength(): Long {
                    return -1 // We don't know the compressed length in advance!
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    val gzipSink: BufferedSink = GzipSink(sink).buffer()
                    body.writeTo(gzipSink)
                    gzipSink.close()
                }
            }
        }
    }
}