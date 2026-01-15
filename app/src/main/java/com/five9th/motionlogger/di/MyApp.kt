package com.five9th.motionlogger.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject lateinit var tracker: ActivityTracker

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(tracker)
    }
}