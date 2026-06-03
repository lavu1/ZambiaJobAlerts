package com.solutions.alphil.zambiajobalerts.ui.gallery

import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeNativeAd

class GalleryFragment : Fragment() {
    private var aboutUsItems by mutableStateOf<List<AboutUsItem>>(emptyList())
    private var nativeAd by mutableStateOf<NativeAd?>(null)
    private var adLoader: AdLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        loadAboutUs()
        initializeNativeAd()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    AboutUsScreen(items = aboutUsItems, nativeAd = nativeAd)
                }
            }
        }
    }

    private fun loadAboutUs() {
        aboutUsItems = listOf(
            AboutUsItem("Expert Engineers", "Our team provides professional career guidance, resume writing, and interview coaching to help you stand out."),
            AboutUsItem("Experience Skills", "With years of experience in job placement and recruitment, we understand the job market and how to navigate it successfully."),
            AboutUsItem("Low Cost", "We offer cost-effective job placement solutions for both job seekers and employers, ensuring everyone gets value for their money."),
            AboutUsItem("Reliable & Verified Job Listings", "All job postings are carefully vetted to ensure authenticity, giving job seekers access to genuine opportunities."),
            AboutUsItem("Trusted Work And Transparent", "We maintain a high level of transparency and integrity in all our services, ensuring a seamless job search and hiring experience."),
            AboutUsItem("High Success Rate", "Many job seekers have successfully landed jobs through our platform, making us a trusted partner in career growth."),
        )
    }

    private fun initializeNativeAd() {
        adLoader = AdLoader.Builder(requireContext(), SharedAdConfig.ANDROID_NATIVE_AD_UNIT_ID)
            .forNativeAd { loadedAd ->
                if (!isAdded) {
                    loadedAd.destroy()
                    return@forNativeAd
                }

                nativeAd?.destroy()
                nativeAd = loadedAd
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("AboutAds", "Native ad failed to load: ${adError.message}")
                    }
                },
            )
            .build()
        adLoader?.loadAd(AdRequest.Builder().build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nativeAd?.destroy()
        nativeAd = null
        adLoader = null
    }

    data class AboutUsItem(
        val title: String,
        val description: String,
    )
}

@Composable
private fun AboutUsScreen(items: List<GalleryFragment.AboutUsItem>, nativeAd: NativeAd?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "About Us",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                color = Color(0xFF001F3F),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        }

        itemsIndexed(items) { index, item ->
            if (index == 1) {
                ComposeNativeAd(nativeAd)
            }
            AboutUsCard(item)
        }
    }
}

@Composable
private fun AboutUsCard(item: GalleryFragment.AboutUsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                color = Color(0xFF001F3F),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.description,
                modifier = Modifier.padding(top = 8.dp),
                color = Color(0xFF333333),
                fontSize = 14.sp,
            )
        }
    }
}
