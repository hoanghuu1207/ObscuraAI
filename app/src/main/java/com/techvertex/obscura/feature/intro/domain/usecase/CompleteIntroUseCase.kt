package com.techvertex.obscura.feature.intro.domain.usecase

import com.techvertex.obscura.feature.intro.domain.repository.IntroRepository
import javax.inject.Inject

class CompleteIntroUseCase @Inject constructor(
    private val repository: IntroRepository
) {
    suspend operator fun invoke() {
        repository.completeIntro()
    }
}
