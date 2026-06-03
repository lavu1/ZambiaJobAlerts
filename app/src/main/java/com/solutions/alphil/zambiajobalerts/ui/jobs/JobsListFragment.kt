package com.solutions.alphil.zambiajobalerts.ui.jobs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
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
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsViewModel

class JobsListFragment : Fragment() {
    private lateinit var viewModel: JobsViewModel
    private lateinit var savedJobsViewModel: SavedJobsViewModel
    private lateinit var prefs: SharedPreferences
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private val nativeAdPool: MutableList<NativeAd> = ArrayList()
    private var adLoader: AdLoader? = null
    private var isDetailsShowing = false
    private var pendingSearchQuery = ""

    private var searchQuery by mutableStateOf("")
    private var displayItems by mutableStateOf<List<Any>>(emptyList())
    private var isLoading by mutableStateOf(false)
    private var hasMoreData by mutableStateOf(true)
    private var showNoJobs by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var savedJobIds by mutableStateOf<Set<Int>>(emptySet())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setupViewModel()
        setupSavedJobsViewModel()
        prefs = requireActivity().getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        initializeNativeAds()
        setupObservers()
        applySearchQuery(getInitialSearchQuery())
        openPendingJobFromLaunch()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    JobsListScreen(
                        displayItems = displayItems,
                        searchQuery = searchQuery,
                        isLoading = isLoading,
                        hasMoreData = hasMoreData,
                        showNoJobs = showNoJobs,
                        errorMessage = errorMessage,
                        onSearchChanged = { searchQuery = it },
                        onSearch = { viewModel.searchJobs(searchQuery.trim()) },
                        onRefresh = { refreshJobs() },
                        onLoadMore = { viewModel.loadMoreJobs() },
                        onJobDetails = { openJobDetails(it) },
                        onApply = { JobActionHandler.applyForJob(requireContext(), it) },
                        onShare = { JobActionHandler.shareJob(requireContext(), it) },
                        onGenerateCv = { openGeneratorForJob(it, CVGeneratorFragment.ARG_CV_TYPE) },
                        onGenerateCoverLetter = { openGeneratorForJob(it, CVGeneratorFragment.ARG_COVER_TYPE) },
                        savedJobIds = savedJobIds,
                        onToggleSave = { toggleSaveJob(it) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        maybeShowRandomSnackbar()
    }

    private fun maybeShowRandomSnackbar() {
        if (Math.random() < 0.2) {
            val messages = arrayOf(
                "Always tailor your CV to the specific job you apply for.",
                "Watch ads as a subscriber to keep your access active.",
                "Redeem your points and unlock special rewards.",
                "Your subscription is valid for only a day. Renew to stay premium.",
                "Keep applying. Your next opportunity is around the corner.",
            )
            val index = (Math.random() * messages.size).toInt()
            val rootView = requireActivity().findViewById<View>(android.R.id.content)
            Snackbar.make(rootView, messages[index], Snackbar.LENGTH_LONG)
                .setAction("Okay") {
                    if (isAdded) {
                        Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
                            .navigate(R.id.nav_rewards)
                    }
                }
                .show()
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application),
        )[JobsViewModel::class.java]
    }

    private fun setupSavedJobsViewModel() {
        savedJobsViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application),
        )[SavedJobsViewModel::class.java]
    }

    private fun openJobDetails(job: Job) {
        if (isDetailsShowing) return

        isDetailsShowing = true
        val viewCount = prefs.getInt("job_views", 0) + 1
        prefs.edit().putInt("job_views", viewCount).apply()

        val showDetails = Runnable {
            if (parentFragmentManager.findFragmentByTag("JobDetails") == null) {
                val detailsSheet = JobDetailsBottomSheet.newInstance(job.getId())
                detailsSheet.show(parentFragmentManager, "JobDetails")
            }
            isDetailsShowing = false
        }

        if (viewCount % 5 == 0) {
            showRewardedInterstitialAd(showDetails)
        } else {
            showDetails.run()
        }
    }

    private fun openGeneratorForJob(job: Job?, prefillType: String) {
        if (job == null || !isAdded) {
            return
        }

        val args = Bundle().apply {
            putInt(CVGeneratorFragment.ARG_SOURCE_JOB_ID, job.getId())
            putString(CVGeneratorFragment.ARG_SOURCE_JOB_TITLE, job.getTitle())
            putString(CVGeneratorFragment.ARG_SOURCE_COMPANY, job.getCompany())
            putString(CVGeneratorFragment.ARG_PREFILL_TYPE, prefillType)
        }

        try {
            Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
                .navigate(R.id.nav_ai, args)
            Toast.makeText(
                requireContext(),
                "Generating " + if (CVGeneratorFragment.ARG_COVER_TYPE == prefillType) "Cover Letter" else "CV",
                Toast.LENGTH_SHORT,
            ).show()
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Could not open generator for job ${job.getId()}", error)
            Toast.makeText(requireContext(), "Could not open generator. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSaveJob(job: Job) {
        val saved = savedJobsViewModel.toggleJob(job)
        Toast.makeText(
            requireContext(),
            if (saved) "Job saved on this device" else "Job removed from saved jobs",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun buildDisplayListWithAds(jobs: List<Job>): List<Any> {
        val displayList = mutableListOf<Any>()
        if (jobs.isEmpty()) return displayList

        var adIndex = 0
        for (index in jobs.indices) {
            displayList.add(jobs[index])
            if ((index + 1) % 4 == 0 && adIndex < nativeAdPool.size) {
                displayList.add(nativeAdPool[adIndex])
                adIndex++
            }
        }

        val totalAdsNeeded = jobs.size / 4 + 1
        if (nativeAdPool.size < totalAdsNeeded) {
            loadMoreNativeAds(totalAdsNeeded - nativeAdPool.size)
        }
        return displayList
    }

    private fun initializeNativeAds() {
        adLoader = AdLoader.Builder(requireContext(), SharedAdConfig.ANDROID_NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                if (!isAdded) {
                    nativeAd.destroy()
                    return@forNativeAd
                }

                nativeAdPool.add(nativeAd)
                val currentJobs = viewModel.getJobs().value
                if (!currentJobs.isNullOrEmpty()) {
                    displayItems = buildDisplayListWithAds(currentJobs)
                }
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("AdsDebug", "Native ad failed to load: ${adError.message}")
                    }
                },
            )
            .build()
        loadMoreNativeAds(3)
    }

    private fun loadMoreNativeAds(count: Int) {
        if (count > 0) {
            adLoader?.loadAds(AdRequest.Builder().build(), count)
        }
    }

    private fun showRewardedInterstitialAd(onAdDismissed: Runnable) {
        if (rewardedInterstitialAd == null) {
            val context = context
            if (context == null) {
                onAdDismissed.run()
                return
            }
            RewardedInterstitialAd.load(
                context,
                AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        if (!isAdded || activity == null) {
                            rewardedInterstitialAd = null
                            onAdDismissed.run()
                            return
                        }
                        rewardedInterstitialAd = ad
                        showLoadedAd(onAdDismissed)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedInterstitialAd = null
                        onAdDismissed.run()
                    }
                },
            )
        } else {
            showLoadedAd(onAdDismissed)
        }
    }

    private fun showLoadedAd(onAdDismissed: Runnable) {
        val ad = rewardedInterstitialAd
        val activity = activity
        if (ad == null || activity == null) {
            rewardedInterstitialAd = null
            onAdDismissed.run()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                onAdDismissed.run()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                onAdDismissed.run()
            }
        }

        ad.show(activity) {}
    }

    fun applySearchQuery(query: String?) {
        pendingSearchQuery = query?.trim().orEmpty()
        if (!::viewModel.isInitialized) {
            return
        }

        searchQuery = pendingSearchQuery
        viewModel.searchJobs(pendingSearchQuery)
    }

    private fun getInitialSearchQuery(): String {
        val args = arguments ?: return pendingSearchQuery
        return args.getString(ARG_SEARCH_QUERY, pendingSearchQuery)
    }

    private fun setupObservers() {
        viewModel.getJobs().observe(viewLifecycleOwner) { jobs ->
            if (!jobs.isNullOrEmpty()) {
                displayItems = buildDisplayListWithAds(jobs)
                showNoJobs = false
            } else {
                displayItems = emptyList()
                showNoJobs = true
            }
        }

        viewModel.getLoading().observe(viewLifecycleOwner) { loading ->
            isLoading = loading
            if (loading) {
                showNoJobs = false
            }
        }

        viewModel.getError().observe(viewLifecycleOwner) { error ->
            errorMessage = error
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                showNoJobs = true
            }
        }

        viewModel.getHasMoreData().observe(viewLifecycleOwner) { hasMore ->
            hasMoreData = hasMore
        }

        savedJobsViewModel.getSavedJobs().observe(viewLifecycleOwner) { savedJobs ->
            savedJobIds = savedJobs.orEmpty().map { it.getId() }.toSet()
        }
    }

    private fun refreshJobs() {
        showNoJobs = false
        viewModel.loadJobs(true)
    }

    private fun openPendingJobFromLaunch() {
        activity?.let { currentActivity ->
            val intent: Intent = currentActivity.intent
            val jobId = intent.getIntExtra("job_id", -1)
            if (jobId > 0) {
                intent.putExtra("job_id", -1)
                JobDetailsBottomSheet.newInstance(jobId).show(parentFragmentManager, "JobDetails")
            }
        }
        arguments?.let { args ->
            val openJobId = args.getInt("open_job_id", -1)
            if (openJobId > 0) {
                JobDetailsBottomSheet.newInstance(openJobId).show(parentFragmentManager, "JobDetails")
                args.remove("open_job_id")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nativeAdPool.forEach { it.destroy() }
        nativeAdPool.clear()
        adLoader = null
    }

    companion object {
        const val ARG_SEARCH_QUERY = "search_query"
        private const val AD_UNIT_ID = "ca-app-pub-2168080105757285/9306188221"
        private const val TAG = "JobsListFragment"
    }
}

@Composable
private fun JobsListScreen(
    displayItems: List<Any>,
    searchQuery: String,
    isLoading: Boolean,
    hasMoreData: Boolean,
    showNoJobs: Boolean,
    errorMessage: String?,
    onSearchChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onJobDetails: (Job) -> Unit,
    onApply: (Job) -> Unit,
    onShare: (Job) -> Unit,
    onGenerateCv: (Job) -> Unit,
    onGenerateCoverLetter: (Job) -> Unit,
    savedJobIds: Set<Int>,
    onToggleSave: (Job) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8)),
        state = listState,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search jobs") },
                )
                Button(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search")
                }
            }
        }

        if (showNoJobs) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = errorMessage ?: "No jobs found.",
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onRefresh) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh")
                        }
                    }
                }
            }
        } else {
            itemsIndexed(displayItems) { _, item ->
                when (item) {
                    is Job -> JobListCard(
                        job = item,
                        onApply = { onApply(item) },
                        onShare = { onShare(item) },
                        onDetails = { onJobDetails(item) },
                        onGenerateCv = { onGenerateCv(item) },
                        onGenerateCoverLetter = { onGenerateCoverLetter(item) },
                        isSaved = savedJobIds.contains(item.getId()),
                        onToggleSave = { onToggleSave(item) },
                    )

                    is NativeAd -> ComposeNativeAd(
                        nativeAd = item,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }

        item {
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    hasMoreData && searchQuery.isBlank() && !showNoJobs -> Button(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_load_more),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load More Jobs")
                    }
                }
            }
        }
    }
}
