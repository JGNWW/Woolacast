package nl.woolacast.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Kleuren die Material 3 niet kent maar de hitlijsten wel nodig hebben. */
@Immutable
data class ChartColors(
    val rise: Color,
    val fall: Color,
    val muted: Color,
    val rankAccent: Color,
    /** Het donkere paneel: mini-speler, noteringenstrip, chart-alerts. */
    val panel: Color,
    val onPanel: Color,
    val onPanelMuted: Color,
    val riseContainer: Color,
    val onRiseContainer: Color,
    val fallContainer: Color,
    val onFallContainer: Color,
    /** Bronkleuren in grafieken; getoetst op kleurenblindheid tegen beide achtergronden. */
    val seriesApple: Color,
    val seriesSpotify: Color
)

val LightChartColors = ChartColors(
    rise = RiseLight, fall = FallLight, muted = Ink3, rankAccent = Ember,
    panel = Ink, onPanel = Color(0xFFF6EFE5), onPanelMuted = Color(0xFFB3A695),
    riseContainer = Color(0xFFDCF0E4), onRiseContainer = Color(0xFF0B6E3E),
    fallContainer = Color(0xFFF7DED8), onFallContainer = Color(0xFF8E2C1B),
    seriesApple = Color(0xFFC4542B), seriesSpotify = Color(0xFF0E8A4E)
)

val DarkChartColors = ChartColors(
    rise = RiseDark, fall = FallDark, muted = NightInk3, rankAccent = EmberLight,
    panel = NightSurface3, onPanel = NightInk, onPanelMuted = NightInk2,
    riseContainer = Color(0xFF1E3A2B), onRiseContainer = Color(0xFF7FD9A4),
    fallContainer = Color(0xFF3F241C), onFallContainer = Color(0xFFF0A87F),
    seriesApple = Color(0xFFDE7047), seriesSpotify = Color(0xFF35A05F)
)

val LocalChartColors = staticCompositionLocalOf { LightChartColors }

private val LightScheme = lightColorScheme(
    primary = Ember,
    onPrimary = PaperSurface,
    primaryContainer = EmberContainer,
    onPrimaryContainer = OnEmberContainer,
    secondary = Ink,
    onSecondary = PaperSurface,
    background = PaperBg,
    onBackground = Ink,
    surface = PaperBg,
    onSurface = Ink,
    surfaceVariant = PaperSurface2,
    onSurfaceVariant = Ink2,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperBg,
    surfaceContainer = PaperSurface2,
    surfaceContainerHigh = PaperSurface3,
    outline = PaperLine,
    outlineVariant = PaperLine2,
    error = FallLight
)

private val DarkScheme = darkColorScheme(
    primary = EmberLight,
    onPrimary = Color(0xFF2A1006),
    primaryContainer = EmberContainerDark,
    onPrimaryContainer = OnEmberContainerDark,
    secondary = NightInk,
    onSecondary = NightBg,
    background = NightBg,
    onBackground = NightInk,
    surface = NightBg,
    onSurface = NightInk,
    surfaceVariant = NightSurface2,
    onSurfaceVariant = NightInk2,
    surfaceContainerLowest = NightSurface,
    surfaceContainerLow = NightBg,
    surfaceContainer = NightSurface2,
    surfaceContainerHigh = NightSurface3,
    outline = NightLine,
    outlineVariant = NightLine2,
    error = FallDark
)

@Composable
fun WoolacastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val chartColors = if (darkTheme) DarkChartColors else LightChartColors

    CompositionLocalProvider(LocalChartColors provides chartColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = WoolacastTypography,
            content = content
        )
    }
}
