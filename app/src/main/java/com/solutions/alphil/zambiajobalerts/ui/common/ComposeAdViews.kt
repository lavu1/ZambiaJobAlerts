package com.solutions.alphil.zambiajobalerts.ui.common

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun ComposeBannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

@Composable
fun ComposeNativeAd(nativeAd: NativeAd?, modifier: Modifier = Modifier) {
    if (nativeAd == null) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { context ->
            NativeAdView(context).apply {
                setBackgroundColor(Color.WHITE)
                setPadding(18, 18, 18, 18)

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                }

                val topRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                }

                val icon = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(72, 72).apply {
                        rightMargin = 14
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                val headline = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(0, 31, 63))
                }

                val choices = AdChoicesView(context)
                topRow.addView(icon)
                topRow.addView(headline)
                topRow.addView(choices)

                val body = TextView(context).apply {
                    textSize = 14f
                    setTextColor(Color.rgb(51, 51, 51))
                    setPadding(0, 10, 0, 0)
                }

                val advertiser = TextView(context).apply {
                    textSize = 12f
                    setTextColor(Color.rgb(102, 102, 102))
                    setPadding(0, 6, 0, 0)
                }

                val callToAction = Button(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = 12
                    }
                    isAllCaps = false
                }

                container.addView(topRow)
                container.addView(body)
                container.addView(advertiser)
                container.addView(callToAction)
                addView(container)

                headlineView = headline
                bodyView = body
                advertiserView = advertiser
                callToActionView = callToAction
                iconView = icon
                adChoicesView = choices
            }
        },
        update = { nativeAdView ->
            bindProgrammaticNativeAd(nativeAdView, nativeAd)
        },
    )
}

private fun bindProgrammaticNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    val headline = nativeAdView.headlineView as TextView
    val body = nativeAdView.bodyView as TextView
    val advertiser = nativeAdView.advertiserView as TextView
    val callToAction = nativeAdView.callToActionView as Button
    val icon = nativeAdView.iconView as ImageView

    headline.text = nativeAd.headline.orEmpty()
    headline.visibility = if (nativeAd.headline.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    body.text = nativeAd.body.orEmpty()
    body.visibility = if (nativeAd.body.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    advertiser.text = nativeAd.advertiser.orEmpty()
    advertiser.visibility = if (nativeAd.advertiser.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    callToAction.text = nativeAd.callToAction.orEmpty()
    callToAction.visibility = if (nativeAd.callToAction.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    val nativeIcon = nativeAd.icon
    if (nativeIcon != null) {
        icon.setImageDrawable(nativeIcon.drawable)
        icon.visibility = View.VISIBLE
    } else {
        icon.setImageDrawable(null)
        icon.visibility = View.INVISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}
