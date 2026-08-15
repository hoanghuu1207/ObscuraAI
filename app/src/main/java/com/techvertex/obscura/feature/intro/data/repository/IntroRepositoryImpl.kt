package com.techvertex.obscura.feature.intro.data.repository

import androidx.compose.ui.graphics.Color
import com.techvertex.obscura.R
import com.techvertex.obscura.core.datastore.DataStoreManager
import com.techvertex.obscura.feature.intro.domain.model.IntroPage
import com.techvertex.obscura.feature.intro.domain.repository.IntroRepository
import javax.inject.Inject

class IntroRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : IntroRepository {

    override fun getIntroPages(): List<IntroPage> {
        return listOf(
            IntroPage(
                id = 0,
                titlePrefix = "Smart AI ",
                titleHighlight = "Face Detection",
                description = "Automatically detect and track faces in your videos frame-by-frame.",
                accentColor = Color(0xFF00E5FF), // Cyan
                imageRes = R.drawable.img_intro_ai_detect
            ),
            IntroPage(
                id = 1,
                titlePrefix = "Real-Time ",
                titleHighlight = "Effects",
                description = "Apply blur effects in real-time powered by high-performance rendering.",
                accentColor = Color(0xFF8B5CF6), // Purple
                imageRes = R.drawable.img_intro_effects
            ),
            IntroPage(
                id = 2,
                titlePrefix = "100% On-Device ",
                titleHighlight = "Privacy",
                description = "All video processing stays locally on your device. Your media is never uploaded to any external server.",
                accentColor = Color(0xFFEC4899), // Pink
                imageRes = R.drawable.img_intro_privacy
            )
        )
    }

    override suspend fun completeIntro() {
        dataStoreManager.setPassIntro(true)
    }
}
