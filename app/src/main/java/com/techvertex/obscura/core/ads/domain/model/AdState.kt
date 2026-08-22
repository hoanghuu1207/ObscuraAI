package com.techvertex.obscura.core.ads.domain.model

sealed interface AdState<out T> {
    object Idle : AdState<Nothing>
    object Loading : AdState<Nothing>
    data class Success<out T>(val data: T) : AdState<T>
    data class Error(val message: String) : AdState<Nothing>
    object Disabled : AdState<Nothing>
}
