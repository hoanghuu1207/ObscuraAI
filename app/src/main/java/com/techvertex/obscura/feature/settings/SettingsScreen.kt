package com.techvertex.obscura.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techvertex.obscura.BuildConfig
import com.techvertex.obscura.R
import com.techvertex.obscura.ui.theme.Black06040A
import com.techvertex.obscura.ui.theme.Black0A0814
import com.techvertex.obscura.ui.theme.Blue00E5FF
import com.techvertex.obscura.ui.theme.Blue131B2E
import com.techvertex.obscura.ui.theme.Blue1E293B
import com.techvertex.obscura.ui.theme.Blue2E3D5C
import com.techvertex.obscura.ui.theme.Gray334155
import com.techvertex.obscura.ui.theme.Gray94A3B8
import com.techvertex.obscura.ui.theme.Purple6366F1
import com.techvertex.obscura.ui.theme.Purple8B5CF6

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLanguageChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cyanColor = Blue00E5FF
    val purpleColor = Purple8B5CF6
    val containerBg = Blue131B2E
    val containerBorder = Blue2E3D5C.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Black0A0814,
                        Black06040A
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Blue1E293B.copy(alpha = 0.6f))
                        .border(1.dp, Gray334155, CircleShape)
                        .clickable { onNavigateBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_settings).uppercase(),
                        color = purpleColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(containerBg)
                            .border(1.dp, containerBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column {
                            SettingRowItem(
                                icon = painterResource(R.drawable.ic_language),
                                iconColor = cyanColor,
                                iconBgColor = cyanColor.copy(alpha = 0.15f),
                                title = stringResource(R.string.language),
                                subtitle = uiState.selectedLanguageName,
                                onClick = {
                                    viewModel.onEvent(
                                        SettingsEvent.ToggleLanguageDialog(
                                            true
                                        )
                                    )
                                }
                            )

                            HorizontalDivider(
                                color = containerBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            SettingRowItem(
                                icon = painterResource(R.drawable.ic_themes),
                                iconColor = purpleColor,
                                iconBgColor = purpleColor.copy(alpha = 0.15f),
                                title = stringResource(R.string.select_theme),
                                subtitle = uiState.selectedTheme,
                                onClick = { viewModel.onEvent(SettingsEvent.ToggleThemeDialog(true)) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(R.string.information_support).uppercase(),
                        color = purpleColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(containerBg)
                            .border(1.dp, containerBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column {
                            SettingRowItem(
                                icon = painterResource(R.drawable.ic_privacy_policy),
                                iconColor = cyanColor,
                                iconBgColor = cyanColor.copy(alpha = 0.15f),
                                title = stringResource(R.string.privacy_policy),
                                subtitle = stringResource(R.string.read_data_protection_policy),
                                onClick = { viewModel.onEvent(SettingsEvent.TogglePrivacyDialog(true)) }
                            )

                            HorizontalDivider(
                                color = containerBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            SettingRowItem(
                                icon = painterResource(R.drawable.ic_info),
                                iconColor = purpleColor,
                                iconBgColor = purpleColor.copy(alpha = 0.15f),
                                title = stringResource(R.string.about),
                                subtitle = stringResource(
                                    R.string.obscura_v,
                                    BuildConfig.VERSION_NAME
                                ),
                                onClick = { viewModel.onEvent(SettingsEvent.ToggleAboutDialog(true)) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append("OBSCURA ")
                        }
                        withStyle(SpanStyle(color = cyanColor, fontWeight = FontWeight.Bold)) {
                            append("AI")
                        }
                    },
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.secure_local_face_blur),
                    color = Gray94A3B8,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(R.string.select_language),
            languages = uiState.supportedLanguages,
            selectedCode = uiState.selectedLanguageCode,
            onLanguageSelected = { langCode ->
                viewModel.onEvent(
                    SettingsEvent.SelectLanguage(
                        languageCode = langCode,
                        context = context,
                        onLanguageChanged = onLanguageChanged
                    )
                )
            },
            onDismiss = {
                viewModel.onEvent(SettingsEvent.ToggleLanguageDialog(false))
            }
        )
    }

    if (uiState.showThemeDialog) {
        OptionSelectionDialog(
            title = stringResource(R.string.select_theme),
            options = listOf(
                stringResource(R.string.dark_theme),
                stringResource(R.string.light_theme),
                stringResource(R.string.system_default)
            ),
            selectedOption = uiState.selectedTheme,
            onOptionSelected = { theme ->
                viewModel.onEvent(SettingsEvent.SelectTheme(theme))
            },
            onDismiss = {
                viewModel.onEvent(SettingsEvent.ToggleThemeDialog(false))
            }
        )
    }

    if (uiState.showPrivacyDialog) {
        InfoDialog(
            title = stringResource(R.string.privacy_policy),
            content = stringResource(R.string.obscura_values_user_privacy_all_video_processing_and_blur_effects_are_executed_locally_on_your_device_no_personal_media_or_data_is_uploaded_to_external_servers),
            onDismiss = {
                viewModel.onEvent(SettingsEvent.TogglePrivacyDialog(false))
            }
        )
    }

    if (uiState.showAboutDialog) {
        InfoDialog(
            title = stringResource(R.string.about_obscura),
            content = stringResource(
                R.string.app_version_developer_techvertex,
                stringResource(R.string.app_name),
                BuildConfig.VERSION_NAME
            ),
            onDismiss = {
                viewModel.onEvent(SettingsEvent.ToggleAboutDialog(false))
            }
        )
    }
}

@Composable
private fun SettingRowItem(
    icon: Painter,
    iconColor: Color,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Gray94A3B8,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Gray94A3B8.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    title: String,
    languages: List<AppLanguage>,
    selectedCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Blue1E293B,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(languages.size) { index ->
                        val lang = languages[index]
                        val isSelected = (lang.code == selectedCode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onLanguageSelected(lang.code) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onLanguageSelected(lang.code) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Purple6366F1,
                                    unselectedColor = Gray94A3B8
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (lang.displayName != lang.nativeName) {
                                    Text(
                                        text = lang.displayName,
                                        color = Gray94A3B8,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Purple6366F1,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Blue1E293B,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Purple6366F1,
                                unselectedColor = Gray94A3B8
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Purple6366F1,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Blue1E293B,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = content,
                    color = Gray94A3B8,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Card(
                        onClick = onDismiss,
                        colors = CardDefaults.cardColors(containerColor = Purple6366F1),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.close),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
