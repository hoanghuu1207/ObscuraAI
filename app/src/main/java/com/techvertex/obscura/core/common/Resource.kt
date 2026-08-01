package com.techvertex.obscura.core.common

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val exception: Throwable, val message: String? = exception.message) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
