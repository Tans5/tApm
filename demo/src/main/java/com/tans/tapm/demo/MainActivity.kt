package com.tans.tapm.demo

import android.view.View
import com.tans.tapm.breakpad.BreakpadNativeCrashMonitor
import com.tans.tapm.demo.databinding.MainActivityBinding
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.monitors.NativeCrashMonitor
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.ContentViewFitSystemWindow
import com.tans.tuiutils.systembar.annotation.SystemBarStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@SystemBarStyle
@ContentViewFitSystemWindow
class MainActivity : BaseCoroutineStateActivity<Unit>(Unit) {

    override val layoutId: Int = R.layout.main_activity

    override fun CoroutineScope.firstLaunchInitDataCoroutine() { }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = MainActivityBinding.bind(contentView)
        viewBinding.testJavaCrashBt.clicks(this) {
            (application as App).apm.getMonitor(JavaCrashMonitor::class.java)?.testJavaCrash()
        }
        viewBinding.blockMainThreadBt.clicks(this) {
            Thread.sleep(8_000)
        }
        viewBinding.testNativeCrashBt.clicks(this) {
            (application as App).apm.getMonitor(NativeCrashMonitor::class.java)?.testNativeCrash()
        }
        viewBinding.testNativeCrashNewThreadBt.clicks(this) {
            Thread {
                (application as App).apm.getMonitor(NativeCrashMonitor::class.java)?.testNativeCrash()
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
                val request = Request.Builder()
                    .post("\"{ \"name\": \"{\"message\":\"Problems parsing JSON\",\"documentation_url\":\"https://docs.github.com/rest/repos/repos#update-a-repository\",\"status\":\"400\"}\" }\"".toRequestBody("application/json".toMediaTypeOrNull()))
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
    }
}