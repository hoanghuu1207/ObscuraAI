package com.techvertex.obscura.core.ads.domain.model

data class NativeAdData(
    val headline: String?,
    val body: String?,
    val callToAction: String?,
    val iconUrl: String?,
    val advertiser: String?,
    val starRating: Double?,
    val store: String?,
    val price: String?,
    val rawNativeAd: Any? = null
)
