package com.five9th.motionlogger.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.five9th.motionlogger.domain.repos.ModelInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityTracker @Inject constructor(
    private val inference: ModelInference
) : Application.ActivityLifecycleCallbacks {

    private var startedActivities = 0  // maybe count created activities instead (?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var closeJob: Job? = null

    override fun onActivityStarted(activity: Activity) {
        startedActivities++

        // UI came back → cancel pending close
        closeJob?.cancel()
        closeJob = null
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--

        if (startedActivities == 0) {
            // Defer closing to survive configuration changes
            closeJob = scope.launch {
                delay(1000) // some time for an Activity to be recreated (if it's going to)
                if (startedActivities == 0) {
                    inference.close()
                }
            }
        }
    }

    // other callbacks ignored
    override fun onActivityCreated(a: Activity, b: Bundle?) {}
    override fun onActivityResumed(a: Activity) {}
    override fun onActivityPaused(a: Activity) {}
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
    override fun onActivityDestroyed(a: Activity) {}
}