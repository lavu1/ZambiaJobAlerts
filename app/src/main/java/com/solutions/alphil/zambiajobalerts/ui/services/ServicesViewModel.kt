package com.solutions.alphil.zambiajobalerts.ui.services

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ServicesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, 0)
    private val adsWatched = MutableLiveData(prefs.getInt(KEY_ADS_WATCHED, 0))
    private val jobsViewed = MutableLiveData(prefs.getInt(KEY_JOBS_VIEWED, 0))

    fun getAdsWatched(): LiveData<Int> = adsWatched

    fun getJobsViewed(): LiveData<Int> = jobsViewed

    fun addAdWatched() {
        val newCount = (adsWatched.value ?: 0) + 1
        adsWatched.value = newCount
        prefs.edit().putInt(KEY_ADS_WATCHED, newCount).apply()
    }

    fun addJobViewed() {
        val newCount = (jobsViewed.value ?: 0) + 1
        jobsViewed.value = newCount
        prefs.edit().putInt(KEY_JOBS_VIEWED, newCount).apply()
    }

    fun resetAds() {
        adsWatched.value = 0
        prefs.edit().putInt(KEY_ADS_WATCHED, 0).apply()
    }

    fun deductAds(amount: Int) {
        val currentAds = adsWatched.value ?: 0
        if (currentAds >= amount) {
            val newCount = currentAds - amount
            adsWatched.value = newCount
            prefs.edit().putInt(KEY_ADS_WATCHED, newCount).apply()
        }
    }

    fun getCurrentAdCount(): Int = adsWatched.value ?: 0

    companion object {
        private const val PREFS_NAME = "ad_prefs"
        private const val KEY_ADS_WATCHED = "ads_watched"
        private const val KEY_JOBS_VIEWED = "jobs_viewed"
    }
}
