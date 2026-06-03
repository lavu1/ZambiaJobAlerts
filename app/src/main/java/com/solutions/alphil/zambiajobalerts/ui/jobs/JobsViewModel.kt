package com.solutions.alphil.zambiajobalerts.ui.jobs

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.solutions.alphil.zambiajobalerts.classes.Job
import com.solutions.alphil.zambiajobalerts.classes.JobEntity
import com.solutions.alphil.zambiajobalerts.classes.JobRepository

class JobsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JobRepository(application)
    private val searchTerm = MutableLiveData("")
    private val loadingLiveData = MutableLiveData(false)
    private val errorLiveData = MutableLiveData<String?>()
    private val hasMoreData = MutableLiveData(true)
    private val jobDetailsLiveData = MutableLiveData<Job>()
    private var currentPage = 0

    @Volatile
    private var requestInFlight = false

    private val jobsLiveData: LiveData<List<Job>>

    init {
        val entitiesLiveData: LiveData<List<JobEntity>> = searchTerm.switchMap { query ->
            if (query.isNullOrEmpty()) {
                repository.allJobs
            } else {
                repository.searchJobs(query)
            }
        }

        jobsLiveData = entitiesLiveData.map { entities ->
            entities.map(Job::fromEntity)
        }

        refreshJobs()
    }

    fun getJobs(): LiveData<List<Job>> = jobsLiveData

    fun getLoading(): LiveData<Boolean> = loadingLiveData

    fun getError(): LiveData<String?> = errorLiveData

    fun getHasMoreData(): LiveData<Boolean> = hasMoreData

    fun getJobDetails(): LiveData<Job> = jobDetailsLiveData

    fun refreshJobs() {
        loadPage(1)
    }

    fun loadJobs(forceRefresh: Boolean) {
        if (forceRefresh || currentPage == 0) {
            refreshJobs()
        }
    }

    fun loadLimitedJobs(limit: Int) {
        refreshJobs()
    }

    fun searchJobs(query: String?) {
        searchTerm.value = query.orEmpty()
    }

    fun loadMoreJobs() {
        if (requestInFlight) return
        if (hasMoreData.value == false) return

        val nextPage = if (currentPage < 1) 1 else currentPage + 1
        loadPage(nextPage)
    }

    private fun loadPage(page: Int) {
        if (requestInFlight) return

        requestInFlight = true
        loadingLiveData.value = true
        errorLiveData.value = null

        repository.loadJobsPage(
            page,
            JobRepository.DEFAULT_PAGE_SIZE,
            object : JobRepository.ResponseListener<JobRepository.JobsPageResult> {
                override fun onResponse(result: JobRepository.JobsPageResult) {
                    currentPage = result.page
                    hasMoreData.postValue(result.hasMore())
                    loadingLiveData.postValue(false)
                    requestInFlight = false
                    Log.d(TAG, "Jobs page ${result.page} loaded. hasMore=${result.hasMore()}")
                }

                override fun onError(error: String) {
                    errorLiveData.postValue(error)
                    loadingLiveData.postValue(false)
                    requestInFlight = false
                    Log.e(TAG, "Jobs page $page failed: $error")
                }
            },
        )
    }

    fun fetchJobDetails(jobId: Int) {
        loadingLiveData.value = true
        repository.fetchJobDetails(
            jobId,
            object : JobRepository.ResponseListener<JobEntity> {
                override fun onResponse(result: JobEntity) {
                    jobDetailsLiveData.postValue(Job.fromEntity(result))
                    loadingLiveData.postValue(false)
                }

                override fun onError(error: String) {
                    errorLiveData.postValue(error)
                    loadingLiveData.postValue(false)
                }
            },
        )
    }

    fun fetchJobDetailsBySlug(slug: String) {
        loadingLiveData.value = true
        repository.fetchJobDetailsBySlug(
            slug,
            object : JobRepository.ResponseListener<JobEntity> {
                override fun onResponse(result: JobEntity) {
                    jobDetailsLiveData.postValue(Job.fromEntity(result))
                    loadingLiveData.postValue(false)
                }

                override fun onError(error: String) {
                    errorLiveData.postValue(error)
                    loadingLiveData.postValue(false)
                }
            },
        )
    }

    companion object {
        private const val TAG = "JobsViewModel"
    }
}
