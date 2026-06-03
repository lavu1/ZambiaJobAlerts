package com.solutions.alphil.zambiajobalerts.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
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
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
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
import com.solutions.alphil.zambiajobalerts.ui.jobs.JobsViewModel
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsViewModel

class HomeFragment : Fragment() {
    private lateinit var viewModel: JobsViewModel
    private lateinit var savedJobsViewModel: SavedJobsViewModel
    private var nativeAds by mutableStateOf<List<NativeAd>>(emptyList())
    private var savedJobIds by mutableStateOf<Set<Int>>(emptySet())
    private var adLoader: AdLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application),
        )[JobsViewModel::class.java]
        savedJobsViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application),
        )[SavedJobsViewModel::class.java]
        viewModel.loadLimitedJobs(3)
        MobileAds.initialize(requireContext())
        initializeNativeAds()
        savedJobsViewModel.getSavedJobs().observe(viewLifecycleOwner) { savedJobs ->
            savedJobIds = savedJobs.orEmpty().map { it.getId() }.toSet()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    HomeScreen(
                        viewModel = viewModel,
                        onRefresh = { viewModel.loadLimitedJobs(3) },
                        onLoadMore = { viewModel.loadMoreJobs() },
                        onViewAllJobs = { navigateToJobs() },
                        onDetails = { showJobDetails(it) },
                        onGenerateCv = { openGenerator(it, CVGeneratorFragment.ARG_CV_TYPE) },
                        onGenerateCoverLetter = { openGenerator(it, CVGeneratorFragment.ARG_COVER_TYPE) },
                        nativeAds = nativeAds,
                        savedJobIds = savedJobIds,
                        onToggleSave = { toggleSaveJob(it) },
                    )
                }
            }
        }
    }

    private fun navigateToJobs() {
        Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
            .navigate(R.id.nav_jobs)
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
        val saved = savedJobsViewModel.toggleJob(job)
        Toast.makeText(
            requireContext(),
            if (saved) "Job saved on this device" else "Job removed from saved jobs",
            Toast.LENGTH_SHORT,
        ).show()
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
                        // Keep content visible if ads cannot be served.
                    }
                },
            )
            .build()
        adLoader?.loadAds(AdRequest.Builder().build(), 4)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nativeAds.forEach { it.destroy() }
        nativeAds = emptyList()
        adLoader = null
    }
}

@Composable
private fun HomeScreen(
    viewModel: JobsViewModel,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onViewAllJobs: () -> Unit,
    onDetails: (Job) -> Unit,
    onGenerateCv: (Job) -> Unit,
    onGenerateCoverLetter: (Job) -> Unit,
    nativeAds: List<NativeAd>,
    savedJobIds: Set<Int>,
    onToggleSave: (Job) -> Unit,
) {
    val context = LocalContext.current
    val jobs by viewModel.getJobs().observeAsState(emptyList())
    val loading by viewModel.getLoading().observeAsState(false)
    val error by viewModel.getError().observeAsState()
    val hasMoreData by viewModel.getHasMoreData().observeAsState(true)
    var visibleJobCount by remember { mutableStateOf(3) }
    val featuredJobs = jobs.take(visibleJobCount)
    val canLoadMore = featuredJobs.size < jobs.size || hasMoreData

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        }
        item {
            Text(
                text = "Welcome to Zambia Job Alerts",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                text = "Finding a job shouldn't be stressful. We connect you with daily job updates, CV and cover letter tools, employer postings, and career services across Zambia.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(onClick = onViewAllJobs, modifier = Modifier.fillMaxWidth()) {
                Text("View All Jobs")
            }
        }
        item {
            Text("Featured Jobs", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (loading && featuredJobs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Loading jobs...", modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else if (featuredJobs.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error ?: "No jobs available right now.")
                    OutlinedButton(
                        onClick = {
                            visibleJobCount = 3
                            onRefresh()
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (loading) "Refreshing..." else "Refresh")
                    }
                }
            }
        } else {
            itemsIndexed(featuredJobs, key = { _, job -> job.getId() }) { index, job ->
                JobListCard(
                    job = job,
                    onApply = { JobActionHandler.applyForJob(context, job) },
                    onShare = { JobActionHandler.shareJob(context, job) },
                    onDetails = { onDetails(job) },
                    onGenerateCv = { onGenerateCv(job) },
                    onGenerateCoverLetter = { onGenerateCoverLetter(job) },
                    isSaved = savedJobIds.contains(job.getId()),
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
            item {
                Button(
                    onClick = {
                        if (featuredJobs.size < jobs.size) {
                            visibleJobCount += 3
                        } else {
                            onLoadMore()
                        }
                    },
                    enabled = !loading && canLoadMore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_load_more),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (loading) "Loading jobs..." else "Load More Jobs")
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        visibleJobCount = 3
                        onRefresh()
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_refresh),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (loading) "Refreshing..." else "Refresh Jobs")
                }
            }
        }
        item {
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
        }
    }
}
