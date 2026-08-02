package com.techvertex.obscura

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.techvertex.obscura.core.datastore.DataStoreManager
import com.techvertex.obscura.navigation.AppNavHost
import com.techvertex.obscura.ui.theme.ObscuraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val savedCode = dataStoreManager.languageCode.firstOrNull() ?: "en"
            applyLocale(savedCode)
        }

        enableEdgeToEdge()
        setContent {
            ObscuraTheme {
                AppNavHost()
            }
        }
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        }
    }
}
