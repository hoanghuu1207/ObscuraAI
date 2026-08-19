package com.techvertex.obscura.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.obscura.feature.intro.domain.usecase.CompleteIntroUseCase
import com.techvertex.obscura.feature.intro.domain.usecase.GetIntroPagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val getIntroPagesUseCase: GetIntroPagesUseCase,
    private val completeIntroUseCase: CompleteIntroUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState: StateFlow<IntroUiState> = _uiState.asStateFlow()

    init {
        loadPages()
    }

    private fun loadPages() {
        val pages = getIntroPagesUseCase()
        _uiState.update { it.copy(pages = pages) }
    }

    fun onEvent(event: IntroEvent) {
        when (event) {
            is IntroEvent.OnPageChanged -> {
                _uiState.update { it.copy(currentPage = event.pageIndex) }
            }
            is IntroEvent.OnNextClicked -> {
                val state = _uiState.value
                if (state.currentPage < state.pages.lastIndex) {
                    _uiState.update { it.copy(currentPage = state.currentPage + 1) }
                } else {
                    completeIntro(event.onCompleted)
                }
            }
            is IntroEvent.OnSkipClicked -> {
                completeIntro(event.onCompleted)
            }
        }
    }

    private fun completeIntro(onCompleted: () -> Unit) {
        viewModelScope.launch {
            completeIntroUseCase()
            _uiState.update { it.copy(isCompleted = true) }
            onCompleted()
        }
    }
}
