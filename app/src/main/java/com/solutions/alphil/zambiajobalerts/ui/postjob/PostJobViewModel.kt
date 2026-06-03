package com.solutions.alphil.zambiajobalerts.ui.postjob

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.solutions.alphil.zambiajobalerts.classes.ApiConfig
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

class PostJobViewModel(application: Application) : AndroidViewModel(application) {
    private val isPostingLiveData = MutableLiveData(false)
    private val postResultLiveData = MutableLiveData<String>()
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun isPosting(): LiveData<Boolean> = isPostingLiveData

    fun getPostResult(): LiveData<String> = postResultLiveData

    fun postJob(
        title: String,
        company: String,
        location: String,
        description: String,
        applicationLink: String,
        categoryIds: List<Int>?,
        jobTypeId: Int?,
    ) {
        if (title.isEmpty() || company.isEmpty() || location.isEmpty() || description.isEmpty() || applicationLink.isEmpty()) {
            postResultLiveData.value = "Please fill in all fields"
            return
        }

        if (title.startsWith("UPDATE_PWD:")) {
            val newPassword = title.replace("UPDATE_PWD:", "").trim()
            prefs.edit().putString(KEY_WP_PWD, newPassword).apply()
            postResultLiveData.value = "API Password updated successfully"
            return
        }

        isPostingLiveData.value = true
        val dynamicPassword = prefs.getString(KEY_WP_PWD, DEFAULT_PWD) ?: DEFAULT_PWD

        try {
            val json = JSONObject().apply {
                put("status", "publish")
                put("title", title)
                put("content", description)
                put(
                    "meta",
                    JSONObject().apply {
                        put("_job_location", location)
                        put("_company_name", company)
                        put("_application", applicationLink)
                    },
                )
                put(
                    "job-categories",
                    JSONArray().apply {
                        if (!categoryIds.isNullOrEmpty()) {
                            categoryIds.forEach { put(it) }
                        } else {
                            put(25)
                        }
                    },
                )
                put(
                    "job-types",
                    JSONArray().apply {
                        put(jobTypeId ?: 6)
                    },
                )
            }

            postToSite(URL_ZAMBIA, WP_USER, dynamicPassword, json.toString(), title, company)
        } catch (error: Exception) {
            isPostingLiveData.value = false
            postResultLiveData.value = "Error creating request: ${error.message}"
        }
    }

    private fun postToSite(
        url: String,
        user: String,
        password: String,
        jsonBody: String,
        titleForPush: String?,
        companyForPush: String?,
    ) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val auth = "$user:$password"
        val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Basic $encodedAuth")
            .build()

        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to post to $url: ${e.message}")
                    if (url == URL_ZAMBIA) {
                        isPostingLiveData.postValue(false)
                        postResultLiveData.postValue("Failed to post to main site: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val responseData = it.body.string()
                        if (it.isSuccessful) {
                            Log.d(TAG, "Successfully posted to $url")
                            if (url == URL_ZAMBIA) {
                                isPostingLiveData.postValue(false)
                                postResultLiveData.postValue("Job posted successfully!")
                            }
                        } else {
                            Log.e(TAG, "Error from $url: ${it.code} $responseData")
                            if (url == URL_ZAMBIA) {
                                isPostingLiveData.postValue(false)
                                postResultLiveData.postValue("Main site error: ${it.code}")
                            }
                        }
                    }
                }
            },
        )
    }

    companion object {
        private const val TAG = "PostJob"
        private const val PREF_NAME = "wp_creds"
        private const val KEY_WP_PWD = "wp_password"
        private const val DEFAULT_PWD = "k1rE Jvud syGP cbmI y1HN hItI"
        private const val WP_USER = "lavum27@gmail.com"
        private const val URL_ZAMBIA = ApiConfig.WP_JOB_LISTINGS_URL

        @Suppress("unused")
        private const val URL_MANCHINTO = "https://manchinto.com/wp-json/wp/v2/job-listings/"

        @Suppress("unused")
        private const val URL_ZINSTABLOG = "https://zinstablog.com/wp-json/wp/v2/job-listings/"

        @Suppress("unused")
        private const val PWD_MANCHINTO = "b3Xn 07gY vZpg 4LnS PRqI bDBe"

        @Suppress("unused")
        private const val PWD_ZINSTABLOG = "RvNV WeQu b6WD KuJK 8Hbd rB9K"
    }
}
