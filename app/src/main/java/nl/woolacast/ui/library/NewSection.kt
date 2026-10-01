package nl.woolacast.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import nl.woolacast.data.inbox.NewFilter
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.parseDate
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.theme.LocalChartColors

/** Zoveel rijen staan er dicht; de rest komt met "Nog N nieuw". */
const val NEW_COLLAPSED = 4

/** De dagkop boven een groep: vandaag, gisteren, of de datum. */
fun dayLabel(iso: String?, today: LocalDate = LocalDate.now()): String {
    val date = parseDate(iso) ?: return "Eerder"
    return when (date) {
        today -> "Vandaag"
        today.minusDays(1) -> "Gisteren"
        else -> WEEKDAY_NAMES[date.dayOfWeek.value - 1] + " " + shortDate(iso)
    }
}

private val WEEKDAY_NAMES = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag", "Zaterdag", "Zondag")

/**
 * Nieuw, bovenaan Gevolgd: alle nieuwe afleveringen van gevolgde shows, de
 * nieuwste eerst en per dag gegroepeerd. Drie vaste chips, "Alles in de
 * wachtrij", en naar links vegen om er een te verbergen. Geen eigen tabblad.
 */
fun LazyGridScope.newSection(
    all: List<SavedEpisode>,
    shown: List<SavedEpisode>,
    filters: Set<NewFilter>,
    expanded: Boolean,
    playingId: String?,
    onToggleFilter: (NewFilter) -> Unit,
    onQueueAll: () -> Unit,
    onExpand: () -> Unit,
    onPlay: (SavedEpisode) -> Unit,
    onHide: (SavedEpisode) -> Unit
) {
    if (all.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "new-head", contentType = "new-head") {
        NewHeader(count = all.size, canQueue = shown.isNotEmpty(), onQueueAll = onQueueAll)
    }
    item(span = { GridItemSpan(maxLineSpan) }, key = "new-chips", contentType = "new-chips") {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NewFilter.entries.forEach { filter ->
                FilterChipBox(filter.label, selected = filter in filters, onClick = { onToggleFilter(filter) })
            }
        }
    }
    if (shown.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "new-none", contentType = "new-none") {
            Text(
                "Niets nieuws dat hierbij past.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
        return
    }
    val visible = if (expanded) shown else shown.take(NEW_COLLAPSED)
    var lastDay: String? = null
    visible.forEach { episode ->
        val day = dayLabel(episode.releaseDate)
        if (day != lastDay) {
            lastDay = day
            item(span = { GridItemSpan(maxLineSpan) }, key = "new-day-$day-${episode.id}", contentType = "new-day") {
                Text(
                    day.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "new-${episode.id}", contentType = "new-row") {
            NewRow(episode, playing = episode.id == playingId, onPlay = { onPlay(episode) }, onHide = { onHide(episode) })
        }
    }
    if (!expanded && shown.size > NEW_COLLAPSED) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "new-more", contentType = "new-more") {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onExpand),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nog ${shown.size - NEW_COLLAPSED} nieuw",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(WoolIcons.ChevronDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }, key = "new-end", contentType = "new-end") {
        Text(
            "Je shows",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun NewHeader(count: Int, canQueue: Boolean, onQueueAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nieuw", style = MaterialTheme.typography.titleMedium)
            Box(
                Modifier
                    .heightIn(min = 22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$count", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (canQueue) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "Alles in de wachtrij", onClick = onQueueAll)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(WoolIcons.Queue, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Alles in de wachtrij", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NewRow(episode: SavedEpisode, playing: Boolean, onPlay: () -> Unit, onHide: () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onHide() },
        backgroundContent = {
            Row(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(end = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(WoolIcons.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Verbergen", style = MaterialTheme.typography.labelMedium)
            }
        }
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlay)
                    .semantics {
                        // Vegen is niet voor iedereen te doen; verbergen kan ook via de toegankelijkheidsacties.
                        customActions = listOf(CustomAccessibilityAction("Verbergen uit Nieuw") { onHide(); true })
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Artwork(episode.artworkUrl, 48.dp, corner = 11.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        listOfNotNull(episode.showTitle.takeIf { it.isNotBlank() }, minutes(episode.durationMillis)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalChartColors.current.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PlayCircle(onClick = onPlay, playing = playing)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
