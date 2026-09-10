package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.woolacast.player.PlaybackState
import nl.woolacast.ui.theme.LocalChartColors

/** De donkere kaart boven de navigatie (.mini): tikken klapt de speler uit. */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChartColors.current
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(colors.panel)
            .clickable(onClick = onExpand)
            .height(60.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(state.artworkUrl, 44.dp, corner = 9.dp, elevation = 0.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                state.title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onPanel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    state.showTitle.takeIf { it.isNotBlank() },
                    when {
                        state.isBuffering && state.durationMs == 0L -> "laden…"
                        state.durationMs > 0L -> remaining(state.remainingMs)
                        else -> null
                    }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPanelMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconAction(
            if (state.isPlaying) WoolIcons.Pause else WoolIcons.Play,
            if (state.isPlaying) "Pauzeren" else "Afspelen",
            onTogglePlay,
            tint = colors.onPanel,
            iconSize = 24.dp
        )
        IconAction(WoolIcons.SkipForward, "30 seconden vooruit", onSkipForward, tint = colors.onPanel, iconSize = 24.dp)
    }
}
