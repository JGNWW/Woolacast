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
    val rankAccent: Color
)

val LocalChartColors = staticCompositionLocalOf {
    ChartColors(RiseLight, FallLight, Ink3, Ember)
}

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
    val chartColors = if (darkTheme) {
        ChartColors(RiseDark, FallDark, NightInk3, EmberLight)
    } else {
        ChartColors(RiseLight, FallLight, Ink3, Ember)
    }

    CompositionLocalProvider(LocalChartColors provides chartColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = WoolacastTypography,
            content = content
        )
    }
}
