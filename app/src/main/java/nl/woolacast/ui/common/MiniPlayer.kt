package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.woolacast.player.PlaybackState
import nl.woolacast.player.SKIP_FORWARD_MS
import nl.woolacast.ui.theme.LocalChartColors

/**
 * De kaart boven de navigatie (.mini): getint naar de hoes van wat er speelt,
 * met de voortgang in de hoeskleur. Tikken klapt de speler uit.
 */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChartColors.current
    val cover = rememberCoverColors(state.artworkUrl)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val onMini = if (dark) colors.onPanel else Color(0xFFF6EFE5)
    val onMiniMuted = onMini.copy(alpha = 0.75f)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(cover.mini)
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp).padding(start = 9.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Artwork(state.artworkUrl, 44.dp, corner = 8.dp, elevation = 0.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = onMini,
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
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = onMiniMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconAction(
                if (state.isPlaying) WoolIcons.Pause else WoolIcons.Play,
                if (state.isPlaying) "Pauzeren" else "Afspelen",
                onTogglePlay,
                tint = onMini,
                iconSize = 24.dp
            )
            // Hetzelfde teken als in de speler: een rondje met het aantal seconden.
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onSkipForward),
                contentAlignment = Alignment.Center
            ) {
                Icon(WoolIcons.Forward30, "${SKIP_FORWARD_MS / 1000} seconden vooruit", tint = onMini, modifier = Modifier.size(26.dp))
                Text(
                    "${SKIP_FORWARD_MS / 1000}",
                    color = onMini,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // Voortgang in de hoeskleur, als dunne lijn langs de onderrand.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(cover.accent)
        )
    }
}
