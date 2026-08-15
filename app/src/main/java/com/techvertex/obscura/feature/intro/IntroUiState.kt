package com.techvertex.obscura.feature.intro

import com.techvertex.obscura.feature.intro.domain.model.IntroPage

data class IntroUiState(
    val pages: List<IntroPage> = emptyList(),
    val currentPage: Int = 0,
    val isCompleted: Boolean = false
)
