package com.solutions.alphil.zambiajobalerts.ui.rewards

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.solutions.alphil.zambiajobalerts.classes.ApiConfig
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

abstract class RewardRequestFragment : Fragment() {
    protected abstract val screenTitle: String
    protected abstract val description: String
    protected abstract val fields: List<RequestField>
    protected abstract val successToast: String
    protected abstract fun buildFormBody(values: Map<String, String>): FormBody

    private val values = mutableStateMapOf<String, String>()
    private val client = OkHttpClient()
    private var loadingDialog: AlertDialog? = null
    private var rewardedAd: RewardedAd? = null
    private var pendingValues: Map<String, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        fields.forEach { field -> values.putIfAbsent(field.key, "") }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    RewardRequestScreen(
                        title = screenTitle,
                        description = description,
                        fields = fields,
                        values = values,
                        onSubmit = { submitForm() },
                    )
                }
            }
        }
    }

    protected fun backendForm(
        type: String,
        days: String,
        name: String = "",
        email: String = "",
        phone: String = "",
        education: String = "",
        work: String = "",
        skills: String = "",
        notes: String = "",
    ): FormBody =
        FormBody.Builder()
            .add("type", type)
            .add("days", days)
            .add("name", name)
            .add("email", email)
            .add("phone", phone)
            .add("education_background", education)
            .add("work_experience", work)
            .add("skills", skills)
            .add("additional_notes", notes)
            .add("status", "Pending")
            .build()

    private fun submitForm() {
        val snapshot = values.toMap()
        val missing = fields.firstOrNull { it.required && snapshot[it.key].isNullOrBlank() }
        if (missing != null) {
            Toast.makeText(context, "Enter ${missing.label}", Toast.LENGTH_SHORT).show()
            return
        }

        pendingValues = snapshot
        showLoadingDialog("Loading ad, please wait...")
        Toast.makeText(context, successToast, Toast.LENGTH_LONG).show()
        loadRewardedAd()
    }

    private fun isNetworkAvailable(): Boolean {
        val context = context ?: return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun sendInfoToServer(values: Map<String, String>) {
        val context = context ?: return
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "You are not connected to the network", Toast.LENGTH_SHORT).show()
            return
        }

        showLoadingDialog("Submitting information please wait...")

        val request = Request.Builder()
            .url(ApiConfig.LEGACY_SERVICES_URL)
            .post(buildFormBody(values))
            .build()

        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requireActivity().runOnUiThread {
                        dismissLoadingDialog()
                        Toast.makeText(context, "Failed to submit request. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val responseBody = it.body.string()
                        val successful = it.isSuccessful
                        val code = it.code
                        requireActivity().runOnUiThread {
                            dismissLoadingDialog()
                            if (successful) {
                                if (responseBody.contains("id") || responseBody.contains("updated_at")) {
                                    Toast.makeText(context, "Request submitted successfully!", Toast.LENGTH_LONG).show()
                                    resetForm()
                                } else {
                                    Toast.makeText(context, "Response: $responseBody", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed: $code - $responseBody", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
        )
    }

    private fun resetForm() {
        fields.forEach { values[it.key] = "" }
    }

    private fun showLoadingDialog(message: String) {
        if (context == null) return
        if (loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }

        loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Please Wait")
            .setMessage(message)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        if (loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }
    }

    private fun loadRewardedAd() {
        val context = context
        if (context == null) {
            dismissLoadingDialog()
            sendInfoToServer(pendingValues)
            return
        }

        RewardedAd.load(
            context,
            SharedAdConfig.ANDROID_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    dismissLoadingDialog()
                    if (!isAdded || activity == null) {
                        rewardedAd = null
                        sendInfoToServer(pendingValues)
                        return
                    }
                    rewardedAd = ad
                    showRewardedAd()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    dismissLoadingDialog()
                    Toast.makeText(context, "Ad failed to load. Submitting your request...", Toast.LENGTH_SHORT).show()
                    sendInfoToServer(pendingValues)
                }
            },
        )
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        val activity = activity
        if (ad != null && activity != null) {
            ad.show(activity) {
                sendInfoToServer(pendingValues)
            }
        } else {
            Toast.makeText(context, "Ad not available. Submitting your request...", Toast.LENGTH_SHORT).show()
            sendInfoToServer(pendingValues)
        }
    }
}

data class RequestField(
    val key: String,
    val label: String,
    val required: Boolean = false,
    val multiline: Boolean = false,
    val keyboardType: KeyboardType = KeyboardType.Text,
)

@Composable
private fun RewardRequestScreen(
    title: String,
    description: String,
    fields: List<RequestField>,
    values: MutableMap<String, String>,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(description)
        fields.forEach { field ->
            OutlinedTextField(
                value = values[field.key].orEmpty(),
                onValueChange = { values[field.key] = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(field.label) },
                minLines = if (field.multiline) 4 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
            )
        }
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
            Text("Submit")
        }
    }
}
