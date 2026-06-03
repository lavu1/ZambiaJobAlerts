package com.solutions.alphil.zambiajobalerts.ui.savedjobs

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solutions.alphil.zambiajobalerts.classes.Job

class SavedJobsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val savedJobsLiveData = MutableLiveData<List<Job>>(emptyList())

    init {
        loadSavedJobs()
    }

    fun getSavedJobs(): LiveData<List<Job>> = savedJobsLiveData

    fun loadSavedJobs() {
        val json = prefs.getString(KEY_SAVED_JOBS, null)
        if (json != null) {
            val type = object : TypeToken<ArrayList<Job>>() {}.type
            savedJobsLiveData.value = gson.fromJson<List<Job>>(json, type) ?: emptyList()
        } else {
            savedJobsLiveData.value = emptyList()
        }
    }

    fun saveJob(job: Job) {
        val currentJobs = savedJobsLiveData.value.orEmpty().toMutableList()
        val alreadySaved = currentJobs.any { it.getId() == job.getId() }

        if (!alreadySaved) {
            currentJobs.add(job)
            saveToPrefs(currentJobs)
            savedJobsLiveData.value = currentJobs
        }
    }

    fun removeJob(jobId: Int) {
        val currentJobs = savedJobsLiveData.value?.toMutableList() ?: return
        currentJobs.removeAll { it.getId() == jobId }
        saveToPrefs(currentJobs)
        savedJobsLiveData.value = currentJobs
    }

    fun toggleJob(job: Job): Boolean {
        return if (isJobSaved(job.getId())) {
            removeJob(job.getId())
            false
        } else {
            saveJob(job)
            true
        }
    }

    private fun saveToPrefs(jobs: List<Job>) {
        prefs.edit().putString(KEY_SAVED_JOBS, gson.toJson(jobs)).apply()
    }

    fun isJobSaved(jobId: Int): Boolean =
        savedJobsLiveData.value.orEmpty().any { it.getId() == jobId }

    companion object {
        private const val PREFS_NAME = "saved_jobs_prefs"
        private const val KEY_SAVED_JOBS = "saved_jobs_list"
    }
}
