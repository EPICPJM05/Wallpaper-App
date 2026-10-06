package com.poojan.pathkeeper

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Night = Color(0xFF0B0F1A)
val NightSoft = Color(0xFF141B2D)
val Card = Color(0xFF1A2236)
val Accent = Color(0xFF8AB4F8)
val AccentWarm = Color(0xFFF2C879)
val TextPrimary = Color(0xFFEAF0FF)
val TextMuted = Color(0xFF8D97B0)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Night,
    background = Night,
    surface = Card,
    onSurface = TextPrimary,
    onBackground = TextPrimary
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 30.sp, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.5.sp)
)

@Composable
fun PathKeeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
