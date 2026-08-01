package com.techvertex.obscura.feature.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val errorMessage: String? = null
)
