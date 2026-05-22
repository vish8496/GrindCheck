package com.grindcheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GrindDarkScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Bg,
    secondary = NeonGreenDim,
    background = Bg,
    onBackground = TextPrimary,
    surface = BgElev1,
    onSurface = TextPrimary,
    surfaceVariant = BgElev2,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    outline = Border,
)

@Composable
fun GrindCheckTheme(
    darkTheme: Boolean = true, // Always dark — GrindCheck is dark-themed by brand
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GrindDarkScheme,
        typography = GrindTypography,
        content = content,
    )
}
