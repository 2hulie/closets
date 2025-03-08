package com.example.closets

import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.perf.FirebasePerformance

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase for the entire app
        FirebaseApp.initializeApp(this)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = !BuildConfig.DEBUG
    }
}