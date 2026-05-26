package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = DeepObsidian,
    primaryContainer = ElectroViolet,
    onPrimaryContainer = TextPrimary,
    secondary = ElectroViolet,
    onSecondary = TextPrimary,
    tertiary = HotPink,
    onTertiary = DeepObsidian,
    background = DeepObsidian,
    onBackground = TextPrimary,
    surface = SteelBlue,
    onSurface = TextPrimary,
    surfaceVariant = LightSlate,
    onSurfaceVariant = TextSecondary,
    error = CoralRed,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ElectroViolet,
    onPrimary = TextPrimary,
    primaryContainer = CyberCyan,
    onPrimaryContainer = DeepObsidian,
    secondary = CyberCyan,
    onSecondary = DeepObsidian,
    tertiary = HotPink,
    background = TextPrimary,
    onBackground = DeepObsidian,
    surface = TextPrimary,
    onSurface = DeepObsidian,
    surfaceVariant = LightSlate,
    onSurfaceVariant = TextSecondary,
    error = CoralRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode as first-class gaming standard
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
