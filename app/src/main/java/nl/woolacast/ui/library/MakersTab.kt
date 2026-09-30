package nl.woolacast.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import nl.woolacast.data.local.FollowedMaker
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.MakerTile
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.OutlinePillButton
import nl.woolacast.ui.common.PanelCard
import nl.woolacast.ui.common.PanelRow
import nl.woolacast.ui.common.SectionLabel
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.ui.theme.LocalChartColors

/**
 * Makers die je volgt. Geen stroom van afleveringen (die staan onder Gevolgd),
 * wel wat er nieuw is: een nieuwe podcast bovenaan, en per maker hoeveel van
 * zijn shows sinds gisteren iets nieuws hebben.
 */
@Composable
internal fun MakersTab(
    makers: List<FollowedMaker>,
    status: Map<String, MakerStatus>,
    faces: Map<String, List<String>>,
    newShows: List<MakerNewShow>,
    suggestions: List<MakerSuggestion>,
    onOpenMaker: (String) -> Unit,
    onOpenNewShow: (MakerNewShow) -> Unit,
    onFollow: (MakerSuggestion) -> Unit,
    onUnfollow: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        if (makers.isEmpty()) {
            item {
                NoticePanel(
                    outerPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                    title = "Nog geen makers gevolgd",
                    message = "Tik op de naam van de maker op een podcastpagina en kies Volg maker. " +
                        "Je ziet hier dan wanneer hij een nieuwe podcast begint."
                )
            }
        } else {
            if (newShows.isNotEmpty()) {
                item {
                    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                        NewShowsCard(newShows, onOpenNewShow)
                    }
                }
            }
            item { SectionLabel("Je makers · ${makers.size}") }
            items(makers, key = { "maker-${it.key}" }) { maker ->
                FollowedMakerRow(maker, status[maker.key], faces[maker.key], onOpenMaker, onUnfollow)
            }
        }

        if (suggestions.isNotEmpty()) {
            item { SectionLabel("Van podcasts die je volgt") }
            items(suggestions, key = { "suggestion-${it.key}" }) { suggestion ->
                SuggestionRow(suggestion, faces[suggestion.key], onOpenMaker, onFollow)
            }
        }
    }
}

/**
 * Dezelfde kaart als de chart-alerts. Rechts staat "Nog niet bekeken" en niet
 * "Sinds gisteren": een nieuwe podcast blijft staan tot je hem opent, ook als
 * hij al een paar dagen oud is.
 */
@Composable
private fun NewShowsCard(shows: List<MakerNewShow>, onOpen: (MakerNewShow) -> Unit) {
    PanelCard("Nieuw van je makers", "Nog niet bekeken") {
        shows.forEach { show ->
            PanelRow(
                artworkUrl = show.artworkUrl,
                title = show.title,
                subtitle = listOfNotNull(
                    "Nieuwe podcast",
                    show.makerName,
                    // Van een kanaal zegt Apple zelf dat hij nieuw is; anders vonden wij hem net.
                    if (show.viaChannel) show.episodes?.let { if (it == 1) "1 afl." else "$it afl." }
                    else "gevonden ${relativeDay(show.foundOn) ?: "vandaag"}"
                ).joinToString(" · "),
                onClick = { onOpen(show) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FollowedMakerRow(
    maker: FollowedMaker,
    status: MakerStatus?,
    face: List<String>?,
    onOpen: (String) -> Unit,
    onUnfollow: (String) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onOpen(maker.name) },
                    onLongClickLabel = "Niet meer volgen",
                    onLongClick = { menu = true }
                )
                .padding(horizontal = 20.dp)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MakerTile(face ?: status?.artworks.orEmpty(), 52.dp)
            Column(Modifier.weight(1f)) {
                Text(maker.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    statusLine(status),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(WoolIcons.ChevronRight, null, tint = LocalChartColors.current.muted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Niet meer volgen") }, onClick = { menu = false; onUnfollow(maker.key) })
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * Hoeveel van zijn podcasts sinds gisteren een nieuwe aflevering hebben. Zonder
 * kanaal zeggen we dat het de gevonden podcasts zijn: meer kent de catalogus niet.
 */
private fun statusLine(status: MakerStatus?): String = when {
    status == null -> "Bijwerken…"
    status.fresh == 0 -> "Geen nieuwe afl. sinds gisteren"
    status.complete -> "Nieuwe afl. bij ${status.fresh} van ${status.total} podcasts"
    else -> "Nieuwe afl. bij ${status.fresh} van ${status.total} gevonden podcasts"
}

@Composable
private fun SuggestionRow(suggestion: MakerSuggestion, face: List<String>?, onOpen: (String) -> Unit, onFollow: (MakerSuggestion) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(suggestion.name) }
            .padding(horizontal = 20.dp)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MakerTile(face ?: suggestion.artworks, 52.dp)
        Column(Modifier.weight(1f)) {
            Text(suggestion.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    suggestion.totalShows != null -> "Je volgt ${suggestion.followedShows} van de ${suggestion.totalShows} podcasts"
                    suggestion.followedShows == 1 -> "Je volgt 1 podcast"
                    else -> "Je volgt ${suggestion.followedShows} podcasts"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Dezelfde volgknop als op de podcast- en makerpagina.
        OutlinePillButton("Volg", WoolIcons.Plus, { onFollow(suggestion) })
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
