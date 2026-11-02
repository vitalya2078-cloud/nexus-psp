package com.nexus.psp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusColors = darkColorScheme(
    primary = Color(0xFF865CFF),
    secondary = Color(0xFF43C8FF),
    background = Color(0xFF101116),
    surface = Color(0xFF1B1D27),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexusColors,
        content = content
    )
}
