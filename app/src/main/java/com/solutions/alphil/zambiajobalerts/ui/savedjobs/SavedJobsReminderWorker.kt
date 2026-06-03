package com.solutions.alphil.zambiajobalerts.ui.savedjobs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.solutions.alphil.zambiajobalerts.MainActivity
import com.solutions.alphil.zambiajobalerts.R

class SavedJobsReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJobsJson = prefs.getString(KEY_SAVED_JOBS, null)

        if (savedJobsJson != null && savedJobsJson != "[]") {
            showNotification(context)
        }

        return Result.success()
    }

    private fun showNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Saved Jobs Reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders for your saved jobs"
            }
            notificationManager?.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "saved_jobs")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zambia Job Alerts")
            .setContentText("You have saved jobs! Look at them and apply now.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(1001, notification)
    }

    companion object {
        private const val PREFS_NAME = "saved_jobs_prefs"
        private const val KEY_SAVED_JOBS = "saved_jobs_list"
        private const val CHANNEL_ID = "saved_jobs_reminder_channel"
    }
}
