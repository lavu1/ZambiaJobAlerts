package com.solutions.alphil.zambiajobalerts.classes

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class RefreshDataWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        Log.d(TAG, "Starting background sync...")

        val app = applicationContext as? Application
        if (app != null) {
            JobRepository(app).refreshJobs()
        } else {
            Log.w(TAG, "Application context was not an Application; skipping job refresh")
        }

        try {
            AdManager.getInstance().loadInterstitialAd(applicationContext)
        } catch (error: Exception) {
            Log.e(TAG, "Error pre-caching ads: ${error.message}")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "RefreshDataWorker"
    }
}
