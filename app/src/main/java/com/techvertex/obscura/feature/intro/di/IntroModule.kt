package com.techvertex.obscura.feature.intro.di

import com.techvertex.obscura.feature.intro.data.repository.IntroRepositoryImpl
import com.techvertex.obscura.feature.intro.domain.repository.IntroRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IntroModule {

    @Binds
    @Singleton
    abstract fun bindIntroRepository(
        impl: IntroRepositoryImpl
    ): IntroRepository
}
