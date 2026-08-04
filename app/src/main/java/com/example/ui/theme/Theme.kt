package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BentoGridColorScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = Color.White,
    primaryContainer = BentoBlueCard,
    onPrimaryContainer = BentoNavy,
    secondary = ElectricPurple,
    onSecondary = Color.White,
    secondaryContainer = BentoPurpleCard,
    onSecondaryContainer = BentoPurpleOn,
    tertiary = NeonPink,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun RDAAcademyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BentoGridColorScheme,
        typography = Typography,
        content = content
    )
}


