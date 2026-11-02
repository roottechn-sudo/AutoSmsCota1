package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = Color(0xFF003909),
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = Color(0xFFA6F5A0),
    secondary = OrangeMoneyDarkAccent,
    onSecondary = Color(0xFF4C1F00),
    secondaryContainer = Color(0xFF6B2F00),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFFF8A80),
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = OrangeMoneyAccent,
    onSecondary = Color.White,
    secondaryContainer = OrangeMoneyContainer,
    onSecondaryContainer = OnOrangeMoneyContainer,
    tertiary = Terracotta,
    tertiaryContainer = TerracottaContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun AutoSmsCotaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
