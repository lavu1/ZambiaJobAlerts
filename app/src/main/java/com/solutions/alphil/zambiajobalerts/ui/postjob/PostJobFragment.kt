package com.solutions.alphil.zambiajobalerts.ui.postjob

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.livedata.observeAsState
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd

class PostJobFragment : Fragment() {
    private lateinit var viewModel: PostJobViewModel
    private var interstitialAd: InterstitialAd? = null

    private val categoryMap = linkedMapOf<String, Int>()
    private val typeMap = linkedMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(this)[PostJobViewModel::class.java]
        setupMappings()
        loadInterstitialAd()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    PostJobScreen(
                        viewModel = viewModel,
                        categories = categoryMap.keys.toList(),
                        types = typeMap.keys.toList(),
                        onPickCategories = { selected, update -> showCategoryDialog(selected, update) },
                        onPickType = { current, update -> showTypeDialog(current, update) },
                        onSubmit = { payload -> submitJob(payload) },
                    )
                }
            }
        }
    }

    private fun setupMappings() {
        if (categoryMap.isNotEmpty()) return

        categoryMap["Accountant"] = 11
        categoryMap["Administrator"] = 12
        categoryMap["Agriculture"] = 13
        categoryMap["Banking/Finance"] = 14
        categoryMap["Development"] = 15
        categoryMap["Education"] = 16
        categoryMap["Engineer/Construction"] = 17
        categoryMap["Health"] = 18
        categoryMap["Hospitality"] = 19
        categoryMap["Human Resources"] = 20
        categoryMap["IT/Telecoms"] = 21
        categoryMap["Legal"] = 22
        categoryMap["Manufacturing/FMCG"] = 23
        categoryMap["Marketing/PR"] = 24
        categoryMap["Public Sector"] = 26
        categoryMap["Retail/Sales"] = 27
        categoryMap["Logistics/Transport"] = 28
        categoryMap["Other"] = 25

        typeMap["Full Time"] = 6
        typeMap["Part Time"] = 7
        typeMap["Temporary"] = 8
        typeMap["Freelance"] = 9
        typeMap["Internship"] = 10
        typeMap["Consultancy"] = 30
        typeMap["Contract"] = 31
        typeMap["Tender"] = 32
    }

    private fun showCategoryDialog(
        selected: List<String>,
        onSelected: (List<String>) -> Unit,
    ) {
        val categories = categoryMap.keys.toTypedArray()
        val checked = BooleanArray(categories.size) { index -> categories[index] in selected }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Categories")
            .setMultiChoiceItems(categories, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                onSelected(categories.filterIndexed { index, _ -> checked[index] })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTypeDialog(current: String, onSelected: (String) -> Unit) {
        val types = typeMap.keys.toTypedArray()
        val currentIndex = types.indexOf(current).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("Select Job Type")
            .setSingleChoiceItems(types, currentIndex) { dialog, which ->
                onSelected(types[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitJob(payload: PostJobPayload) {
        val categoryIds = payload.categories.mapNotNull { categoryMap[it] }
        val jobTypeId = typeMap[payload.jobType] ?: 6

        val ad = interstitialAd
        val activity = activity
        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    post(payload, categoryIds, jobTypeId)
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    post(payload, categoryIds, jobTypeId)
                    loadInterstitialAd()
                }
            }
            ad.show(activity)
        } else {
            post(payload, categoryIds, jobTypeId)
        }
    }

    private fun post(payload: PostJobPayload, categoryIds: List<Int>, jobTypeId: Int) {
        viewModel.postJob(
            payload.title,
            payload.company,
            payload.location,
            payload.description,
            payload.applicationLink,
            categoryIds,
            jobTypeId,
        )
    }

    private fun loadInterstitialAd() {
        val context: Context = context ?: return
        InterstitialAd.load(
            context,
            SharedAdConfig.ANDROID_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = if (isAdded) ad else null
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            },
        )
    }
}

data class PostJobPayload(
    val title: String,
    val company: String,
    val location: String,
    val description: String,
    val applicationLink: String,
    val categories: List<String>,
    val jobType: String,
)

@Composable
private fun PostJobScreen(
    viewModel: PostJobViewModel,
    categories: List<String>,
    types: List<String>,
    onPickCategories: (List<String>, (List<String>) -> Unit) -> Unit,
    onPickType: (String, (String) -> Unit) -> Unit,
    onSubmit: (PostJobPayload) -> Unit,
) {
    val context = LocalContext.current
    val isPosting by viewModel.isPosting().observeAsState(false)
    val result by viewModel.getPostResult().observeAsState()

    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var applicationLink by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedType by remember { mutableStateOf(types.firstOrNull().orEmpty()) }

    LaunchedEffect(result) {
        val message = result ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        if (message.contains("successfully", ignoreCase = true)) {
            title = ""
            company = ""
            location = ""
            description = ""
            applicationLink = ""
            selectedCategories = emptyList()
            selectedType = types.firstOrNull().orEmpty()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        Text("Post a Job", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        JobTextField(title, { title = it }, "Job Title")
        JobTextField(company, { company = it }, "Company Name")
        JobTextField(location, { location = it }, "Location")
        JobTextField(description, { description = it }, "Description", multiline = true)
        JobTextField(
            value = applicationLink,
            onValueChange = { applicationLink = it },
            label = "Application Email or Link",
            keyboardType = KeyboardType.Uri,
        )

        OutlinedButton(
            onClick = { onPickCategories(selectedCategories) { selectedCategories = it } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selectedCategories.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Select Categories")
        }

        OutlinedButton(
            onClick = { onPickType(selectedType) { selectedType = it } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selectedType.ifBlank { "Select Job Type" })
        }

        Button(
            onClick = {
                if (
                    title.isBlank() ||
                    company.isBlank() ||
                    location.isBlank() ||
                    description.isBlank() ||
                    applicationLink.isBlank()
                ) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                onSubmit(
                    PostJobPayload(
                        title = title.trim(),
                        company = company.trim(),
                        location = location.trim(),
                        description = description.trim(),
                        applicationLink = applicationLink.trim(),
                        categories = selectedCategories,
                        jobType = selectedType.ifBlank { "Full Time" },
                    ),
                )
            },
            enabled = !isPosting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isPosting) "Posting..." else "Submit Job")
        }

        if (isPosting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
    }
}

@Composable
private fun JobTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = if (multiline) 4 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
