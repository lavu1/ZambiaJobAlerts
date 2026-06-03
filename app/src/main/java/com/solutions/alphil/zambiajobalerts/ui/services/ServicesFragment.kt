package com.solutions.alphil.zambiajobalerts.ui.services

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.Navigation
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.solutions.alphil.zambiajobalerts.AppViewIds
import com.solutions.alphil.zambiajobalerts.R
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd

class ServicesFragment : Fragment() {
    private lateinit var viewModel: ServicesViewModel
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var lastRewardTier = 0
    private var isLoadingAd by mutableStateOf(false)
    private var loadingDialog: AlertDialog? = null
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(this)[ServicesViewModel::class.java]

        return ComposeView(requireContext()).apply {
            this@ServicesFragment.rootView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ServicesRewardsScreen(
                        viewModel = viewModel,
                        isLoadingAd = isLoadingAd,
                        onWatchAd = { loadRewardedAd() },
                        onRedeem = { handleRedeem() },
                        onRewardCountChanged = { checkForNewUnlock(it) },
                    )
                }
            }
        }
    }

    private fun loadRewardedAd() {
        if (isLoadingAd) return

        isLoadingAd = true
        showLoadingDialog("Loading video ad...")

        val context = context
        if (context == null) {
            dismissLoadingDialog()
            isLoadingAd = false
            return
        }

        RewardedInterstitialAd.load(
            context,
            SharedAdConfig.ANDROID_REWARDED_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    dismissLoadingDialog()
                    isLoadingAd = false
                    if (!isAdded || activity == null) {
                        rewardedInterstitialAd = null
                        return
                    }
                    rewardedInterstitialAd = ad
                    showRewardedAd()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    dismissLoadingDialog()
                    isLoadingAd = false
                    Toast.makeText(context, "Failed to load ad. Please try again.", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    private fun showRewardedAd() {
        val ad = rewardedInterstitialAd
        val activity = activity
        if (ad == null || activity == null) {
            Toast.makeText(context, "Ad not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
            }
        }

        ad.show(activity) {
            viewModel.addAdWatched()
            Toast.makeText(context, "Reward earned!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRedeem() {
        val ads = viewModel.getAdsWatched().value ?: 0

        if (ads >= 10) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose Service")
                .setItems(
                    arrayOf(
                        "Email Alerts (1 ad)",
                        "Phone Alerts (1 ad)",
                        "Priority Job Applications (3 ads)",
                        "CV Review/Write (5 ads)",
                        "Career Coaching (7 ads)",
                    ),
                ) { _, which ->
                    when (which) {
                        0 -> redeem(ads, 1, R.id.nav_email_alerts)
                        1 -> redeem(ads, 1, R.id.nav_phone_alerts)
                        2 -> redeem(ads, 3, R.id.nav_priority_job)
                        3 -> chooseCvService(ads)
                        4 -> redeem(ads, 7, R.id.nav_career_coaching)
                    }
                }
                .show()
        } else {
            showIndividualOptions(ads)
        }
    }

    private fun chooseCvService(ads: Int) {
        if (ads < 5) {
            Toast.makeText(context, "Not enough ads!", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose CV Service")
            .setItems(arrayOf("CV Review / Update (5 ads)", "CV Write from Scratch (5 ads)")) { _, which ->
                viewModel.deductAds(5)
                navigateTo(if (which == 0) R.id.nav_cv_review else R.id.nav_cv_write)
            }
            .show()
    }

    private fun showIndividualOptions(ads: Int) {
        val options = mutableListOf<String>()
        val costs = mutableListOf<Int>()
        val destinationIds = mutableListOf<Int>()

        if (ads >= 1) {
            options.add("Email Alerts (1 ad)")
            costs.add(1)
            destinationIds.add(R.id.nav_email_alerts)
            options.add("Phone Alerts (1 ad)")
            costs.add(1)
            destinationIds.add(R.id.nav_phone_alerts)
        }

        if (ads >= 3) {
            options.add("Priority Job Applications (3 ads)")
            costs.add(3)
            destinationIds.add(R.id.nav_priority_job)
        }

        if (ads >= 5) {
            options.add("CV Review / Update (5 ads)")
            costs.add(5)
            destinationIds.add(R.id.nav_cv_review)
            options.add("CV Write from Scratch (5 ads)")
            costs.add(5)
            destinationIds.add(R.id.nav_cv_write)
        }

        if (ads >= 7) {
            options.add("Career Coaching Session (7 ads)")
            costs.add(7)
            destinationIds.add(R.id.nav_career_coaching)
        }

        if (options.isEmpty()) {
            Toast.makeText(context, "Watch more ads to unlock rewards!", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Redeem Rewards ($ads ads available)")
            .setItems(options.toTypedArray()) { _, which ->
                redeem(ads, costs[which], destinationIds[which])
            }
            .show()
    }

    private fun redeem(ads: Int, cost: Int, destinationId: Int) {
        if (ads >= cost) {
            viewModel.deductAds(cost)
            navigateTo(destinationId)
        } else {
            Toast.makeText(context, "Not enough ads!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForNewUnlock(adsWatched: Int) {
        val newTier = when {
            adsWatched >= 10 -> 10
            adsWatched >= 7 -> 7
            adsWatched >= 5 -> 5
            adsWatched >= 3 -> 3
            adsWatched >= 1 -> 1
            else -> 0
        }

        val root = rootView
        if (newTier > lastRewardTier && root != null) {
            Snackbar.make(root, "New Reward Unlocked! (${rewardName(newTier)})", Snackbar.LENGTH_LONG)
                .setAction("Redeem") { handleRedeem() }
                .show()
            lastRewardTier = newTier
        }
    }

    private fun rewardName(tier: Int): String =
        when (tier) {
            1 -> "Job Alerts"
            3 -> "Priority Job Applications"
            5 -> "CV Services"
            7 -> "Career Coaching"
            10 -> "All Rewards"
            else -> "Reward"
        }

    private fun navigateTo(destinationId: Int) {
        if (!isAdded || activity == null) return
        Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
            .navigate(destinationId)
    }

    private fun showLoadingDialog(message: String) {
        if (context == null) return
        loadingDialog?.dismiss()
        loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Please Wait")
            .setMessage(message)
            .setCancelable(true)
            .create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
        dismissLoadingDialog()
    }
}

@Composable
private fun ServicesRewardsScreen(
    viewModel: ServicesViewModel,
    isLoadingAd: Boolean,
    onWatchAd: () -> Unit,
    onRedeem: () -> Unit,
    onRewardCountChanged: (Int) -> Unit,
) {
    val adsWatched by viewModel.getAdsWatched().observeAsState(0)
    val jobsViewed by viewModel.getJobsViewed().observeAsState(0)

    LaunchedEffect(adsWatched) {
        onRewardCountChanged(adsWatched)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        Text("Rewards", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            RewardStatCard("Ads Watched", adsWatched.toString(), Modifier.weight(1f))
            RewardStatCard("Jobs Viewed", jobsViewed.toString(), Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Available Rewards", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Ads Available: $adsWatched")
                Text(if (adsWatched >= 1) "Job Alerts unlocked (1 ad each)" else "Job Alerts need 1 ad")
                Text(if (adsWatched >= 3) "Priority Job Applications unlocked (3 ads)" else "Priority Job Applications need 3 ads")
                Text(if (adsWatched >= 5) "CV Services unlocked (5 ads)" else "CV Services need 5 ads")
                Text(if (adsWatched >= 7) "Career Coaching unlocked (7 ads)" else "Career Coaching needs 7 ads")
            }
        }

        Button(onClick = onWatchAd, enabled = !isLoadingAd, modifier = Modifier.fillMaxWidth()) {
            Text(if (isLoadingAd) "Loading..." else "Watch Ad to Earn Reward")
        }
        OutlinedButton(onClick = onRedeem, modifier = Modifier.fillMaxWidth()) {
            Text("Redeem Rewards")
        }
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
    }
}

@Composable
private fun RewardStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 13.sp, color = Color(0xFF4B5563))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
