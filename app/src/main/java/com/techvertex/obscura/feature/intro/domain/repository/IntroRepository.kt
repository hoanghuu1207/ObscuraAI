package com.techvertex.obscura.feature.intro.domain.repository

import com.techvertex.obscura.feature.intro.domain.model.IntroPage

interface IntroRepository {
    fun getIntroPages(): List<IntroPage>
    suspend fun completeIntro()
}
