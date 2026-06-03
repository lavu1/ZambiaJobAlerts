package com.solutions.alphil.zambiajobalerts.ui.slideshow

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

class SlideshowFragment : Fragment() {
    private var services by mutableStateOf<List<ServiceItem>>(emptyList())
    private var nativeAd by mutableStateOf<NativeAd?>(null)
    private var adLoader: AdLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        loadServices()
        initializeNativeAd()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ServicesScreen(services = services, nativeAd = nativeAd)
                }
            }
        }
    }

    private fun loadServices() {
        services = listOf(
            ServiceItem("Job Posting & Recruitment", "Employers can post job vacancies and find the best candidates through our platform"),
            ServiceItem("Resume Writing & Review", "We help job seekers create professional resumes that stand out to potential employers"),
            ServiceItem("Application Letter Writing", "Our team assists in crafting compelling and personalized job application letters"),
            ServiceItem("Career Guidance & Coaching", "We provide expert career advice, interview tips, and industry insights to help job seekers succeed."),
            ServiceItem("Internship & Graduate Programs", "We connect fresh graduates with internship opportunities to gain valuable work experience."),
            ServiceItem("Freelance & Remote Work Opportunities", "We help professionals find remote or freelance jobs suited to their skills and experience."),
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
                        Log.e("ServicesAds", "Native ad failed to load: ${adError.message}")
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

    data class ServiceItem(
        val title: String,
        val description: String,
    )
}

@Composable
private fun ServicesScreen(services: List<SlideshowFragment.ServiceItem>, nativeAd: NativeAd?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Our Services",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                color = Color(0xFF001F3F),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
        }

        itemsIndexed(services) { index, service ->
            if (index == 1) {
                ComposeNativeAd(nativeAd)
            }
            ServiceCard(service)
        }
    }
}

@Composable
private fun ServiceCard(service: SlideshowFragment.ServiceItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = service.title,
                color = Color(0xFF001F3F),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = service.description,
                modifier = Modifier.padding(top = 8.dp),
                color = Color(0xFF333333),
                fontSize = 14.sp,
            )
        }
    }
}
