package com.techvertex.obscura.core.datastore

import SYSTEM
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_USER_TOKEN = stringPreferencesKey("user_token")
        val KEY_IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val KEY_IS_PASS_INTRO = booleanPreferencesKey("is_pass_intro")
        val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
    }

    /**
     * Read a Boolean preference as a Flow
     */
    fun getBoolean(key: Preferences.Key<Boolean>, defaultValue: Boolean = false): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }

    /**
     * Save a Boolean preference
     */
    suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Read a String preference as a Flow
     */
    fun getString(key: Preferences.Key<String>, defaultValue: String? = null): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }

    /**
     * Save a String preference
     */
    suspend fun saveString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Read an Int preference as a Flow
     */
    fun getInt(key: Preferences.Key<Int>, defaultValue: Int = 0): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }

    /**
     * Save an Int preference
     */
    suspend fun saveInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Shortcut Properties & Methods
    val isDarkMode: Flow<Boolean> = getBoolean(KEY_IS_DARK_MODE, false)

    suspend fun setDarkMode(enabled: Boolean) {
        saveBoolean(KEY_IS_DARK_MODE, enabled)
    }

    val themeMode: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_THEME_MODE] ?: SYSTEM
        }

    suspend fun saveThemeMode(mode: String) {
        saveString(KEY_THEME_MODE, mode)
    }

    val userToken: Flow<String?> = getString(KEY_USER_TOKEN)

    suspend fun saveUserToken(token: String) {
        saveString(KEY_USER_TOKEN, token)
    }

    suspend fun clearUserToken() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_USER_TOKEN)
        }
    }

    /**
     * Clear all preferences saved in DataStore
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val isPassIntro: Flow<Boolean> = getBoolean(KEY_IS_PASS_INTRO, false)

    suspend fun setPassIntro(value: Boolean) {
        saveBoolean(KEY_IS_PASS_INTRO, value)
    }

    val languageCode: Flow<String?> = getString(KEY_LANGUAGE_CODE, "en")

    suspend fun saveLanguageCode(code: String) {
        saveString(KEY_LANGUAGE_CODE, code)
    }

    val isPremium: Flow<Boolean> = getBoolean(KEY_IS_PREMIUM, false)

    suspend fun setIsPremium(value: Boolean) {
        saveBoolean(KEY_IS_PREMIUM, value)
    }
}
