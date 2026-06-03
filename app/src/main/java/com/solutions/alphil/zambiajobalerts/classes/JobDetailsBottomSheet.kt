package com.solutions.alphil.zambiajobalerts.classes

import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.solutions.alphil.zambiajobalerts.AppViewIds
import com.solutions.alphil.zambiajobalerts.R
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.aigenerate.CVGeneratorFragment
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import com.solutions.alphil.zambiajobalerts.ui.jobs.JobsViewModel
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsViewModel

class JobDetailsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var viewModel: JobsViewModel
    private lateinit var savedJobsViewModel: SavedJobsViewModel
    private lateinit var repository: JobRepository

    private var jobId = -1
    private var jobSlug: String? = null
    private var openedFromDeepLink = false
    private var hasShownJobLoadedAd = false

    private var currentJob by mutableStateOf<Job?>(null)
    private var isLoading by mutableStateOf(true)
    private var loadError by mutableStateOf<String?>(null)
    private var isSaved by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            jobId = args.getInt(ARG_JOB_ID, -1)
            jobSlug = args.getString(ARG_JOB_SLUG)
            openedFromDeepLink = args.getBoolean(ARG_OPENED_FROM_DEEP_LINK, false)
        }
        repository = JobRepository(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    JobDetailsContent(
                        job = currentJob,
                        isLoading = isLoading,
                        loadError = loadError,
                        isSaved = isSaved,
                        onApply = { currentJob?.let { JobActionHandler.applyForJob(requireContext(), it) } },
                        onShare = { currentJob?.let { JobActionHandler.shareJob(requireContext(), it) } },
                        onToggleSave = { toggleSave() },
                        onGenerateCv = { openGenerator(CVGeneratorFragment.ARG_CV_TYPE) },
                        onGenerateCover = { openGenerator(CVGeneratorFragment.ARG_COVER_TYPE) },
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[JobsViewModel::class.java]
        savedJobsViewModel = ViewModelProvider(requireActivity())[SavedJobsViewModel::class.java]

        loadJobDetails()
        showPreloadedInterstitialIfAvailable()
    }

    private fun showPreloadedInterstitialIfAvailable() {
        if (!AdManager.getInstance().isInterstitialAdLoaded()) return

        val interstitialAd = AdManager.getInstance().getInterstitialAd()
        val activity = activity
        val context = context
        if (interstitialAd != null && activity != null && !activity.isFinishing && !activity.isDestroyed) {
            interstitialAd.show(activity)
            AdManager.getInstance().clearInterstitialAd()
            if (context != null) {
                AdManager.getInstance().loadInterstitialAd(context)
            }
        }
    }

    private fun loadJobDetails() {
        isLoading = true
        loadError = null

        if (jobId != -1) {
            repository.fetchJobDetails(
                jobId,
                object : JobRepository.ResponseListener<JobEntity> {
                    override fun onResponse(result: JobEntity) {
                        onJobLoaded(result)
                    }

                    override fun onError(error: String) {
                        onLoadError(error)
                    }
                },
            )
        } else if (jobSlug != null) {
            viewModel.fetchJobDetailsBySlug(jobSlug!!)
            viewModel.getJobDetails().observe(viewLifecycleOwner) { job ->
                if (job != null) {
                    currentJob = job
                    updateSaveButtonState(job.getId())
                    isLoading = false
                    showJobLoadedAdOnce()
                }
            }
        } else {
            isLoading = false
            loadError = "Job not found"
        }
    }

    private fun onJobLoaded(result: JobEntity) {
        val loadedJob = Job.fromEntity(result)
        runOnActiveUiThread {
            currentJob = loadedJob
            updateSaveButtonState(loadedJob.getId())
            isLoading = false
            showJobLoadedAdOnce()
        }
    }

    private fun onLoadError(error: String) {
        runOnActiveUiThread {
            isLoading = false
            loadError = "Offline: $error"
            Toast.makeText(context, "Offline: $error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runOnActiveUiThread(action: () -> Unit) {
        val activity: FragmentActivity = activity ?: return
        activity.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            action()
        }
    }

    private fun showJobLoadedAdOnce() {
        if (hasShownJobLoadedAd || !isAdded || activity == null) return

        hasShownJobLoadedAd = true
        if (openedFromDeepLink) {
            showRewardedAdOnceAfterJobLoaded()
        } else {
            showInterstitialAdOnceAfterJobLoaded()
        }
    }

    private fun showInterstitialAdOnceAfterJobLoaded() {
        val activity = activity ?: return

        if (!AdManager.getInstance().isInterstitialAdLoaded()) {
            loadNextInterstitialAd()
            return
        }

        val interstitialAd = AdManager.getInstance().getInterstitialAd() ?: return
        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                AdManager.getInstance().clearInterstitialAd()
                loadNextInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdManager.getInstance().clearInterstitialAd()
                loadNextInterstitialAd()
            }
        }

        interstitialAd.show(activity)
    }

    private fun loadNextInterstitialAd() {
        context?.let {
            AdManager.getInstance().loadInterstitialAd(it)
        }
    }

    private fun showRewardedAdOnceAfterJobLoaded() {
        val context = context ?: return

        RewardedAd.load(
            context,
            SharedAdConfig.ANDROID_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    val activity = activity
                    if (!isAdded || activity == null) return

                    rewardedAd.show(activity) {
                        // The ad is only used after deep-link job loading.
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("JobDetailsBottomSheet", "Rewarded ad failed to load: ${loadAdError.message}")
                }
            },
        )
    }

    private fun toggleSave() {
        val job = currentJob ?: return
        if (savedJobsViewModel.isJobSaved(job.getId())) {
            savedJobsViewModel.removeJob(job.getId())
        } else {
            savedJobsViewModel.saveJob(job)
        }
        updateSaveButtonState(job.getId())
    }

    private fun updateSaveButtonState(jobId: Int) {
        isSaved = savedJobsViewModel.isJobSaved(jobId)
    }

    private fun openGenerator(prefillType: String) {
        val job = currentJob ?: return

        val args = Bundle().apply {
            putInt(CVGeneratorFragment.ARG_SOURCE_JOB_ID, job.getId())
            putString(CVGeneratorFragment.ARG_SOURCE_JOB_TITLE, job.getTitle())
            putString(CVGeneratorFragment.ARG_SOURCE_COMPANY, job.getCompany())
            putString(CVGeneratorFragment.ARG_PREFILL_TYPE, prefillType)
        }

        Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
            .navigate(R.id.nav_ai, args)
        dismiss()
    }

    companion object {
        private const val ARG_JOB_ID = "job_id"
        private const val ARG_JOB_SLUG = "job_slug"
        private const val ARG_OPENED_FROM_DEEP_LINK = "opened_from_deep_link"

        @JvmStatic
        fun newInstance(jobId: Int): JobDetailsBottomSheet = newInstance(jobId, false)

        @JvmStatic
        fun newInstance(jobId: Int, openedFromDeepLink: Boolean): JobDetailsBottomSheet =
            JobDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_JOB_ID, jobId)
                    putBoolean(ARG_OPENED_FROM_DEEP_LINK, openedFromDeepLink)
                }
            }

        @JvmStatic
        fun newInstance(slug: String): JobDetailsBottomSheet = newInstance(slug, false)

        @JvmStatic
        fun newInstance(slug: String, openedFromDeepLink: Boolean): JobDetailsBottomSheet =
            JobDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_JOB_SLUG, slug)
                    putBoolean(ARG_OPENED_FROM_DEEP_LINK, openedFromDeepLink)
                }
            }
    }
}

@Composable
private fun JobDetailsContent(
    job: Job?,
    isLoading: Boolean,
    loadError: String?,
    isSaved: Boolean,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onToggleSave: () -> Unit,
    onGenerateCv: () -> Unit,
    onGenerateCover: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Loading job details...", modifier = Modifier.padding(top = 8.dp))
                }
            }
            job == null -> {
                Text(loadError ?: "Job details unavailable")
            }
            else -> {
                JobDetailsBody(
                    job = job,
                    isSaved = isSaved,
                    onApply = onApply,
                    onShare = onShare,
                    onToggleSave = onToggleSave,
                    onGenerateCv = onGenerateCv,
                    onGenerateCover = onGenerateCover,
                )
            }
        }
    }
}

@Composable
private fun JobDetailsBody(
    job: Job,
    isSaved: Boolean,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onToggleSave: () -> Unit,
    onGenerateCv: () -> Unit,
    onGenerateCover: () -> Unit,
) {
    val primary = Color(0xFF001F3F)
    val imageUrl = job.getFeaturedImage()
    val description = cleanHtml(job.getContent())
    val splitIndex = if (description.length > 600) {
        description.indexOf(" ", description.length / 2).takeIf { it > 0 }
    } else {
        null
    }

    if (imageUrl.isNotBlank()) {
        JobLogo(imageUrl)
    }

    Text(cleanHtml(job.getTitle()), color = primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    DetailLine("Company", job.getCompany())
    DetailLine("Location", job.getLocation())
    DetailLine("Date", job.getFormattedDate().orEmpty())
    DetailLine("Type", job.getJobType())

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onApply, modifier = Modifier.weight(1f)) {
            Text("Apply")
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Text("Share")
        }
        OutlinedButton(onClick = onToggleSave, modifier = Modifier.weight(1f)) {
            Text(if (isSaved) "Saved" else "Save")
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onGenerateCv, modifier = Modifier.weight(1f)) {
            Text("Generate CV")
        }
        Button(onClick = onGenerateCover, modifier = Modifier.weight(1f)) {
            Text("Generate Cover")
        }
    }

    ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (splitIndex != null) {
                Text(description.substring(0, splitIndex).trim())
                ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
                Text(description.substring(splitIndex).trim())
            } else {
                Text(description)
            }
        }
    }

    ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
}

@Composable
private fun JobLogo(imageUrl: String) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        factory = {
            ImageView(it).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        },
        update = { imageView ->
            Glide.with(context).load(imageUrl).into(imageView)
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    val cleaned = cleanHtml(value)
    if (cleaned.isNotBlank()) {
        Text("$label: $cleaned")
    }
}

private fun cleanHtml(value: String): String =
    Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
