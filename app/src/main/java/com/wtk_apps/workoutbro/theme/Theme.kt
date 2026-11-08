package com.wtk_apps.workoutbro.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WorkoutColorScheme =
  darkColorScheme(
    primary = AccentGold,
    onPrimary = Ink,
    secondary = RestGreen,
    onSecondary = Ink,
    tertiary = PrepareBlue,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Outline1,
  )

/** Fixed, black-and-gold dark theme for WorkoutBro. */
@Composable
fun WorkoutBroTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = WorkoutColorScheme, typography = Typography, content = content)
}
