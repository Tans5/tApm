package com.tans.tpowercalculator.demo

import android.view.View
import com.tans.tpowercalculator.demo.databinding.MainActivityBinding
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import kotlinx.coroutines.CoroutineScope

class MainActivity : BaseCoroutineStateActivity<Unit>(Unit) {

    override val layoutId: Int = R.layout.main_activity

    override fun CoroutineScope.firstLaunchInitDataCoroutine() { }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = MainActivityBinding.bind(contentView)

    }

}