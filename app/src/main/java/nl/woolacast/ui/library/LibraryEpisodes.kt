package nl.woolacast.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.data.local.Bookmark
import nl.woolacast.data.local.HistoryItem
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.ReorderableColumn
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.clock
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.ui.common.remaining
import nl.woolacast.ui.theme.LocalChartColors

/** De onderdelen van een tabblad, als rij chips die opzij schuift als hij niet past. */
@Composable
internal fun SectionChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            FilterChipBox(label, selected = index == selected, onClick = { onSelect(index) })
        }
    }
}

/**
 * Verder luisteren, bovenaan Gevolgd: de afleveringen waar je halverwege bent,
 * de laatst gespeelde eerst. Eén tik en hij gaat verder waar je was.
 */
internal fun LazyGridScope.continueSection(
    items: List<SavedEpisode>,
    progress: Map<String, Long>,
    playingId: String?,
    onPlay: (SavedEpisode) -> Unit
) {
    if (items.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "continue-head", contentType = "continue-head") {
        Text("Verder luisteren", style = MaterialTheme.typography.titleMedium)
    }
    item(span = { GridItemSpan(maxLineSpan) }, key = "continue-row", contentType = "continue-row") {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { episode ->
                ContinueCard(episode, progress[episode.id] ?: 0L, playing = episode.id == playingId, onPlay = { onPlay(episode) })
            }
        }
    }
}

@Composable
private fun ContinueCard(episode: SavedEpisode, positionMs: Long, playing: Boolean, onPlay: () -> Unit) {
    val duration = episode.durationMillis?.takeIf { it > 0L }
    Column(
        Modifier
            .width(132.dp)
            .clickable(onClickLabel = "Verder luisteren", onClick = onPlay)
    ) {
        Box {
            Artwork(episode.artworkUrl, 132.dp, corner = 13.dp, elevation = 0.dp)
            // Hoe ver je bent, als streep langs de onderrand van de hoes.
            if (duration != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((positionMs.toFloat() / duration).coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            episode.title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
            color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (duration != null) remaining(duration - positionMs) else episode.showTitle,
            fontSize = 11.sp,
            color = LocalChartColors.current.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** De wachtrij, met een handvat om de volgorde te verslepen. */
@Composable
internal fun QueueTab(
    queue: List<SavedEpisode>,
    playingId: String?,
    onPlay: (Episode) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit
) {
    if (queue.isEmpty()) {
        NoticePanel(
            title = "Leeg",
            message = "Nog niets in de wachtrij. Zet een aflevering erin vanaf een podcastpagina; " +
                "de speler gaat er vanzelf mee door."
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                listOfNotNull(
                    if (queue.size == 1) "1 aflevering" else "${queue.size} afleveringen",
                    minutes(queue.sumOf { it.durationMillis ?: 0L }.takeIf { it > 0L })
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Wissen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = "Wachtrij wissen", onClick = onClear)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
        ReorderableColumn(items = queue, key = { it.id }, onMove = onMove) { index, episode, handle ->
            val playing = episode.id == playingId
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(episode.toEpisode()) }
                        .padding(start = 20.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LocalChartColors.current.muted,
                        modifier = Modifier.width(22.dp)
                    )
                    Artwork(episode.artworkUrl, 48.dp, corner = 11.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            episode.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            listOfNotNull(episode.showTitle.takeIf { it.isNotBlank() }, minutes(episode.durationMillis)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconAction(
                        WoolIcons.Close, "Uit de wachtrij", { onRemove(episode.id) },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, iconSize = 18.dp
                    )
                    handle()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

/** Wat je afspeelde, de laatste eerst, met een ingang naar de statistiek. */
@Composable
internal fun HistoryTab(
    history: List<HistoryItem>,
    progress: Map<String, Long>,
    playingId: String?,
    onPlay: (Episode) -> Unit,
    onRemove: (String) -> Unit,
    onOpenStats: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        item(key = "stats") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onOpenStats)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(WoolIcons.Bars, null, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("Je luisterstatistiek", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Hoeveel je luisterde, en naar wat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(WoolIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
        if (history.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Nog niets afgespeeld. Wat je luistert komt hier te staan, zodat je het terugvindt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
        items(history, key = { it.episode.id }) { item ->
            val episode = item.episode
            val playing = episode.id == playingId
            val position = progress[episode.id]
            val duration = episode.durationMillis?.takeIf { it > 0L }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(episode.toEpisode()) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(episode.artworkUrl, 52.dp, corner = 12.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        listOfNotNull(
                            episode.showTitle.takeIf { it.isNotBlank() },
                            relativeDay(item.at.take(10)),
                            when {
                                item.finished -> "uitgeluisterd"
                                position != null && duration != null -> remaining(duration - position)
                                else -> null
                            }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PlayCircle(onClick = { onPlay(episode.toEpisode()) }, playing = playing)
                IconAction(
                    WoolIcons.Close, "Uit de geschiedenis", { onRemove(episode.id) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, iconSize = 18.dp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Plekken die je markeerde. Een tik speelt de aflevering vanaf daar. */
@Composable
internal fun BookmarksTab(
    bookmarks: List<Bookmark>,
    onPlayAt: (Episode, Long) -> Unit,
    onRemove: (String) -> Unit
) {
    if (bookmarks.isEmpty()) {
        NoticePanel(
            title = "Nog geen bladwijzers",
            message = "Hoor je iets wat je wilt terugvinden? Tik in de speler op Bladwijzer; " +
                "de plek komt hier te staan en een tik speelt hem weer af."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
        items(bookmarks.asReversed(), key = { it.id }) { bookmark ->
            val episode = bookmark.episode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayAt(episode.toEpisode(), bookmark.positionMs) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(episode.artworkUrl, 52.dp, corner = 12.dp)
                Column(Modifier.weight(1f)) {
                    Text(episode.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        listOfNotNull("op ${clock(bookmark.positionMs)}", episode.showTitle.takeIf { it.isNotBlank() }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconAction(
                    WoolIcons.Close, "Bladwijzer verwijderen", { onRemove(bookmark.id) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, iconSize = 18.dp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
