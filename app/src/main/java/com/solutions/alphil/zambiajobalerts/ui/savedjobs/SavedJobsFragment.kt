package com.solutions.alphil.zambiajobalerts.ui.savedjobs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.Navigation
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.snackbar.Snackbar
import com.solutions.alphil.zambiajobalerts.AppViewIds
import com.solutions.alphil.zambiajobalerts.R
import com.solutions.alphil.zambiajobalerts.classes.Job
import com.solutions.alphil.zambiajobalerts.classes.JobActionHandler
import com.solutions.alphil.zambiajobalerts.classes.JobDetailsBottomSheet
import com.solutions.alphil.zambiajobalerts.classes.JobListCard
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.aigenerate.CVGeneratorFragment
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeNativeAd

class SavedJobsFragment : Fragment() {
    private lateinit var viewModel: SavedJobsViewModel
    private val handler = Handler(Looper.getMainLooper())
    private var rootView: View? = null
    private var nativeAds by mutableStateOf<List<NativeAd>>(emptyList())
    private var adLoader: AdLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application),
        )[SavedJobsViewModel::class.java]
        viewModel.loadSavedJobs()
        initializeNativeAds()

        return ComposeView(requireContext()).apply {
            this@SavedJobsFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    SavedJobsScreen(
                        viewModel = viewModel,
                        onDetails = { showJobDetails(it) },
                        onGenerateCv = { openGenerator(it, CVGeneratorFragment.ARG_CV_TYPE) },
                        onGenerateCoverLetter = { openGenerator(it, CVGeneratorFragment.ARG_COVER_TYPE) },
                        nativeAds = nativeAds,
                        onToggleSave = { toggleSaveJob(it) },
                    )
                }
            }
            scheduleLocalReminder()
        }
    }

    private fun showJobDetails(job: Job) {
        JobDetailsBottomSheet.newInstance(job.getId()).show(parentFragmentManager, "JobDetails")
    }

    private fun openGenerator(job: Job, prefillType: String) {
        val args = Bundle().apply {
            putInt(CVGeneratorFragment.ARG_SOURCE_JOB_ID, job.getId())
            putString(CVGeneratorFragment.ARG_SOURCE_JOB_TITLE, job.getTitle())
            putString(CVGeneratorFragment.ARG_SOURCE_COMPANY, job.getCompany())
            putString(CVGeneratorFragment.ARG_PREFILL_TYPE, prefillType)
        }
        Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
            .navigate(R.id.nav_ai, args)
    }

    private fun toggleSaveJob(job: Job) {
        viewModel.toggleJob(job)
        Snackbar.make(rootView ?: requireActivity().findViewById(android.R.id.content), "Saved job updated", Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun initializeNativeAds() {
        adLoader = AdLoader.Builder(requireContext(), SharedAdConfig.ANDROID_NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                if (!isAdded) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                nativeAds = nativeAds + nativeAd
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        // Keep saved jobs usable if an ad request is not filled.
                    }
                },
            )
            .build()
        adLoader?.loadAds(AdRequest.Builder().build(), 4)
    }

    private fun scheduleLocalReminder() {
        handler.postDelayed(
            {
                val savedJobs = viewModel.getSavedJobs().value.orEmpty()
                val root = rootView
                if (isAdded && savedJobs.isNotEmpty() && root != null) {
                    Snackbar.make(root, "Check your saved jobs! New opportunities are arriving.", Snackbar.LENGTH_LONG)
                        .setAction("View") {
                            // Already on Saved Jobs.
                        }
                        .show()
                }
                if (isAdded) {
                    scheduleLocalReminder()
                }
            },
            60000L * 30L,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        nativeAds.forEach { it.destroy() }
        nativeAds = emptyList()
        adLoader = null
        rootView = null
    }
}

@Composable
private fun SavedJobsScreen(
    viewModel: SavedJobsViewModel,
    onDetails: (Job) -> Unit,
    onGenerateCv: (Job) -> Unit,
    onGenerateCoverLetter: (Job) -> Unit,
    nativeAds: List<NativeAd>,
    onToggleSave: (Job) -> Unit,
) {
    val context = LocalContext.current
    val savedJobs by viewModel.getSavedJobs().observeAsState(emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Saved Jobs", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (savedJobs.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("No saved jobs yet.")
                    Text("Jobs you save on this device will appear here.")
                }
            }
            item {
                nativeAds.firstOrNull()?.let { nativeAd ->
                    ComposeNativeAd(nativeAd = nativeAd, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        } else {
            itemsIndexed(savedJobs, key = { _, job -> job.getId() }) { index, job ->
                JobListCard(
                    job = job,
                    onApply = { JobActionHandler.applyForJob(context, job) },
                    onShare = { JobActionHandler.shareJob(context, job) },
                    onDetails = { onDetails(job) },
                    onGenerateCv = { onGenerateCv(job) },
                    onGenerateCoverLetter = { onGenerateCoverLetter(job) },
                    isSaved = true,
                    onToggleSave = { onToggleSave(job) },
                )

                val adIndex = (index + 1) / 3 - 1
                if ((index + 1) % 3 == 0 && adIndex in nativeAds.indices) {
                    ComposeNativeAd(
                        nativeAd = nativeAds[adIndex],
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }

        item {
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        }
    }
}
