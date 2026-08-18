package com.techvertex.obscura.ui.theme

import DARK
import LIGHT
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class ObscuraColors(
    val backgroundGradient: List<Color>,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val topBarBg: Color,
    val bottomSheetBg: Color,
    val bottomSheetItemBg: Color,
    val backButtonBg: Color,
    val backButtonBorder: Color,
    val isDark: Boolean
)

val DarkObscuraColors = ObscuraColors(
    backgroundGradient = listOf(Color(0xFF0A0814), Color(0xFF06040A)),
    cardBackground = Color(0xFF131B2E),
    cardBorder = Color(0xFF2E3D5C).copy(alpha = 0.3f),
    textPrimary = Color.White,
    textSecondary = Color(0xFF94A3B8),
    topBarBg = Color(0xFF0F172A),
    bottomSheetBg = Color(0xFF111726),
    bottomSheetItemBg = Color(0xFF1A2642),
    backButtonBg = Color(0xFF1E293B).copy(alpha = 0.6f),
    backButtonBorder = Color(0xFF334155),
    isDark = true
)

val LightObscuraColors = ObscuraColors(
    backgroundGradient = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
    cardBackground = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    topBarBg = Color(0xFFFFFFFF),
    bottomSheetBg = Color(0xFFFFFFFF),
    bottomSheetItemBg = Color(0xFFF1F5F9),
    backButtonBg = Color(0xFFE2E8F0).copy(alpha = 0.8f),
    backButtonBorder = Color(0xFFCBD5E1),
    isDark = false
)

val LocalObscuraColors = staticCompositionLocalOf { DarkObscuraColors }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF0A0814),
    surface = Color(0xFF131B2E)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun ObscuraTheme(
    themeMode: String = DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        DARK -> true
        LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    val obscuraColors = if (isDark) DarkObscuraColors else LightObscuraColors

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalObscuraColors provides obscuraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object ObscuraCustomTheme {
    val colors: ObscuraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalObscuraColors.current
}
