package com.solutions.alphil.zambiajobalerts

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.messaging.FirebaseMessaging
import com.solutions.alphil.zambiajobalerts.classes.AdManager
import com.solutions.alphil.zambiajobalerts.classes.RefreshDataWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) {
            AdManager.getInstance().loadInterstitialAd(this)
        }

        appOpenManager = AppOpenManager(this)
        scheduleJobSync()

        FirebaseMessaging.getInstance().subscribeToTopic("new_jobs")
    }

    private fun scheduleJobSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequest.Builder(
            RefreshDataWorker::class.java,
            1,
            TimeUnit.HOURS,
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "JobSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest,
        )
    }

    companion object {
        private var appOpenManager: AppOpenManager? = null
    }
}
