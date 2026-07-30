package com.techvertex.blurvision.feature.home.domain

import com.techvertex.blurvision.core.common.Resource
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun fetchHomeItems(): Flow<Resource<List<String>>>
}
