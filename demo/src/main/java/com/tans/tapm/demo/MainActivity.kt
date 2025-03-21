package com.tans.tapm.demo

import android.view.View
import com.tans.tapm.demo.databinding.MainActivityBinding
import com.tans.tapm.monitors.JavaCrashMonitor
import com.tans.tapm.monitors.NativeCrashMonitor
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.ContentViewFitSystemWindow
import com.tans.tuiutils.systembar.annotation.SystemBarStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope

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
    }

}