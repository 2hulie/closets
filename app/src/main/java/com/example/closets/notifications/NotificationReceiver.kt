package com.example.closets.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.closets.R
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "onReceive triggered")
        createNotification(context)
    }

    companion object {
        private const val CHANNEL_ID = "closets_reminder"
        private const val NOTIFICATION_ID = 1
        private const val PREFS_NAME = "ClosetsNotifPrefs"
        private const val LAST_NOTIF_DATE_KEY = "last_notification_date"

        fun createNotification(context: Context) {
            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Closets Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily outfit logging reminders"
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }

            // Create the intent to navigate to CurrentItemFragment
            val intent = NavDeepLinkBuilder(context)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.currentItemFragment)
                .createTaskStackBuilder()
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            // Create the notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_closets_logo)
                .setContentTitle("Closets Reminder")
                .setContentText("What's your outfit today? Don't forget to log it!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(intent)

            // Update last notification date
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit().putLong(LAST_NOTIF_DATE_KEY, System.currentTimeMillis()).apply()

            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(NOTIFICATION_ID, builder.build())
                }
            }
        }

        fun scheduleExactDailyNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Create an intent for the BroadcastReceiver
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Get current time and last notification date
            val currentTime = Calendar.getInstance()
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastNotifDate = sharedPrefs.getLong(LAST_NOTIF_DATE_KEY, 0)

            // Check if a notification has already been sent today
            val isNotificationStillValid =
                lastNotifDate == 0L || !isSameDay(lastNotifDate, System.currentTimeMillis())

            // - If before 9 AM, schedule at 9 AM today.
            // - If between 9 AM and 9 PM, schedule at 9 PM today.
            // - If 9 PM or later, schedule at 9 AM tomorrow.
            val notificationTime = Calendar.getInstance().apply {
                when {
                    currentTime.get(Calendar.HOUR_OF_DAY) < 9 -> {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    currentTime.get(Calendar.HOUR_OF_DAY) in 9 until 21 -> {
                        set(Calendar.HOUR_OF_DAY, 21)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    else -> {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }

            // Only schedule if no notification has been sent today
            if (isNotificationStillValid) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.timeInMillis,
                    pendingIntent
                )
            }
        }

        private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
            val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
            val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        fun cancelDailyNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}