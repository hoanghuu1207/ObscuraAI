package com.techvertex.obscura.core.ads.data.manager

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.techvertex.obscura.core.ads.data.config.AdConstants
import com.techvertex.obscura.core.ads.data.mapper.NativeAdMapper
import com.techvertex.obscura.core.ads.domain.model.AdState
import com.techvertex.obscura.core.ads.domain.model.NativeAdData
import com.techvertex.obscura.core.ads.domain.repository.AdManager
import com.techvertex.obscura.core.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManagerImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : AdManager {

    private var isInitialized = false

    override fun initialize(context: Context) {
        if (isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                context.applicationContext,
                InitializationConfig.Builder(AdConstants.ADMOB_APP_ID).build()
            ) {
                isInitialized = true
            }
        }
    }

    override fun isAdEnabled(): Flow<Boolean> {
        return dataStoreManager.isPremium.map { isPremium -> !isPremium }
    }

    override fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onResult: (AdState<NativeAdData>) -> Unit
    ) {
        onResult(AdState.Loading)

        val adRequest = NativeAdRequest
            .Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                val domainModel = NativeAdMapper.mapToDomain(nativeAd)
                onResult(AdState.Success(domainModel))
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                onResult(AdState.Error(adError.message))
            }
        }

        NativeAdLoader.load(adRequest, adCallback)
    }
}
