package nl.woolacast.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit
import nl.woolacast.player.PlaybackState
import nl.woolacast.ui.common.Artwork

/**
 * Het volledige spelerscherm. De mini-speler klapt hierheen uit; dat ontbrak nog.
 */
@Composable
fun PlayerScreen(
    state: PlaybackState,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSeekBy: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tijdens het slepen volgt de balk de vinger, niet de speler.
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val progress = scrubbing ?: state.progress
    val positionMs = (progress * state.durationMs).toLong()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.ExpandMore, contentDescription = "Speler inklappen")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SPEELT NU UIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.showTitle,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.size(44.dp))
            }

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Artwork(state.artworkUrl, 300.dp, corner = 24.dp)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                state.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.showTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Slider(
                value = progress,
                onValueChange = { scrubbing = it },
                onValueChangeFinished = {
                    scrubbing?.let(onSeekTo)
                    scrubbing = null
                },
                enabled = state.durationMs > 0L
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "-" + formatTime(state.durationMs - positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeekBy(-10_000L) }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Filled.Replay10,
                        contentDescription = "10 seconden terug",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(76.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pauzeren" else "Afspelen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
                IconButton(onClick = { onSeekBy(30_000L) }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Filled.Forward30,
                        contentDescription = "30 seconden vooruit",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (state.durationMs <= 0L) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Duur nog onbekend — die komt binnen zodra de aflevering geladen is.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    val hours = TimeUnit.MILLISECONDS.toHours(safe)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
