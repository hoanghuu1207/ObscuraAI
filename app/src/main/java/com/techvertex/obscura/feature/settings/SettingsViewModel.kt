package com.techvertex.obscura.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.obscura.core.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
)

val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("en", "English", "English"),
    AppLanguage("vi", "Tiếng Việt", "Tiếng Việt"),
    AppLanguage("de", "Deutsch", "Deutsch"),
    AppLanguage("fr", "Français", "Français"),
    AppLanguage("in", "Bahasa Indonesia", "Bahasa Indonesia"),
    AppLanguage("ja", "日本語", "日本語"),
    AppLanguage("ko", "한국어", "한국어"),
    AppLanguage("pt", "Português", "Português")
)

data class SettingsUiState(
    val selectedLanguageCode: String = "en",
    val selectedLanguageName: String = "English",
    val supportedLanguages: List<AppLanguage> = SUPPORTED_LANGUAGES,
    val selectedTheme: String = "Dark Theme",
    val showLanguageDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showAboutDialog: Boolean = false
)

sealed interface SettingsEvent {
    data class SelectLanguage(
        val languageCode: String,
        val context: Context,
        val onLanguageChanged: () -> Unit
    ) : SettingsEvent
    data class SelectTheme(val theme: String) : SettingsEvent
    data class ToggleLanguageDialog(val show: Boolean) : SettingsEvent
    data class ToggleThemeDialog(val show: Boolean) : SettingsEvent
    data class TogglePrivacyDialog(val show: Boolean) : SettingsEvent
    data class ToggleAboutDialog(val show: Boolean) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedCode = dataStoreManager.languageCode.firstOrNull() ?: "en"
            val lang = SUPPORTED_LANGUAGES.find { it.code == savedCode } ?: SUPPORTED_LANGUAGES.first()
            _uiState.update {
                it.copy(
                    selectedLanguageCode = lang.code,
                    selectedLanguageName = lang.nativeName
                )
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SelectLanguage -> {
                val lang = SUPPORTED_LANGUAGES.find { it.code == event.languageCode } ?: SUPPORTED_LANGUAGES.first()
                _uiState.update {
                    it.copy(
                        selectedLanguageCode = lang.code,
                        selectedLanguageName = lang.nativeName,
                        showLanguageDialog = false
                    )
                }
                viewModelScope.launch {
                    dataStoreManager.saveLanguageCode(lang.code)
                    applyLocale(event.context, lang.code)
                    event.onLanguageChanged()
                }
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

    private fun applyLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        }
    }
}
