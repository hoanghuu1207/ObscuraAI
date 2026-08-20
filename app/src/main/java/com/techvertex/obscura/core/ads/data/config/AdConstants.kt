package com.techvertex.obscura.core.ads.data.config

object AdConstants {
    // Sample Test AdMob App ID
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    // AdMob Sample Test Ad Unit IDs
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    // Default Fallbacks
    val DEFAULT_BANNER_ID: String
        get() = TEST_BANNER_AD_UNIT_ID

    val DEFAULT_NATIVE_ID: String
        get() = TEST_NATIVE_AD_UNIT_ID
}
