package com.tans.tapm.demo

import android.view.View
import com.tans.tapm.demo.databinding.MainActivityBinding
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
            error("Test Crash.")
        }
        viewBinding.blockMainThreadBt.clicks(this) {
            Thread.sleep(8_000)
        }
    }

}