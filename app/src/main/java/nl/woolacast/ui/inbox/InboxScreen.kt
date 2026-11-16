package nl.woolacast.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.ui.library.AlertCard
import nl.woolacast.ui.library.LibraryViewModel

/**
 * Wat er sinds je laatste bezoek gebeurde, achter de bel: shows die je volgt en
 * die bewogen in de hitlijst, nieuwe afleveringen, en nieuwe podcasts van makers
 * die je volgt. Dezelfde gegevens als in de Bibliotheek, op één plek bij elkaar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: LibraryViewModel,
    countryCode: String,
    playingId: String?,
    onBack: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    onPlay: (Episode) -> Unit
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val newEpisodes by viewModel.newEpisodeList.collectAsStateWithLifecycle()
    val newShows by viewModel.newShows.collectAsStateWithLifecycle()
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val makers by viewModel.makers.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    LaunchedEffect(countryCode, follows.size, makers.size) {
        viewModel.loadAlerts(countryCode)
        viewModel.refreshFeeds()
        viewModel.refreshMakers(countryCode)
    }

    Column(Modifier.fillMaxSize()) {
        TitleBar("Wat is er nieuw", onBack)
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                viewModel.loadAlerts(countryCode)
                viewModel.refreshFeeds(force = true)
                viewModel.refreshMakers(countryCode, force = true)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
                if (follows.isEmpty() && makers.isEmpty()) {
                    item {
                        NoticePanel(
                            title = "Nog niets om te melden",
                            message = "Volg een podcast of een maker. Hier zie je dan wie er beweegt in de hitlijst, " +
                                "welke afleveringen nieuw zijn en wanneer een maker iets nieuws begint.",
                            outerPadding = PaddingValues(vertical = 20.dp)
                        )
                    }
                    return@LazyColumn
                }
                if (alerts.isEmpty() && newEpisodes.isEmpty() && newShows.isEmpty()) {
                    item {
                        Text(
                            "Niets nieuws sinds gisteren. Trek naar beneden om opnieuw te kijken.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }
                if (alerts.isNotEmpty()) {
                    item(key = "alerts") { AlertCard(alerts, onOpenPodcast) }
                }
                if (newShows.isNotEmpty()) {
                    item(key = "shows-head") { Head("Nieuwe podcasts van makers") }
                    items(newShows, key = { "show-" + it.showId }) { show ->
                        Line(
                            artworkUrl = show.artworkUrl,
                            title = show.title,
                            subtitle = "Nieuw van ${show.makerName}",
                            onClick = {
                                viewModel.seen(show)
                                onOpenPodcast(show.showId, show.feedUrl, show.title)
                            }
                        )
                    }
                }
                if (newEpisodes.isNotEmpty()) {
                    item(key = "episodes-head") { Head("Nieuwe afleveringen") }
                    items(newEpisodes.take(MAX_EPISODES), key = { "episode-" + it.id }) { episode ->
                        Line(
                            artworkUrl = episode.artworkUrl,
                            title = episode.title,
                            subtitle = listOfNotNull(
                                episode.showTitle.takeIf { it.isNotBlank() },
                                relativeDay(episode.releaseDate),
                                minutes(episode.durationMillis)
                            ).joinToString(" · "),
                            onClick = { onPlay(episode.toEpisode()) },
                            trailing = { PlayCircle(onClick = { onPlay(episode.toEpisode()) }, playing = episode.id == playingId) }
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_EPISODES = 40

@Composable
private fun Head(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 22.dp, bottom = 4.dp))
}

@Composable
private fun Line(
    artworkUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(artworkUrl, 48.dp, corner = 11.dp)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
