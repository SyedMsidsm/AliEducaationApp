package com.alieducational.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AliEducationalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger")
    }
}
