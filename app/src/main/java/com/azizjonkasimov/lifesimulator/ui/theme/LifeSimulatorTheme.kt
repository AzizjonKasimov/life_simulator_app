package com.azizjonkasimov.lifesimulator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeSimulatorColorScheme = lightColorScheme(
    primary = Color(0xFF1F5EFF),
    onPrimary = Color.White,
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    tertiary = Color(0xFF7A4F01),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7ECF4),
    onSurface = Color(0xFF172033),
    onSurfaceVariant = Color(0xFF536070),
    error = Color(0xFFBA1A1A),
)

@Composable
fun LifeSimulatorTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LifeSimulatorColorScheme,
        content = content,
    )
}
