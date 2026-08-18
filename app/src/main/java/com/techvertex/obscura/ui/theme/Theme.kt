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
    backgroundGradient = listOf(Black0A0814, Black06040A),
    cardBackground = Blue131B2E,
    cardBorder = Blue2E3D5C.copy(alpha = 0.3f),
    textPrimary = Color.White,
    textSecondary = Gray94A3B8,
    topBarBg = Blue0F172A,
    bottomSheetBg = Black111726,
    bottomSheetItemBg = Blue1A2642,
    backButtonBg = Blue1E293B.copy(alpha = 0.6f),
    backButtonBorder = Gray334155,
    isDark = true
)

val LightObscuraColors = ObscuraColors(
    backgroundGradient = listOf(GrayF8FAFC, GrayF1F5F9),
    cardBackground = Color.White,
    cardBorder = GrayE2E8F0,
    textPrimary = Blue0F172A,
    textSecondary = Gray64748B,
    topBarBg = Color.White,
    bottomSheetBg = Color.White,
    bottomSheetItemBg = GrayF1F5F9,
    backButtonBg = GrayE2E8F0.copy(alpha = 0.8f),
    backButtonBorder = GrayCBD5E1,
    isDark = false
)

val LocalObscuraColors = staticCompositionLocalOf { DarkObscuraColors }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Black0A0814,
    surface = Blue131B2E
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = GrayF8FAFC,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Blue0F172A,
    onSurface = Blue0F172A
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
