package com.techvertex.obscura.feature.intro.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

data class IntroPage(
    val id: Int,
    val titlePrefix: String,
    val titleHighlight: String,
    val description: String,
    val accentColor: Color,
    @DrawableRes val imageRes: Int
)
