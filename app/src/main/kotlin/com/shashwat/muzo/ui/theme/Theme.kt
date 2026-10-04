package com.shashwat.muzo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.shashwat.muzo.data.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = MuzoCyan,
    onPrimary = DarkBackground,
    primaryContainer = MuzoPurple,
    onPrimaryContainer = TextPrimary,
    secondary = MuzoPink,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

private val AmoledColorScheme = darkColorScheme(
    primary = MuzoCyan,
    onPrimary = AmoledBackground,
    primaryContainer = MuzoPurple,
    onPrimaryContainer = TextPrimary,
    secondary = MuzoPink,
    onSecondary = TextPrimary,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledBackground,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = MuzoPurple,
    onPrimary = LightSurface,
    primaryContainer = MuzoCyan,
    onPrimaryContainer = LightTextPrimary,
    secondary = MuzoPink,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun MuzoTheme(
    themeMode: AppThemeMode = AppThemeMode.AMOLED,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == AppThemeMode.AMOLED -> AmoledColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
