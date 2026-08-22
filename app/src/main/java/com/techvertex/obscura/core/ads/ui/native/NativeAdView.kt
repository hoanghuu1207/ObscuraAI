package com.techvertex.obscura.core.ads.ui.native

import android.text.TextUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.techvertex.obscura.core.ads.data.config.AdConstants
import com.techvertex.obscura.core.ads.domain.model.AdState
import com.techvertex.obscura.core.ads.domain.model.NativeAdData
import com.techvertex.obscura.core.ads.domain.repository.AdManager
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView as GoogleNativeAdView

@Composable
fun NativeAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    adUnitId: String = adManager.getNativeAdUnitId(),
    showMediaContent: Boolean = false
) {
    val isAdEnabled by adManager.isAdEnabled().collectAsState(initial = true)
    if (!isAdEnabled) return

    val context = LocalContext.current
    var adState by remember { mutableStateOf<AdState<NativeAdData>>(AdState.Loading) }

    LaunchedEffect(adUnitId) {
        adManager.loadNativeAd(context, adUnitId) { result ->
            adState = result
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (adState as? AdState.Success)?.data?.rawNativeAd?.let { raw ->
                (raw as? NativeAd)?.destroy()
            }
        }
    }

    when (val state = adState) {
        is AdState.Success -> {
            NativeAdCard(
                adData = state.data,
                showMediaContent = showMediaContent,
                modifier = modifier
            )
        }

        is AdState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF1E1E2C), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        else -> {
            //
        }
    }
}

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adData: NativeAdData,
    showMediaContent: Boolean = false,
) {
    val rawNativeAd = adData.rawNativeAd as? NativeAd ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2C)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            factory = { context ->
                val nativeAdView = GoogleNativeAdView(context)

                val container = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }

                val headerRow = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                }

                val iconView = ImageView(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(120, 120)
                }
                nativeAdView.iconView = iconView

                val titleCol = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(24, 0, 0, 0)
                }

                val headlineView = TextView(context).apply {
                    textSize = 16f
                    setTextColor(android.graphics.Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                nativeAdView.headlineView = headlineView

                val bodyView = TextView(context).apply {
                    textSize = 13f
                    setTextColor(android.graphics.Color.LTGRAY)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                }
                nativeAdView.bodyView = bodyView

                titleCol.addView(headlineView)
                titleCol.addView(bodyView)

                headerRow.addView(iconView)
                headerRow.addView(titleCol)

                container.addView(headerRow)

                var mediaView: MediaView? = null
                if (showMediaContent) {
                    mediaView = MediaView(context).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            450
                        ).apply {
                            setMargins(0, 16, 0, 16)
                        }
                    }
                    container.addView(mediaView)
                }

                val ctaButton = Button(context).apply {
                    setBackgroundColor("#00E5FF".toColorInt())
                    setTextColor(android.graphics.Color.BLACK)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (!showMediaContent) {
                            setMargins(0, 16, 0, 0)
                        }
                    }
                }
                nativeAdView.callToActionView = ctaButton
                container.addView(ctaButton)

                nativeAdView.tag = mediaView
                nativeAdView.addView(container)
                nativeAdView
            },
            update = { googleNativeAdView ->
                (googleNativeAdView.headlineView as? TextView)?.text = rawNativeAd.headline
                (googleNativeAdView.bodyView as? TextView)?.text = rawNativeAd.body
                (googleNativeAdView.callToActionView as? Button)?.text = rawNativeAd.callToAction

                rawNativeAd.icon?.drawable?.let {
                    (googleNativeAdView.iconView as? ImageView)?.setImageDrawable(it)
                }

                val mediaView = googleNativeAdView.tag as? MediaView
                googleNativeAdView.registerNativeAd(rawNativeAd, mediaView)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
