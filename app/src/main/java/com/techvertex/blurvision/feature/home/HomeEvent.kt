package com.techvertex.blurvision.feature.home

sealed interface HomeEvent {
    data object LoadItems : HomeEvent
    data object RefreshItems : HomeEvent
    data class ItemClicked(val item: String) : HomeEvent
}
