package com.solutions.alphil.zambiajobalerts.ui.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.MobileAds
import com.solutions.alphil.zambiajobalerts.R
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd

class TermsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        MobileAds.initialize(requireContext())

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    TermsScreen(
                        title = getString(R.string.menu_terms),
                        partOne = getString(R.string.terms_part1),
                        partTwo = getString(R.string.terms_part2),
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsScreen(
    title: String,
    partOne: String,
    partTwo: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(partOne, modifier = Modifier.fillMaxWidth())
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
        Text(partTwo, modifier = Modifier.fillMaxWidth())
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
    }
}
