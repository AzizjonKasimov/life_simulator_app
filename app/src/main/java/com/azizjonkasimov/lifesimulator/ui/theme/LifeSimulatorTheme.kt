package com.azizjonkasimov.lifesimulator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark "command-center" palette. All UI colors flow from this scheme, so the
// whole look can be retuned (or swapped back to light) from this one place.
private val LifeSimulatorColorScheme = darkColorScheme(
    primary = Color(0xFF6E97FF),
    onPrimary = Color(0xFF06122B),
    primaryContainer = Color(0xFF1C2C4E),
    onPrimaryContainer = Color(0xFFC2D4FF),

    secondary = Color(0xFF36D6A4),
    onSecondary = Color(0xFF03150F),
    secondaryContainer = Color(0xFF12342A),
    onSecondaryContainer = Color(0xFF8BF1D2),

    tertiary = Color(0xFFF5B53F),
    onTertiary = Color(0xFF2A1B00),
    tertiaryContainer = Color(0xFF3A2A07),
    onTertiaryContainer = Color(0xFFFFD98A),

    background = Color(0xFF0B0F18),
    onBackground = Color(0xFFE7ECF6),
    surface = Color(0xFF141A27),
    onSurface = Color(0xFFE7ECF6),
    surfaceVariant = Color(0xFF1E2636),
    onSurfaceVariant = Color(0xFF98A4BA),
    outline = Color(0xFF2C3648),
    outlineVariant = Color(0xFF222B3B),

    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1F0606),
    errorContainer = Color(0xFF3A1517),
    onErrorContainer = Color(0xFFFFB4AF),
)

private val LifeSimulatorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LifeSimulatorTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LifeSimulatorColorScheme,
        shapes = LifeSimulatorShapes,
        content = content,
    )
}
