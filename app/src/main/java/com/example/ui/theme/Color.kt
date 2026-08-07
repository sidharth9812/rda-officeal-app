package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bento Grid Light Palette Base
val LightCyberBackground = Color(0xFFF3F4F9)
val LightCyberSurface = Color(0xFFFFFFFF)
val LightCyberSurfaceVariant = Color(0xFFE1E2EC)
val LightCyberBorder = Color(0xFFE1E2EC)

// Dark Palette Base
val DarkCyberBackground = Color(0xFF0F172A) // Deep Slate Dark
val DarkCyberSurface = Color(0xFF1E293B)    // Dark Slate Surface
val DarkCyberSurfaceVariant = Color(0xFF334155)
val DarkCyberBorder = Color(0xFF334155)

// Dynamic Adaptive Surface Colors
val CyberBackground: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkCyberBackground else LightCyberBackground

val CyberSurface: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkCyberSurface else LightCyberSurface

val CyberSurfaceVariant: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkCyberSurfaceVariant else LightCyberSurfaceVariant

val CyberBorder: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkCyberBorder else LightCyberBorder

// Bento Accents & Tones (Light vs Dark Adaptive)
val BentoBlueCard: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF1E3A8A) else Color(0xFFD3E4FF)

val BentoNavy: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF0284C7) else Color(0xFF001C38)

val BentoPurpleCard: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF3B0764) else Color(0xFFF3E8FF)

val BentoPurpleOn: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFFC084FC) else Color(0xFF6B21A8)

val BentoMintCard: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF064E3B) else Color(0xFFBAF3DB)

val BentoMintOn: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF34D399) else Color(0xFF064E3B)

val BentoCoralCard: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)

val BentoCoralOn: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFFFCA5A5) else Color(0xFF991B1B)

val BentoYellowCard: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFF713F12) else Color(0xFFFEF08A)

val BentoYellowOn: Color
    @Composable get() = if (ThemeManager.isDarkMode) Color(0xFFFDE047) else Color(0xFF854D0E)

// Mapped Color Compatibility for Bento Grid
val NeonCyan = Color(0xFF005AC1)
val ElectricPurple = Color(0xFF6B21A8)
val NeonPurple = Color(0xFF581C87)
val NeonPink = Color(0xFF810051)
val NeonGreen = Color(0xFF064E3B) // Present / Positive
val NeonRed = Color(0xFFBA1A1A)   // Absent / Warning
val NeonYellow = Color(0xFF854D0E)

// Text Colors (Light vs Dark)
val LightTextPrimary = Color(0xFF1A1C1E)
val LightTextSecondary = Color(0xFF44474E)
val LightTextMuted = Color(0xFF74777F)

val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF94A3B8)

val TextPrimary: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkTextPrimary else LightTextPrimary

val TextSecondary: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkTextSecondary else LightTextSecondary

val TextMuted: Color
    @Composable get() = if (ThemeManager.isDarkMode) DarkTextMuted else LightTextMuted

val DarkPrimary = Color(0xFF38BDF8)
val DarkSecondary = Color(0xFFC084FC)
val DarkTertiary = NeonPink
val DarkBackground = DarkCyberBackground
val DarkSurface = DarkCyberSurface
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkOnBackground = DarkTextPrimary
val DarkOnSurface = DarkTextPrimary



