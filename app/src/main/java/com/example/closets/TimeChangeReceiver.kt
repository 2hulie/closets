package com.example.closets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_TIME_CHANGED == intent?.action) {
            Log.d("TimeChangeReceiver", "System time changed.")
            startUpdateWork(context)
        }
    }

    private fun startUpdateWork(context: Context?) {
        val workRequest = OneTimeWorkRequestBuilder<UpdateItemsWorker>().build()
        WorkManager.getInstance(context!!).enqueue(workRequest)
    }
}