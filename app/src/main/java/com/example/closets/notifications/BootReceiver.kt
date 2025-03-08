package com.example.closets.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if notifications were previously enabled
            val prefs = context.getSharedPreferences("ClosetsPrefs", Context.MODE_PRIVATE)
            val isNotifEnabled = prefs.getBoolean("notification_state", false)

            if (isNotifEnabled) {
                NotificationReceiver.scheduleExactDailyNotification(context)
            }
        }
    }
}