package com.solutions.alphil.zambiajobalerts.classes

import android.app.Application
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solutions.alphil.zambiajobalerts.JobLaunchParser

class JobRepository(application: Application) {
    private val jobDao: JobDao
    val allJobs: LiveData<List<JobEntity>>
    private val queue: RequestQueue
    private val gson = Gson()
    private val isRefreshing = MutableLiveData(false)

    init {
        val db = AppDatabase.getDatabase(application)
        jobDao = db.jobDao()
        allJobs = jobDao.getAllJobs()
        queue = Volley.newRequestQueue(application)
    }

    fun searchJobs(query: String): LiveData<List<JobEntity>> = jobDao.searchJobs(query)

    fun getIsRefreshing(): LiveData<Boolean> = isRefreshing

    fun refreshJobs() {
        loadJobsPage(1, DEFAULT_PAGE_SIZE, null)
    }

    fun loadJobsPage(page: Int, perPage: Int, listener: ResponseListener<JobsPageResult>?) {
        setRefreshing(true)

        val safePage = page.coerceAtLeast(1)
        val safePerPage = perPage.coerceAtLeast(1)
        var totalPages = -1
        val url = ApiConfig.WP_JOB_LISTINGS_URL +
            "?per_page=$safePerPage" +
            "&page=$safePage" +
            "&_embed=1"

        val request = object : StringRequest(
            Request.Method.GET,
            url,
            Response.Listener { response ->
                try {
                    if (looksLikeHtmlChallenge(response)) {
                        throw IllegalStateException(PROTECTED_RESPONSE_LOG_MESSAGE)
                    }
                    val listType = object : TypeToken<List<Job>>() {}.type
                    val jobs = gson.fromJson<List<Job>>(response, listType)
                    val entities = jobs.map(::mapToEntity)
                    AppDatabase.databaseWriteExecutor.execute {
                        if (safePage == 1) {
                            jobDao.deleteAllJobs()
                        }
                        jobDao.insertJobs(entities)
                        isRefreshing.postValue(false)
                        listener?.let {
                            val hasMore = if (totalPages > 0) {
                                safePage < totalPages
                            } else {
                                entities.size >= safePerPage
                            }
                            Log.d(TAG, "Loaded jobs page $safePage with ${entities.size} jobs. hasMore=$hasMore")
                            it.onResponse(JobsPageResult(safePage, entities, hasMore))
                        }
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Error parsing jobs: ${error.message}")
                    setRefreshing(false)
                    listener?.onError(JOBS_CONNECTION_MESSAGE)
                }
            },
            Response.ErrorListener { error ->
                val message = jobsErrorMessage()
                Log.e(TAG, "Error fetching jobs: ${error.networkResponse?.statusCode ?: "no-status"} ${error.message}")
                setRefreshing(false)
                listener?.onError(message)
            },
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = requestHeaders()

            override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                totalPages = parseIntHeader(response.headers, TOTAL_PAGES_HEADER, -1)
                return super.parseNetworkResponse(response)
            }
        }

        queue.add(request)
    }

    private fun setRefreshing(refreshing: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            isRefreshing.value = refreshing
        } else {
            isRefreshing.postValue(refreshing)
        }
    }

    fun fetchJobDetails(jobId: Int, listener: ResponseListener<JobEntity>) {
        AppDatabase.databaseWriteExecutor.execute {
            val localJob = jobDao.getJobById(jobId)
            if (localJob != null) {
                listener.onResponse(localJob)
            } else {
                val url = "${ApiConfig.WP_JOB_LISTINGS_URL}/$jobId?_embed"
                fetchJobFromUrl(url, listener)
            }
        }
    }

    fun fetchJobDetailsBySlug(slug: String, listener: ResponseListener<JobEntity>) {
        val url = JobLaunchParser.buildJobDetailsBySlugUrl(ApiConfig.WP_JOB_LISTINGS_URL, slug)
        fetchJobFromUrl(
            url,
            object : ResponseListener<List<JobEntity>> {
                override fun onResponse(result: List<JobEntity>) {
                    if (result.isNotEmpty()) {
                        listener.onResponse(result[0])
                    } else {
                        listener.onError("Job not found")
                    }
                }

                override fun onError(error: String) {
                    listener.onError(error)
                }
            },
            object : TypeToken<List<Job>>() {}.type,
        )
    }

    private fun fetchJobFromUrl(url: String, listener: ResponseListener<JobEntity>) {
        val request = object : StringRequest(
            Request.Method.GET,
            url,
            { response ->
                try {
                    if (looksLikeHtmlChallenge(response)) {
                        throw IllegalStateException(PROTECTED_RESPONSE_LOG_MESSAGE)
                    }
                    val job = gson.fromJson(response, Job::class.java)
                    val entity = mapToEntity(job)
                    AppDatabase.databaseWriteExecutor.execute {
                        jobDao.insertJobs(listOf(entity))
                    }
                    listener.onResponse(entity)
                } catch (error: Exception) {
                    Log.e(TAG, "Error parsing job details: ${error.message}")
                    listener.onError(JOB_DETAILS_CONNECTION_MESSAGE)
                }
            },
            { error ->
                Log.e(TAG, "Error fetching job details: ${error.networkResponse?.statusCode ?: "no-status"} ${error.message}")
                listener.onError(jobDetailsErrorMessage())
            },
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = requestHeaders()
        }
        queue.add(request)
    }

    private fun fetchJobFromUrl(
        url: String,
        listener: ResponseListener<List<JobEntity>>,
        type: java.lang.reflect.Type,
    ) {
        val request = object : StringRequest(
            Request.Method.GET,
            url,
            { response ->
                try {
                    if (looksLikeHtmlChallenge(response)) {
                        throw IllegalStateException(PROTECTED_RESPONSE_LOG_MESSAGE)
                    }
                    val jobs = gson.fromJson<List<Job>>(response, type)
                    val entities = jobs.map(::mapToEntity)
                    AppDatabase.databaseWriteExecutor.execute {
                        jobDao.insertJobs(entities)
                    }
                    listener.onResponse(entities)
                } catch (error: Exception) {
                    Log.e(TAG, "Error parsing job details by slug: ${error.message}")
                    listener.onError(JOB_DETAILS_CONNECTION_MESSAGE)
                }
            },
            { error ->
                Log.e(TAG, "Error fetching job details by slug: ${error.networkResponse?.statusCode ?: "no-status"} ${error.message}")
                listener.onError(jobDetailsErrorMessage())
            },
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = requestHeaders()
        }
        queue.add(request)
    }

    private fun mapToEntity(job: Job): JobEntity =
        JobEntity(
            job.getId(),
            job.getTitle(),
            job.getExcerpt(),
            job.getContent(),
            job.getDate(),
            job.getLink(),
            job.getFeaturedImage(),
            job.getCompany(),
            job.getLocation(),
            job.getJobType(),
            job.getApplication(),
        )

    private fun parseIntHeader(headers: Map<String, String>?, headerName: String, defaultValue: Int): Int {
        headers ?: return defaultValue

        headers.forEach { (key, value) ->
            if (key.equals(headerName, ignoreCase = true)) {
                return value.toIntOrNull() ?: defaultValue
            }
        }

        return defaultValue
    }

    private fun requestHeaders(): MutableMap<String, String> =
        mutableMapOf(
            "Accept" to "application/json",
            "User-Agent" to USER_AGENT,
        )

    private fun jobsErrorMessage(): String =
        JOBS_CONNECTION_MESSAGE

    private fun jobDetailsErrorMessage(): String =
        JOB_DETAILS_CONNECTION_MESSAGE

    private fun looksLikeHtmlChallenge(response: String): Boolean {
        val trimmed = response.trimStart()
        return trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.contains("Checking your browser before accessing", ignoreCase = true) ||
            trimmed.contains("/hcdn-cgi/jschallenge", ignoreCase = true)
    }

    class JobsPageResult(
        val page: Int,
        val jobs: List<JobEntity>,
        private val hasMore: Boolean,
    ) {
        fun hasMore(): Boolean = hasMore
    }

    interface ResponseListener<T> {
        fun onResponse(result: T)
        fun onError(error: String)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
        private const val TOTAL_PAGES_HEADER = "X-WP-TotalPages"
        private const val TAG = "JobRepository"
        private const val USER_AGENT = "ZambiaJobAlerts/25 Android"
        private const val PROTECTED_RESPONSE_LOG_MESSAGE =
            "Protected or non-JSON response returned for job listings"
        private const val JOBS_CONNECTION_MESSAGE =
            "Unable to load jobs right now. Please tap Refresh, check your internet connection, or switch networks and try again."
        private const val JOB_DETAILS_CONNECTION_MESSAGE =
            "Unable to load this job right now. Please check your connection, switch networks, or try again."
    }
}
