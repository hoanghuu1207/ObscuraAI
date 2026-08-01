package com.techvertex.obscura.feature.home.data

import com.techvertex.obscura.core.common.Resource
import com.techvertex.obscura.feature.home.domain.HomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor() : HomeRepository {
    override fun fetchHomeItems(): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading)
        try {
            delay(1500)
            val mockData = listOf(
                "Premium Glassmorphism Design",
                "Advanced Blur Shader Effects",
                "Jetpack Compose UI Toolkit",
                "Hilt Dependency Injection",
                "Unidirectional Data Flow (MVI)"
            )
            emit(Resource.Success(mockData))
        } catch (e: Exception) {
            emit(Resource.Error(e))
        }
    }
}
