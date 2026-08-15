package com.techvertex.obscura.feature.intro

sealed interface IntroEvent {
    data class OnPageChanged(val pageIndex: Int) : IntroEvent
    data class OnNextClicked(val onCompleted: () -> Unit) : IntroEvent
    data class OnSkipClicked(val onCompleted: () -> Unit) : IntroEvent
}
