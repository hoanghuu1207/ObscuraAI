package com.techvertex.obscura.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techvertex.obscura.BuildConfig
import com.techvertex.obscura.R
import com.techvertex.obscura.ui.theme.Blue0F172A
import com.techvertex.obscura.ui.theme.Blue1E293B
import com.techvertex.obscura.ui.theme.Gray334155
import com.techvertex.obscura.ui.theme.Gray94A3B8
import com.techvertex.obscura.ui.theme.Purple6366F1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLanguageChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue0F172A
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Blue0F172A,
                            Blue1E293B
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "App Settings",
                        color = Gray94A3B8,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                item {
                    SettingItemCard(
                        icon = Icons.Default.AccountCircle,
                        title = "Language",
                        subtitle = uiState.selectedLanguageName,
                        onClick = { viewModel.onEvent(SettingsEvent.ToggleLanguageDialog(true)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    SettingItemCard(
                        icon = Icons.Default.DateRange,
                        title = "Theme",
                        subtitle = uiState.selectedTheme,
                        onClick = { viewModel.onEvent(SettingsEvent.ToggleThemeDialog(true)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Text(
                        text = "Information & Support",
                        color = Gray94A3B8,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    )
                }

                item {
                    SettingItemCard(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy",
                        subtitle = "Read data protection policy",
                        onClick = { viewModel.onEvent(SettingsEvent.TogglePrivacyDialog(true)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    SettingItemCard(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Obscura v${BuildConfig.VERSION_NAME}",
                        onClick = { viewModel.onEvent(SettingsEvent.ToggleAboutDialog(true)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // Language Selection Dialog
    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            title = "Select Language",
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

    // Theme Selection Dialog
    if (uiState.showThemeDialog) {
        OptionSelectionDialog(
            title = "Select Theme",
            options = listOf("Dark Theme", "Light Theme", "System Default"),
            selectedOption = uiState.selectedTheme,
            onOptionSelected = { theme ->
                viewModel.onEvent(SettingsEvent.SelectTheme(theme))
            },
            onDismiss = {
                viewModel.onEvent(SettingsEvent.ToggleThemeDialog(false))
            }
        )
    }

    // Privacy Policy Dialog
    if (uiState.showPrivacyDialog) {
        InfoDialog(
            title = "Privacy Policy",
            content = "Obscura values user privacy. All video processing and GPU OpenGL blur effects are executed locally on your device. No personal media or data is uploaded to external servers.",
            onDismiss = {
                viewModel.onEvent(SettingsEvent.TogglePrivacyDialog(false))
            }
        )
    }

    // About Dialog
    if (uiState.showAboutDialog) {
        InfoDialog(
            title = "About Obscura",
            content = "${stringResource(R.string.app_name)} App\nVersion: ${BuildConfig.VERSION_NAME}\nDeveloper: TechVertex\nArchitecture: Clean Architecture + MVI + Jetpack Compose + OpenGL ES",
            onDismiss = {
                viewModel.onEvent(SettingsEvent.ToggleAboutDialog(false))
            }
        )
    }
}

@Composable
private fun SettingItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Gray334155.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Purple6366F1.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Purple6366F1,
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
                    fontWeight = FontWeight.SemiBold
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
                tint = Gray94A3B8
            )
        }
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
                            text = "Cancel",
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
                            text = "Cancel",
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
                                text = "Close",
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
