package com.jarvis.personalassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object JarvisPalette {
    val DeepVoid = Color(0xFF050B14)
    val CyberSlate = Color(0xFF0D1B2A)
    val GlassSurface = Color(0xB80D1B2A)
    val ArcCyan = Color(0xFF00F0FF)
    val HologramBlue = Color(0xFF0088FF)
    val NeonTeal = Color(0xFF00E5A3)
    val ReactorGold = Color(0xFFFFB800)
    val CrimsonAlert = Color(0xFFFF2A54)
    val TextPrimary = Color(0xFFE6F7FF)
    val TextSecondary = Color(0xFF8AB4F8)
    val GridLine = Color(0x3300F0FF)
}

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisPalette.ArcCyan,
    onPrimary = JarvisPalette.DeepVoid,
    primaryContainer = JarvisPalette.CyberSlate,
    onPrimaryContainer = JarvisPalette.ArcCyan,
    secondary = JarvisPalette.HologramBlue,
    onSecondary = JarvisPalette.DeepVoid,
    tertiary = JarvisPalette.NeonTeal,
    background = JarvisPalette.DeepVoid,
    onBackground = JarvisPalette.TextPrimary,
    surface = JarvisPalette.CyberSlate,
    onSurface = JarvisPalette.TextPrimary,
    surfaceVariant = Color(0xFF102438),
    onSurfaceVariant = JarvisPalette.TextSecondary,
    error = JarvisPalette.CrimsonAlert
)

val JarvisTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = 1.4.sp,
        color = JarvisPalette.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        letterSpacing = 1.1.sp,
        color = JarvisPalette.ArcCyan
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.8.sp,
        color = JarvisPalette.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = JarvisPalette.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = JarvisPalette.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.0.sp,
        color = JarvisPalette.ArcCyan
    )
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = JarvisTypography,
        content = content
    )
}
