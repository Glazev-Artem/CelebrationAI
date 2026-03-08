package com.glazev.celebrationai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.glazev.celebrationai.data.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = LightSurface,
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outline = LightOutline
)

private val CelebrationColorScheme = lightColorScheme(
    primary = Color(0xFF673AB7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFF4081),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9C27B0),
    onSecondary = Color.White,
    background = Color(0xFFFFE1E1),
    onBackground = Color(0xFF311B92),
    surface = Color.White.copy(alpha = 0.9f),
    onSurface = Color(0xFF311B92),
    surfaceVariant = Color(0xFFF3E5F5),
    onSurfaceVariant = Color(0xFF311B92),
    outline = Color(0xFFCE93D8)
)

@Composable
fun CelebrationAITheme(
    appTheme: AppTheme = AppTheme.CELEBRATION,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.CELEBRATION -> CelebrationColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
