package com.techvertex.obscura.feature.intro.domain.usecase

import com.techvertex.obscura.feature.intro.domain.model.IntroPage
import com.techvertex.obscura.feature.intro.domain.repository.IntroRepository
import javax.inject.Inject

class GetIntroPagesUseCase @Inject constructor(
    private val repository: IntroRepository
) {
    operator fun invoke(): List<IntroPage> {
        return repository.getIntroPages()
    }
}
