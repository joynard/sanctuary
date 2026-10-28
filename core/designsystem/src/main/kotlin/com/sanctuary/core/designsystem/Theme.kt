package com.sanctuary.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Sage,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE9DF),
    onPrimaryContainer = Color(0xFF1E3428),
    secondary = Color(0xFF6F8174),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E9DF),
    onSecondaryContainer = Color(0xFF263029),
    tertiary = Clay,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1E1D2),
    onTertiaryContainer = Color(0xFF3D2B1D),
    background = Oat,
    onBackground = Ink,
    surface = Color(0xFFFBF8F1),
    onSurface = Ink,
    surfaceVariant = OatDeep,
    onSurfaceVariant = Color(0xFF5C655D),
    outline = Color(0xFF7A8278),
    outlineVariant = Color(0xFFCFD2C7),
    error = Color(0xFFA15C56),
    onError = Color.White,
    errorContainer = Color(0xFFF6DCD8),
    onErrorContainer = Color(0xFF3F1612),
    inverseSurface = Color(0xFF2B352E),
    inverseOnSurface = Color(0xFFF0F2E9),
    inversePrimary = SageLight,
)

private val DarkScheme = darkColorScheme(
    primary = SageLight,
    onPrimary = Color(0xFF1A3024),
    primaryContainer = Color(0xFF2C4738),
    onPrimaryContainer = Color(0xFFCDE6D4),
    secondary = Color(0xFFB4C1B5),
    onSecondary = Color(0xFF223128),
    secondaryContainer = Color(0xFF334239),
    onSecondaryContainer = Color(0xFFDDE8DF),
    tertiary = Color(0xFFD8B79D),
    onTertiary = Color(0xFF3B2A1D),
    tertiaryContainer = Color(0xFF55402F),
    onTertiaryContainer = Color(0xFFF5DFCC),
    background = Night,
    onBackground = Color(0xFFE4EAE2),
    surface = NightSurface,
    onSurface = Color(0xFFE4EAE2),
    surfaceVariant = Color(0xFF2A342E),
    onSurfaceVariant = Color(0xFFB6C1B7),
    outline = Color(0xFF8B968C),
    outlineVariant = Color(0xFF3E4B43),
    error = Color(0xFFE0A49E),
    onError = Color(0xFF4A1713),
    errorContainer = Color(0xFF5F2A25),
    onErrorContainer = Color(0xFFF6DCD8),
    inverseSurface = Color(0xFFE4EAE2),
    inverseOnSurface = Color(0xFF27322B),
    inversePrimary = Sage,
)

@Composable
fun SanctuaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extras = if (darkTheme) DarkSanctuaryColors else LightSanctuaryColors

    CompositionLocalProvider(LocalSanctuaryColors provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SanctuaryTypography,
            shapes = SanctuaryShapes,
            content = content,
        )
    }
}
