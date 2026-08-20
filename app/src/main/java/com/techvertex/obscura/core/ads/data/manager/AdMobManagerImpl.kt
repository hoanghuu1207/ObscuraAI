package com.techvertex.obscura.core.ads.data.manager

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.techvertex.obscura.R
import com.techvertex.obscura.core.ads.data.config.AdConstants
import com.techvertex.obscura.core.ads.data.mapper.NativeAdMapper
import com.techvertex.obscura.core.ads.domain.model.AdState
import com.techvertex.obscura.core.ads.domain.model.NativeAdData
import com.techvertex.obscura.core.ads.domain.repository.AdManager
import com.techvertex.obscura.core.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManagerImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : AdManager {

    private var isInitialized = false
    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val isRemoteAdsEnabled = MutableStateFlow(true)

    override fun initialize(context: Context) {
        if (isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                context.applicationContext,
                InitializationConfig.Builder(AdConstants.ADMOB_APP_ID).build()
            ) {
                isInitialized = true
            }

            setupRemoteConfig()
        }
    }

    private fun setupRemoteConfig() {
        try {
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val enabled = remoteConfig.getBoolean(AdConstants.KEY_IS_ADS_ENABLED)
                    isRemoteAdsEnabled.value = enabled
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isAdEnabled(): Flow<Boolean> {
        return combine(
            dataStoreManager.isPremium,
            isRemoteAdsEnabled
        ) { isPremium, isRemoteEnabled ->
            !isPremium && isRemoteEnabled
        }
    }

    override fun getBannerAdUnitId(): String {
        val fetchedId = remoteConfig.getString(AdConstants.KEY_BANNER_AD_UNIT_ID)
        return fetchedId.ifBlank { AdConstants.DEFAULT_BANNER_ID }
    }

    override fun getNativeAdUnitId(): String {
        val fetchedId = remoteConfig.getString(AdConstants.KEY_NATIVE_AD_UNIT_ID)
        return fetchedId.ifBlank { AdConstants.DEFAULT_NATIVE_ID }
    }

    override fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onResult: (AdState<NativeAdData>) -> Unit
    ) {
        val targetAdUnitId = adUnitId.ifBlank { getNativeAdUnitId() }
        onResult(AdState.Loading)

        val adRequest = NativeAdRequest
            .Builder(targetAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
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
