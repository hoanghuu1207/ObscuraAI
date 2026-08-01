package com.techvertex.obscura.feature.home.domain.usecase

import com.techvertex.obscura.core.common.Resource
import com.techvertex.obscura.feature.home.domain.HomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHomeItemsUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    operator fun invoke(): Flow<Resource<List<String>>> {
        return repository.fetchHomeItems()
    }
}
