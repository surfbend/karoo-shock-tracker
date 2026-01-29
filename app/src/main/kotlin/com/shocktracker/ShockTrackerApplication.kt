package com.shocktracker

import android.app.Application
import timber.log.Timber

class ShockTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Timber.d("ShockTracker application started")
    }
}
