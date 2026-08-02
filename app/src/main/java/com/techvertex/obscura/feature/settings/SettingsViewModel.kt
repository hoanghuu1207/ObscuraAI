package com.techvertex.obscura.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val selectedLanguage: String = "English",
    val selectedTheme: String = "Dark Theme",
    val showLanguageDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showAboutDialog: Boolean = false
)

sealed interface SettingsEvent {
    data class SelectLanguage(val language: String) : SettingsEvent
    data class SelectTheme(val theme: String) : SettingsEvent
    data class ToggleLanguageDialog(val show: Boolean) : SettingsEvent
    data class ToggleThemeDialog(val show: Boolean) : SettingsEvent
    data class TogglePrivacyDialog(val show: Boolean) : SettingsEvent
    data class ToggleAboutDialog(val show: Boolean) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SelectLanguage -> {
                _uiState.update { it.copy(selectedLanguage = event.language, showLanguageDialog = false) }
            }
            is SettingsEvent.SelectTheme -> {
                _uiState.update { it.copy(selectedTheme = event.theme, showThemeDialog = false) }
            }
            is SettingsEvent.ToggleLanguageDialog -> {
                _uiState.update { it.copy(showLanguageDialog = event.show) }
            }
            is SettingsEvent.ToggleThemeDialog -> {
                _uiState.update { it.copy(showThemeDialog = event.show) }
            }
            is SettingsEvent.TogglePrivacyDialog -> {
                _uiState.update { it.copy(showPrivacyDialog = event.show) }
            }
            is SettingsEvent.ToggleAboutDialog -> {
                _uiState.update { it.copy(showAboutDialog = event.show) }
            }
        }
    }
}
