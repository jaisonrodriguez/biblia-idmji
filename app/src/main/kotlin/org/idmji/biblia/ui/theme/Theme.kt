package org.idmji.biblia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import org.idmji.biblia.model.ReadingTheme

private val DayColorScheme = lightColorScheme(
    primary = IdmjiPrimaryNavy,
    onPrimary = DaySurface,
    primaryContainer = IdmjiNavyLight,
    onPrimaryContainer = DaySurface,
    secondary = IdmjiGold,
    onSecondary = DayText,
    background = DayBackground,
    onBackground = DayText,
    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayTextMuted
)

private val SepiaColorScheme = lightColorScheme(
    primary = IdmjiPrimaryNavy,
    onPrimary = SepiaSurface,
    primaryContainer = IdmjiNavyLight,
    onPrimaryContainer = SepiaSurface,
    secondary = IdmjiGold,
    onSecondary = SepiaText,
    background = SepiaBackground,
    onBackground = SepiaText,
    surface = SepiaSurface,
    onSurface = SepiaText,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = SepiaTextMuted
)

private val NightColorScheme = darkColorScheme(
    primary = IdmjiGold,
    onPrimary = NightBackground,
    primaryContainer = IdmjiNavyLight,
    onPrimaryContainer = NightText,
    secondary = IdmjiGold,
    onSecondary = NightBackground,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextMuted
)

@Composable
fun BibliaIDMJITheme(
    readingTheme: ReadingTheme = ReadingTheme.SEPIA,
    content: @Composable () -> Unit
) {
    val colorScheme = when (readingTheme) {
        ReadingTheme.DAY -> DayColorScheme
        ReadingTheme.SEPIA -> SepiaColorScheme
        ReadingTheme.NIGHT -> NightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
