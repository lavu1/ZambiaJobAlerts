package com.solutions.alphil.zambiajobalerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.solutions.alphil.zambiajobalerts.shared.SharedLaunchRouter
import com.solutions.alphil.zambiajobalerts.shared.SharedNotificationParser
import com.solutions.alphil.zambiajobalerts.shared.SharedNotificationPayload

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body
        val payload = SharedNotificationParser.fromMap(
            data,
            notificationTitle,
            notificationBody,
        )

        payload.newWpPassword?.let { newPassword ->
            updateWpPassword(newPassword)
            if (SharedNotificationParser.shouldSuppressVisibleNotification(payload)) {
                return
            }
        }

        if (!areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled by user, skipping...")
            return
        }

        if (data.isNotEmpty() || remoteMessage.notification != null) {
            Log.d(TAG, "Message data payload: $data")
            showNotification(payload)
        }
    }

    private fun updateWpPassword(newPwd: String?) {
        if (!newPwd.isNullOrEmpty()) {
            val wpPrefs = getSharedPreferences(WP_PREF_NAME, Context.MODE_PRIVATE)
            wpPrefs.edit().putString(KEY_WP_PWD, newPwd).apply()
            Log.d(TAG, "WordPress password updated remotely via FCM")
        }
    }

    private fun showNotification(payload: SharedNotificationPayload) {
        try {
            createNotificationChannel()

            var title = payload.title
            var body = payload.message
            val jobId = payload.jobId
            val jobSlug = payload.jobSlug
            val type = payload.type
            val company = payload.company
            val location = payload.location
            val link = payload.link

            if (title.isNullOrBlank()) {
                title = "Zambia Job Alerts"
            }
            if (body.isNullOrBlank()) {
                body = "Tap to open Zambia Job Alerts"
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            if (jobId != null) {
                val numericJobId = jobId.toIntOrNull()
                if (numericJobId != null) {
                    intent.putExtra("job_id", numericJobId)
                } else {
                    intent.putExtra("job_id", jobId)
                    Log.d(TAG, "Using non-numeric job identifier: $jobId")
                }
            }
            if (!jobSlug.isNullOrEmpty()) {
                intent.putExtra("job_slug", jobSlug)
            }
            if (!link.isNullOrEmpty()) {
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(normalizeLaunchUrl(link))
                intent.putExtra("link", link)
            }
            if (jobId.isNullOrEmpty() && jobSlug.isNullOrEmpty() && link.isNullOrEmpty()) {
                intent.putExtra("open_home", true)
            }
            if (type != null) {
                val numericType = type.toIntOrNull()
                if (numericType != null) {
                    intent.putExtra("type", numericType)
                } else {
                    Log.e(TAG, "Invalid type: $type")
                }
            }

            val requestCode = buildRequestCode(jobId, jobSlug, link)
            val pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            if (body.length > 50 || company != null || location != null) {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                val bigText = StringBuilder(body)

                if (company != null) {
                    bigText.append("\n\nCompany: ").append(company)
                }
                if (location != null) {
                    bigText.append("\nLocation: ").append(location)
                }

                bigTextStyle.bigText(bigText.toString())
                builder.setStyle(bigTextStyle)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager != null) {
                var notificationId = requestCode
                if (jobId != null) {
                    val numericJobId = jobId.toIntOrNull()
                    if (numericJobId != null) {
                        notificationId = numericJobId
                    } else {
                        Log.e(TAG, "Invalid notification ID: $jobId")
                    }
                }
                manager.notify(notificationId, builder.build())
            }
        } catch (error: Exception) {
            Log.e(TAG, "Error showing notification", error)
        }
    }

    private fun buildRequestCode(jobId: String?, jobSlug: String?, link: String?): Int {
        val stableKey = firstNonEmpty(jobId, jobSlug, link)
        return stableKey?.hashCode() ?: System.currentTimeMillis().toInt()
    }

    private fun firstNonEmpty(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private fun normalizeLaunchUrl(value: String): String =
        SharedLaunchRouter.normalizeLaunchUrl(value)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for new job postings and updates"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userEnabled = prefs.getBoolean(NOTIFICATIONS_ENABLED, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager != null) {
                return userEnabled && manager.areNotificationsEnabled()
            }
        }

        return userEnabled
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")
        subscribeToJobTopics()
    }

    private fun subscribeToJobTopics() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all_jobs")
            FirebaseMessaging.getInstance().subscribeToTopic("new_jobs")
        } catch (error: Exception) {
            Log.e(TAG, "Error subscribing to topics", error)
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "job_alerts_channel"
        private const val CHANNEL_NAME = "Job Alerts"
        private const val PREFS_NAME = "app_prefs"
        private const val NOTIFICATIONS_ENABLED = "notifications_enabled"

        private const val WP_PREF_NAME = "wp_creds"
        private const val KEY_WP_PWD = "wp_password"

        @JvmStatic
        fun setNotificationsEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply()
        }

        @JvmStatic
        fun isNotificationsEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(NOTIFICATIONS_ENABLED, true)
        }
    }
}
