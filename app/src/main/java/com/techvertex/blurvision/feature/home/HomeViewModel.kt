package com.techvertex.blurvision.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.blurvision.core.common.Resource
import com.techvertex.blurvision.feature.home.domain.usecase.GetHomeItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeItemsUseCase: GetHomeItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        onEvent(HomeEvent.LoadItems)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadItems -> fetchItems()
            is HomeEvent.RefreshItems -> fetchItems()
            is HomeEvent.ItemClicked -> {
                // Handle item click action
            }
        }
    }

    private fun fetchItems() {
        viewModelScope.launch(Dispatchers.IO) {
            getHomeItemsUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, items = resource.data) }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
        }
    }
}
