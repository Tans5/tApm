package com.tans.tapm.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle

internal object AppLifecycleOwner {

    private var startedActivityCount: Int = 0

    var lifecycleState: LifecycleState = LifecycleState.Background
        private set

    private val observers: ArrayList<AppLifecycleObserver> by lazy {
        ArrayList()
    }

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(
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
        synchronized(this) {
            if (!observers.contains(o)) {
                observers.add(o)
                when (lifecycleState) {
                    LifecycleState.Foreground -> o.onAppForeground()
                    LifecycleState.Background -> o.onAppBackground()
                }
            }
        }
    }

    fun removeLifecycleObserver(o: AppLifecycleObserver) {
        synchronized(this) {
            observers.remove(o)
        }
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
                Executors.bgExecutor.execute {
                    synchronized(this@AppLifecycleOwner) {
                        for (o in observers) {
                            o.onAppForeground()
                        }
                    }
                }
            }
            LifecycleState.Background -> {
                Executors.bgExecutor.execute {
                    synchronized(this@AppLifecycleOwner) {
                        for (o in observers) {
                            o.onAppBackground()
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

    enum class LifecycleState {
        Foreground,
        Background
    }
}