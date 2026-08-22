package com.techvertex.obscura.core.ads.ui.banner

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.techvertex.obscura.core.ads.domain.repository.AdManager

const val TAG = "BannerAd"

@Composable
fun BannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    adUnitId: String = adManager.getBannerAdUnitId()
) {
    val isAdEnabled by adManager.isAdEnabled().collectAsState(initial = true)
    if (!isAdEnabled) return

    val isPreviewMode = LocalInspectionMode.current
    if (isPreviewMode) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                val adSize = AdSize.BANNER
                val request = BannerAdRequest.Builder(adUnitId, adSize).build()
                loadAd(
                    request,
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            Log.d(TAG, "onAdLoaded: success")
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d(TAG, "onAdFailedToLoad: failed")
                        }
                    }
                )
            }
        }
    )
}
