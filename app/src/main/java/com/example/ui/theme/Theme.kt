package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeManager {
    private const val PREFS_NAME = "rda_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"

    var isDarkMode by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun toggleTheme(context: Context) {
        val newValue = !isDarkMode
        isDarkMode = newValue
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, newValue).apply()
    }

    fun setDarkMode(context: Context, dark: Boolean) {
        isDarkMode = dark
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }
}

private val BentoGridColorScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = ElectricPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF6B21A8),
    tertiary = NeonPink,
    background = LightCyberBackground,
    onBackground = LightTextPrimary,
    surface = LightCyberSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCyberSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCyberBorder,
    error = NeonRed,
    onError = Color.White
)

private val DarkBentoGridColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF001C38),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF3B0764),
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = Color(0xFFF472B6),
    background = DarkCyberBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCyberSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCyberSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCyberBorder,
    error = Color(0xFFF87171),
    onError = Color.White
)

@Composable
fun RDAAcademyTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkBentoGridColorScheme else BentoGridColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}



