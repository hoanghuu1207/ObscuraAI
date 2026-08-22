package com.techvertex.obscura.core.ads.data.mapper

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.techvertex.obscura.core.ads.domain.model.NativeAdData

object NativeAdMapper {
    fun mapToDomain(nativeAd: NativeAd): NativeAdData {
        return NativeAdData(
            headline = nativeAd.headline,
            body = nativeAd.body,
            callToAction = nativeAd.callToAction,
            iconUrl = nativeAd.icon?.uri?.toString(),
            advertiser = nativeAd.advertiser,
            starRating = nativeAd.starRating,
            store = nativeAd.store,
            price = nativeAd.price,
            rawNativeAd = nativeAd
        )
    }
}
