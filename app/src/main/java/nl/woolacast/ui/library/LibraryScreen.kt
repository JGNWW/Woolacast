package nl.woolacast.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.TimeUnit
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.theme.LocalChartColors

private enum class LibraryTab(val label: String) {
    FOLLOWED("Gevolgd"), QUEUE("Wachtrij"), SAVED("Bewaard")
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    countryCode: String,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    onPlay: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LibraryTab.FOLLOWED) }

    LaunchedEffect(countryCode, follows.size) { viewModel.loadAlerts(countryCode) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "WOOLACAST",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp)
        )
        Text(
            "Bibliotheek",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 12.dp)
        )

        TabRow(
            selectedTabIndex = LibraryTab.entries.indexOf(tab),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LibraryTab.entries.forEach { entry ->
                Tab(
                    selected = entry == tab,
                    onClick = { tab = entry },
                    text = { Text(entry.label, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }

        when (tab) {
            LibraryTab.FOLLOWED -> FollowedTab(follows, alerts, onOpenPodcast)
            LibraryTab.QUEUE -> EpisodeTab(
                episodes = queue,
                empty = "Nog niets in de wachtrij. Zet een aflevering erin vanaf een podcastpagina.",
                onPlay = onPlay,
                onRemove = viewModel::removeFromQueue
            )
            LibraryTab.SAVED -> EpisodeTab(
                episodes = saved,
                empty = "Nog niets bewaard. Bewaar een aflevering om hem hier terug te vinden.",
                onPlay = onPlay,
                onRemove = viewModel::removeSaved
            )
        }
    }
}

@Composable
private fun FollowedTab(
    follows: List<nl.woolacast.data.local.FollowedShow>,
    alerts: List<ChartAlert>,
    onOpenPodcast: (String, String?, String) -> Unit
) {
    if (follows.isEmpty()) {
        NoticePanel(
            title = "Nog niets gevolgd",
            message = "Open een podcast uit een hitlijst en tik op Volgen. " +
                "Wat je volgt komt hier te staan."
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (alerts.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                AlertCard(alerts, onOpenPodcast)
            }
        }
        items(follows, key = { it.id }) { show ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPodcast(show.id, show.feedUrl, show.title) }
            ) {
                Artwork(show.artworkUrl, 102.dp, corner = 13.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    show.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Van de shows die je volgt: wie er bewoog. Alleen mogelijk dankzij de dagelijkse metingen. */
@Composable
private fun AlertCard(alerts: List<ChartAlert>, onOpenPodcast: (String, String?, String) -> Unit) {
    val colors = LocalChartColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Chart-alerts",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
        alerts.forEach { alert ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPodcast(alert.showId, alert.feedUrl, alert.title) }
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Artwork(alert.artworkUrl, 36.dp, corner = 9.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Nu #${alert.rank} · ${alert.source} ${alert.country}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ArrowDropUp,
                        contentDescription = null,
                        tint = colors.rise,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${alert.move}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.rise
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeTab(
    episodes: List<SavedEpisode>,
    empty: String,
    onPlay: (Episode) -> Unit,
    onRemove: (String) -> Unit
) {
    if (episodes.isEmpty()) {
        NoticePanel(title = "Leeg", message = empty)
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(episodes.size) { index ->
            val episode = episodes[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(episode.toEpisode()) }
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Artwork(episode.artworkUrl, 52.dp, corner = 12.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        listOfNotNull(
                            episode.showTitle.takeIf { it.isNotBlank() },
                            episode.durationMillis?.let { "${TimeUnit.MILLISECONDS.toMinutes(it)} min" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier.size(44.dp).clickable { onPlay(episode.toEpisode()) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Afspelen")
                }
                IconButton(onClick = { onRemove(episode.id) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Verwijderen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private fun SavedEpisode.toEpisode() = Episode(
    id = id,
    showId = showId,
    showTitle = showTitle,
    title = title,
    description = null,
    artworkUrl = artworkUrl,
    audioUrl = audioUrl,
    durationMillis = durationMillis,
    releaseDate = releaseDate
)
