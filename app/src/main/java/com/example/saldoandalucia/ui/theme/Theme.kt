package com.example.saldoandalucia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF2E7D32)
val GreenDark = Color(0xFF1B5E20)
val GreenLight = Color(0xFF81C784)
val SurfaceDark = Color(0xFF121212)
val SurfaceCard = Color(0xFF1E1E1E)
val ErrorRed = Color(0xFFEF5350)

private val AppColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    secondary = GreenLight,
    background = SurfaceDark,
    surface = SurfaceCard,
    error = ErrorRed
)

@Composable
fun SaldoAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
