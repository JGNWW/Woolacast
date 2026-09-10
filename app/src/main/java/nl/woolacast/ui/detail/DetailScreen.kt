package nl.woolacast.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Episode
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.Artwork

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onPlay: (Episode) -> Unit,
    onOpenTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val queued by viewModel.queued.collectAsStateWithLifecycle()
    val stored by viewModel.stored.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(DetailTab.EPISODES) }
    val podcast = state.podcast
    val isFollowed = podcast != null && follows.any { it.id == podcast.id }

    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Terug")
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Text(
                state.error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp)
            )

            podcast != null -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Artwork(podcast.artworkUrl, 112.dp, corner = 18.dp)
                        Column {
                            Text(podcast.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                podcast.publisher,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            podcast.episodeCount?.let { count ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${podcast.genre.orEmpty()} · $count afleveringen".trim(' ', '·'),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Button(
                            onClick = viewModel::toggleFollow,
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)
                        ) {
                            Icon(
                                if (isFollowed) Icons.Filled.Check else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (isFollowed) "Gevolgd" else "Volgen")
                        }
                        state.episodes.firstOrNull()?.let { newest ->
                            OutlinedButton(
                                onClick = { onPlay(newest) },
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 46.dp)
                            ) { Text("Nieuwste afl.") }
                        }
                    }
                }

                if (state.positions.isNotEmpty()) {
                    item {
                        ChartStrip(
                            positions = state.positions,
                            countryCount = state.countryCount,
                            onClick = onOpenTracker
                        )
                    }
                }



                item {
                    TabRow(
                        selectedTabIndex = DetailTab.entries.indexOf(tab),
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        DetailTab.entries.forEach { entry ->
                            Tab(
                                selected = entry == tab,
                                onClick = { tab = entry },
                                text = {
                                    Text(entry.label, style = MaterialTheme.typography.titleMedium)
                                }
                            )
                        }
                    }
                }

                when (tab) {
                    DetailTab.EPISODES -> items(state.episodes.size) { index ->
                        val episode = state.episodes[index]
                        EpisodeRow(
                            episode = episode,
                            positionMs = progress[episode.id],
                            inQueue = queued.any { it.id == episode.id },
                            isSaved = stored.any { it.id == episode.id },
                            onPlay = { onPlay(episode) },
                            onQueue = { viewModel.toggleQueue(episode) },
                            onSave = { viewModel.toggleSaved(episode) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    DetailTab.CHARTS -> {
                        if (state.positions.isEmpty()) {
                            item {
                                Text(
                                    "Deze show staat nog niet in de vastgelegde lijsten.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        } else {
                            items(state.positions.size) { index ->
                                val position = state.positions[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .height(46.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(Catalog.country(position.country).flag, fontSize = 17.sp)
                                    Text(
                                        Catalog.country(position.country).label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        position.source.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "#${position.rank}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }

                    DetailTab.ABOUT -> item {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                podcast.description ?: "Geen omschrijving in de feed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            podcast.feedUrl?.let { feed ->
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "FEED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(feed, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DetailTab(val label: String) {
    EPISODES("Afleveringen"), CHARTS("Noteringen"), ABOUT("Over")
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    positionMs: Long?,
    inQueue: Boolean,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 20.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Artwork(episode.artworkUrl, 56.dp, corner = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Text(
                listOfNotNull(
                    episode.releaseDate,
                    episode.durationMillis?.let(::formatDuration),
                    if (episode.audioUrl == null) "geen audio" else null
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Waar je gebleven bent, maar alleen als je echt begonnen bent.
            val duration = episode.durationMillis
            if (positionMs != null && duration != null && duration > 0L) {
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(
                    progress = { (positionMs.toFloat() / duration).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "nog ${formatDuration(duration - positionMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Afspelen")
            }
            IconButton(onClick = onQueue) {
                Icon(
                    if (inQueue) Icons.Filled.PlaylistAddCheck else Icons.Filled.PlaylistAdd,
                    contentDescription = if (inQueue) "Uit wachtrij" else "In wachtrij",
                    tint = if (inQueue) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSave) {
                Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isSaved) "Niet meer bewaren" else "Bewaren",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    return "$minutes min"
}

/**
 * De noteringen van deze show in één balk: waar staat hij, bij welke bron.
 * Tikken opent het volledige verloop.
 */
@Composable
private fun ChartStrip(
    positions: List<ShowPositionLike>,
    countryCount: Int,
    onClick: () -> Unit
) {
    val here = positions.groupBy { it.source }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Filled.BarChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(20.dp)
        )
        listOf(SourceId.APPLE, SourceId.SPOTIFY).forEach { source ->
            val best = here[source]?.minByOrNull { it.rank } ?: return@forEach
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${best.rank}",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    source.label.substringBefore(' '),
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (countryCount == 1) "1 land" else "$countryCount landen",
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "Verloop bekijken",
            tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

private typealias ShowPositionLike = nl.woolacast.data.dataset.ShowPosition
