package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandBlueLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = StatusOnline,
    onTertiary = Color.White,
    error = StatusOffline,
    onError = Color.White,
    errorContainer = StatusOfflineBg,
    onErrorContainer = Color(0xFF991B1B),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightCardBg,
    onSurface = LightTextPrimary,
    surfaceVariant = LightInputBg,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueAccent,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = BrandBlueLight,
    onSecondary = Color.White,
    tertiary = StatusOnlineGlow,
    onTertiary = Color(0xFF064E3B),
    error = StatusOffline,
    onError = Color.White,
    background = Color(0xFF0B132B),
    onBackground = DarkTextPrimary,
    surface = DarkCardBg,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkInputBg,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder
)

@Composable
fun TalalProTheme(
    darkTheme: Boolean = false, // Light mode by default as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
