package com.tans.tapm

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

object AppLifecycleOwner {

    private var startedActivityCount: Int = 0
    private val apm: AtomicReference<tApm?> by lazy {
        AtomicReference(null)
    }

    var lifecycleState: LifecycleState = LifecycleState.Background
        private set

    private val observers: LinkedBlockingQueue<ObserverWrapper> by lazy {
        LinkedBlockingQueue()
    }

    internal fun init(apm: tApm) {
        this.apm.set(apm)
        apm.application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount ++
                    checkAndUpdateLifecycleState()
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount --
                    checkAndUpdateLifecycleState()
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }

    fun addLifecycleObserver(o: AppLifecycleObserver) {
        val wrapper = ObserverWrapper(o)
        val s = lifecycleState
        if (wrapper.lastDispatchState.compareAndSet(null, s)) {
            when (s) {
                LifecycleState.Foreground -> wrapper.observer.onAppForeground()
                LifecycleState.Background -> wrapper.observer.onAppBackground()
            }
        }
        observers.add(wrapper)
    }

    fun removeLifecycleObserver(o: AppLifecycleObserver) {
        observers.removeIf { it.observer === o }
    }

    private fun checkAndUpdateLifecycleState() {
        if (startedActivityCount == 1 && lifecycleState == LifecycleState.Background) {
            lifecycleState = LifecycleState.Foreground
            dispatchLifecycleState()
        }
        if (startedActivityCount == 0 && lifecycleState == LifecycleState.Foreground) {
            lifecycleState = LifecycleState.Background
            dispatchLifecycleState()
        }
    }

    private fun dispatchLifecycleState() {
        when (lifecycleState) {
            LifecycleState.Foreground -> {
                apm.get()!!.executor.executeOnBackgroundThread {
                    for (o in observers) {
                        if (o.lastDispatchState.compareAndSet(LifecycleState.Background, LifecycleState.Foreground)) {
                            o.observer.onAppForeground()
                        }
                    }
                }
            }
            LifecycleState.Background -> {
                apm.get()!!.executor.executeOnBackgroundThread {
                    for (o in observers) {
                        if (o.lastDispatchState.compareAndSet(LifecycleState.Foreground, LifecycleState.Background)) {
                            o.observer.onAppBackground()
                        }
                    }
                }
            }
        }
    }

    interface AppLifecycleObserver {

        fun onAppForeground() {

        }

        fun onAppBackground() {

        }
    }

    private data class ObserverWrapper(
        val observer: AppLifecycleObserver,
        val lastDispatchState: AtomicReference<LifecycleState?> = AtomicReference()
    )

    enum class LifecycleState {
        Foreground,
        Background
    }
}