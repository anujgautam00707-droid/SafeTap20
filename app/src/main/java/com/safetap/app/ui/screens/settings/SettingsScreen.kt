package com.safetap.app.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetap.app.R
import com.safetap.app.di.SafeTapViewModelFactory
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.WarningAmber

private data class SafeTapLanguage(
    val code: String,
    val displayName: String
)

private val supportedLanguages = listOf(
    SafeTapLanguage(code = "en", displayName = "English (US)"),
    SafeTapLanguage(code = "hi", displayName = "हिन्दी (Hindi)"),
    SafeTapLanguage(code = "es", displayName = "Español"),
    SafeTapLanguage(code = "fr", displayName = "Français"),
    SafeTapLanguage(code = "de", displayName = "Deutsch")
)

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = SafeTapViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Local UI-only state settings
    var isDarkMode by remember { mutableStateOf(false) }
    var autoShareGps by remember { mutableStateOf(true) }
    var audioSirenOnSos by remember { mutableStateOf(true) }
    var emergencyNumber by remember { mutableStateOf("911") }

    // Dialog controllers
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEmergencyNumberDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    var tempEmergencyNumber by remember { mutableStateOf(emergencyNumber) }

    // Read current application locale
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val activeLangCode = if (!currentLocales.isEmpty) {
        currentLocales.get(0)?.language ?: "en"
    } else {
        "en"
    }
    val activeLanguage = supportedLanguages.find { it.code == activeLangCode } ?: supportedLanguages.first()

    // Sign Out Confirmation Dialog
    if (uiState.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowSignOutDialog(false) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.sign_out_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.sign_out_dialog_message))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onSignOut(onLoggedOut) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text(stringResource(R.string.sign_out), color = EmergencyWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowSignOutDialog(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    supportedLanguages.forEach { lang ->
                        val isSelected = activeLanguage.code == lang.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val appLocale = LocaleListCompat.forLanguageTags(lang.code)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    val appLocale = LocaleListCompat.forLanguageTags(lang.code)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Emergency Number Config Dialog
    if (showEmergencyNumberDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyNumberDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    tint = EmergencyRed
                )
            },
            title = { Text(stringResource(R.string.emergency_dispatch_number), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.emergency_number_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempEmergencyNumber,
                        onValueChange = { tempEmergencyNumber = it },
                        label = { Text(stringResource(R.string.emergency_number_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempEmergencyNumber.isNotBlank()) {
                            emergencyNumber = tempEmergencyNumber.trim()
                        }
                        showEmergencyNumberDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyNumberDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text(stringResource(R.string.about_safetap), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.about_safetap_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.about_safetap_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_safetap_copyright),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }

    // Privacy & Security Info Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = SafeGreen
                )
            },
            title = { Text(stringResource(R.string.privacy_encryption), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.privacy_bullet_1),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(R.string.privacy_bullet_2),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(R.string.privacy_bullet_3),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.understood))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar Initials
                val userInitial = remember(uiState.userEmail) {
                    uiState.userEmail.firstOrNull()?.uppercase() ?: "U"
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.userEmail.ifBlank { stringResource(R.string.default_user_name) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SafeGreenContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pro_active),
                                color = SafeGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.safeguard_on),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Emergency Preferences
        SettingsSectionHeader(stringResource(R.string.section_emergency_configuration))

        SettingsCardGroup {
            SettingsClickableRow(
                icon = Icons.Filled.Call,
                iconTint = EmergencyRed,
                title = stringResource(R.string.emergency_dispatch_number),
                subtitle = stringResource(R.string.emergency_dispatch_subtitle, emergencyNumber),
                onClick = {
                    tempEmergencyNumber = emergencyNumber
                    showEmergencyNumberDialog = true
                }
            )

            SettingsDivider()

            SettingsToggleRow(
                icon = Icons.Filled.LocationOn,
                iconTint = SafeGreen,
                title = stringResource(R.string.auto_share_live_gps),
                subtitle = stringResource(R.string.auto_share_live_gps_desc),
                checked = autoShareGps,
                onCheckedChange = { autoShareGps = it }
            )

            SettingsDivider()

            SettingsToggleRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconTint = WarningAmber,
                title = stringResource(R.string.audible_alarm_siren),
                subtitle = stringResource(R.string.audible_alarm_siren_desc),
                checked = audioSirenOnSos,
                onCheckedChange = { audioSirenOnSos = it }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Appearance & Localization
        SettingsSectionHeader(stringResource(R.string.section_app_preferences))

        SettingsCardGroup {
            SettingsToggleRow(
                icon = Icons.Filled.DarkMode,
                iconTint = Color(0xFF6366F1),
                title = stringResource(R.string.dark_theme),
                subtitle = stringResource(R.string.dark_theme_desc),
                checked = isDarkMode,
                onCheckedChange = { isDarkMode = it }
            )

            SettingsDivider()

            SettingsClickableRow(
                icon = Icons.Filled.Language,
                iconTint = Color(0xFF0EA5E9),
                title = stringResource(R.string.app_language),
                subtitle = activeLanguage.displayName,
                onClick = { showLanguageDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: About & Security
        SettingsSectionHeader(stringResource(R.string.section_about_security))

        SettingsCardGroup {
            SettingsClickableRow(
                icon = Icons.Filled.Shield,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.about_safetap),
                subtitle = stringResource(R.string.about_safetap_version),
                onClick = { showAboutDialog = true }
            )

            SettingsDivider()

            SettingsClickableRow(
                icon = Icons.Outlined.Policy,
                iconTint = Color(0xFF10B981),
                title = stringResource(R.string.privacy_encryption),
                subtitle = stringResource(R.string.privacy_encryption_desc),
                onClick = { showPrivacyDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        OutlinedButton(
            onClick = { viewModel.onShowSignOutDialog(true) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EmergencyRed
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(EmergencyRed.copy(alpha = 0.5f))
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = EmergencyRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sign_out),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = EmergencyRed
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCardGroup(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmergencyWhite,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
