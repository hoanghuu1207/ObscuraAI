package com.techvertex.obscura.core.ads.data.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
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

const val TAG_REWARDED = "RewardedAd"

@Singleton
class AdMobManagerImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : AdManager {

    private var isInitialized = false
    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val isRemoteAdsEnabled = MutableStateFlow(true)

    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewardedAd = false

    override fun initialize(context: Context) {
        if (isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                context.applicationContext,
                InitializationConfig.Builder(AdConstants.ADMOB_APP_ID).build()
            ) {
                isInitialized = true
                preloadRewardedAd(context.applicationContext)
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

    override fun getRewardedAdUnitId(): String {
        val fetchedId = remoteConfig.getString(AdConstants.KEY_REWARDED_AD_UNIT_ID)
        return fetchedId.ifBlank { AdConstants.DEFAULT_REWARDED_ID }
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

    override fun preloadRewardedAd(context: Context, adUnitId: String) {
        if (rewardedAd != null || isLoadingRewardedAd) return

        val targetAdUnitId = adUnitId.ifBlank { getRewardedAdUnitId() }
        isLoadingRewardedAd = true
        Log.d(TAG_REWARDED, "Preloading Rewarded Ad with Unit ID: $targetAdUnitId")

        val request = AdRequest.Builder(targetAdUnitId).build()
        RewardedAd.load(
            request,
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewardedAd = false
                    Log.d(TAG_REWARDED, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewardedAd = false
                    Log.e(TAG_REWARDED, "Rewarded ad failed to load: ${adError.message}")
                }
            }
        )
    }

    override fun showRewardedAd(
        activity: Activity,
        onAdDismissedOrCompleted: () -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            Log.d(TAG_REWARDED, "Rewarded ad is null/failed to load. Proceeding directly.")
            preloadRewardedAd(activity.applicationContext)
            onAdDismissedOrCompleted()
            return
        }

        currentAd.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG_REWARDED, "Rewarded ad dismissed.")
                rewardedAd = null
                preloadRewardedAd(activity.applicationContext)
                onAdDismissedOrCompleted()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.e(TAG_REWARDED, "Rewarded ad failed to show: ${fullScreenContentError.message}")
                rewardedAd = null
                preloadRewardedAd(activity.applicationContext)
                onAdDismissedOrCompleted()
            }
        }

        currentAd.show(activity) { rewardItem ->
            Log.d(TAG_REWARDED, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }
}
