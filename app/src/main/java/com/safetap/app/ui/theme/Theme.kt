package com.safetap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightEmergencyScheme = lightColorScheme(
    primary = EmergencyRed,
    onPrimary = EmergencyWhite,
    primaryContainer = EmergencyRedContainer,
    onPrimaryContainer = EmergencyOnRedContainer,
    secondary = EmergencyRedDark,
    onSecondary = EmergencyWhite,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = SafeGreen,
    onTertiary = EmergencyWhite,
    tertiaryContainer = SafeGreenContainer,
    onTertiaryContainer = SafeOnGreenContainer,
    background = EmergencySurface,
    onBackground = EmergencyOnSurface,
    surface = EmergencyWhite,
    onSurface = EmergencyOnSurface,
    surfaceVariant = EmergencySurfaceVariant,
    onSurfaceVariant = EmergencyOnSurfaceVariant,
    outline = EmergencyOutline,
    outlineVariant = EmergencyOutlineVariant,
    error = EmergencyRed,
    onError = EmergencyWhite
)

private val DarkEmergencyScheme = darkColorScheme(
    primary = EmergencyRedLight,
    onPrimary = Color(0xFF410002),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF690005),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = SafeGreenLight,
    onTertiary = Color(0xFF00390B),
    tertiaryContainer = Color(0xFF005315),
    onTertiaryContainer = SafeGreenContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = DarkOutline,
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun SafeTapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkEmergencyScheme else LightEmergencyScheme,
        typography = Typography,
        content = content
    )
}
