package com.techvertex.obscura.core.ads.domain.repository

import android.content.Context
import com.techvertex.obscura.core.ads.domain.model.AdState
import com.techvertex.obscura.core.ads.domain.model.NativeAdData
import kotlinx.coroutines.flow.Flow

interface AdManager {
    fun initialize(context: Context)
    fun isAdEnabled(): Flow<Boolean>
    fun getBannerAdUnitId(): String
    fun getNativeAdUnitId(): String
    fun loadNativeAd(
        context: Context,
        adUnitId: String = getNativeAdUnitId(),
        onResult: (AdState<NativeAdData>) -> Unit
    )
}
