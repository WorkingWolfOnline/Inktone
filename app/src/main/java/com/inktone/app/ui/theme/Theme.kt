package com.inktone.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun InktoneTheme(
    theme: AppTheme = AppThemes.first(),
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme(
        primary = theme.primary,
        onPrimary = Color.White,
        primaryContainer = theme.primary.copy(alpha = 0.28f),
        onPrimaryContainer = Color.White,
        secondary = theme.secondary,
        onSecondary = Color.White,
        secondaryContainer = theme.secondary.copy(alpha = 0.28f),
        onSecondaryContainer = Color.White,
        tertiary = theme.tertiary,
        onTertiary = Color.White,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF222222),
        onSurfaceVariant = Color(0xFFBFBFBF),
        outline = Color(0xFF8A8A8A)
    ) else lightColorScheme(
        primary = theme.primary,
        onPrimary = Color.White,
        primaryContainer = theme.primary.copy(alpha = 0.15f),
        onPrimaryContainer = theme.primary,
        secondary = theme.secondary,
        onSecondary = Color.White,
        secondaryContainer = theme.secondary.copy(alpha = 0.15f),
        onSecondaryContainer = theme.secondary,
        tertiary = theme.tertiary,
        onTertiary = Color.White,
        background = theme.background,
        onBackground = Color(0xFF1A1A1A),
        surface = theme.surface,
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = theme.primary.copy(alpha = 0.08f),
        onSurfaceVariant = Color(0xFF3C3C3C),
        outline = Color(0xFF9A9A9A)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = InktoneTypography,
        content = content
    )
}
