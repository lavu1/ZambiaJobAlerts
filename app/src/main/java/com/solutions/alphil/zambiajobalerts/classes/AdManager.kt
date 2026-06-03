package com.solutions.alphil.zambiajobalerts.classes

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig

class AdManager private constructor() {
    private var interstitialAd: InterstitialAd? = null

    fun loadInterstitialAd(context: Context) {
        val appContext = context.applicationContext
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { loadInterstitialAd(appContext) }
            return
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.i(TAG, "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.i(TAG, loadAdError.message)
                    interstitialAd = null
                }
            },
        )
    }

    fun isInterstitialAdLoaded(): Boolean = interstitialAd != null

    fun getInterstitialAd(): InterstitialAd? = interstitialAd

    fun clearInterstitialAd() {
        interstitialAd = null
    }

    companion object {
        private const val TAG = "AdManager"
        private const val INTERSTITIAL_AD_UNIT_ID = SharedAdConfig.ANDROID_INTERSTITIAL_AD_UNIT_ID

        @Volatile
        private var instance: AdManager? = null

        @JvmStatic
        fun getInstance(): AdManager =
            instance ?: synchronized(this) {
                instance ?: AdManager().also { instance = it }
            }
    }
}
