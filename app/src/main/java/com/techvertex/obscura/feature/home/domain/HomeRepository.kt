package com.techvertex.obscura.feature.home.domain

import com.techvertex.obscura.core.common.Resource
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun fetchHomeItems(): Flow<Resource<List<String>>>
}
