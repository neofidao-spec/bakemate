package com.bakemate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB0703C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2E3D3),
    onPrimaryContainer = Color(0xFF4A2A12),
    secondary = Color(0xFF7A6A55),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE4D6),
    onSecondaryContainer = Color(0xFF2E2417),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFEFEAE2),
    onSurfaceVariant = Color(0xFF514940),
    outline = Color(0xFF84796D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9A36A),
    onPrimary = Color(0xFF4A2A12),
    primaryContainer = Color(0xFF6B4423),
    onPrimaryContainer = Color(0xFFF2E3D3),
    secondary = Color(0xFFC7B8A4),
    onSecondary = Color(0xFF33291C),
    secondaryContainer = Color(0xFF4A3F31),
    onSecondaryContainer = Color(0xFFEADFCE),
    background = Color(0xFF171512),
    onBackground = Color(0xFFEAE5DE),
    surface = Color(0xFF201D19),
    onSurface = Color(0xFFEAE5DE),
    surfaceVariant = Color(0xFF3A352E),
    onSurfaceVariant = Color(0xFFD0C7BA),
    outline = Color(0xFF9A9084)
)

@Composable
fun BakeMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
