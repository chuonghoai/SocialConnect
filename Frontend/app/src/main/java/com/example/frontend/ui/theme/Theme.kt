package com.example.frontend.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = AccentBlue,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF2A2723),
    primaryContainer = Color(0xFF5C3818),
    onPrimary = Color.White,
    onBackground = WhiteText,
    onSurface = WhiteText,
    onSurfaceVariant = LightGrayText,
    outlineVariant = DarkBorder,
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = AccentBlue,
    background = BackgroundLight,
    surface = SurfaceWarm,
    surfaceVariant = Color(0xFFFFF2E7),
    primaryContainer = Color(0xFFFFE0C4),
    onPrimary = Color.White,
    onBackground = BlackText,
    onSurface = BlackText,
    onSurfaceVariant = GrayText,
    outlineVariant = BorderSoft
)

@Composable
fun FrontendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep a stable branded palette for consistent UI across devices.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
