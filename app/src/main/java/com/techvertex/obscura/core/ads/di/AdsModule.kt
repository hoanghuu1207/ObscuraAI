package com.techvertex.obscura.core.ads.di

import com.techvertex.obscura.core.ads.data.manager.AdMobManagerImpl
import com.techvertex.obscura.core.ads.domain.repository.AdManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindAdManager(
        adMobManagerImpl: AdMobManagerImpl
    ): AdManager
}
