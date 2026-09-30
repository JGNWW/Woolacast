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
import nl.woolacast.ui.common.ButtonKind
import nl.woolacast.ui.common.MakerLogo
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.SectionLabel
import nl.woolacast.ui.common.WoolButton
import nl.woolacast.ui.common.WoolIcons
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
                FollowedMakerRow(maker, status[maker.key], onOpenMaker, onUnfollow)
            }
        }

        if (suggestions.isNotEmpty()) {
            item { SectionLabel("Van podcasts die je volgt") }
            items(suggestions, key = { "suggestion-${it.key}" }) { suggestion ->
                SuggestionRow(suggestion, onOpenMaker, onFollow)
            }
        }
    }
}

/** Dezelfde kaart als de chart-alerts: donker paneel, bel, rijen van 52 dp. */
@Composable
private fun NewShowsCard(shows: List<MakerNewShow>, onOpen: (MakerNewShow) -> Unit) {
    val colors = LocalChartColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.panel)
            .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(WoolIcons.Bell, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Text("Nieuw van je makers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onPanel)
            Spacer(Modifier.weight(1f))
            Text("Nog niet bekeken", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = colors.onPanelMuted)
        }
        shows.forEach { show ->
            HorizontalDivider(color = colors.onPanel.copy(alpha = 0.13f))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(show) }.height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Artwork(show.artworkUrl, 36.dp, corner = 9.dp, elevation = 0.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        show.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPanel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(
                            "Nieuwe podcast",
                            show.makerName,
                            // Van een kanaal zegt Apple zelf dat hij nieuw is; anders vonden wij hem net.
                            if (show.viaChannel) show.episodes?.let { if (it == 1) "1 afl." else "$it afl." }
                            else "gevonden vandaag"
                        ).joinToString(" · "),
                        fontSize = 11.5.sp,
                        color = colors.onPanelMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FollowedMakerRow(
    maker: FollowedMaker,
    status: MakerStatus?,
    onOpen: (String) -> Unit,
    onUnfollow: (String) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onOpen(maker.name) }, onLongClick = { menu = true })
                .padding(horizontal = 20.dp)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MakerLogo(maker.name, status?.logoUrl ?: maker.logoUrl, status?.color ?: maker.color, 48.dp)
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

/** "2 van 31 shows nieuw sinds gisteren"; zonder kanaal zeggen we dat het de gevonden shows zijn. */
private fun statusLine(status: MakerStatus?): String = when {
    status == null -> "Bijwerken…"
    status.fresh == 0 && status.complete -> "Niets nieuws sinds gisteren"
    status.fresh == 0 -> "Niets nieuws in de ${status.total} gevonden shows"
    status.complete -> "${status.fresh} van ${status.total} shows nieuw sinds gisteren"
    else -> "${status.fresh} van ${status.total} gevonden nieuw sinds gisteren"
}

@Composable
private fun SuggestionRow(suggestion: MakerSuggestion, onOpen: (String) -> Unit, onFollow: (MakerSuggestion) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(suggestion.name) }
            .padding(horizontal = 20.dp)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MakerLogo(suggestion.name, suggestion.logoUrl, suggestion.color, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(suggestion.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    suggestion.totalShows != null -> "Je volgt ${suggestion.followedShows} van hun ${suggestion.totalShows} podcasts"
                    suggestion.followedShows == 1 -> "Je volgt 1 podcast van deze maker"
                    else -> "Je volgt ${suggestion.followedShows} podcasts van deze maker"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        WoolButton("Volg", { onFollow(suggestion) }, kind = ButtonKind.OUTLINE)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
