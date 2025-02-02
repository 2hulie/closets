import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.closets.R

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        createNotification()
        return Result.success()
    }

    private fun createNotification() {
        val channelId = "closets_reminder"
        val notificationId = 1

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
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
            .setGraph(R.navigation.mobile_navigation) // navigation graph resource
            .setDestination(R.id.currentItemFragment) // fragment ID
            .createTaskStackBuilder()
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Create the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.icon_closets_logo) // icon for notifications
            .setContentTitle("Closets Reminder")
            .setContentText("What's your outfit today? Don't forget to log it!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(intent) // Set the PendingIntent

        // Show the notification
        NotificationManagerCompat.from(context).apply {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, builder.build())
            }
        }
    }
}