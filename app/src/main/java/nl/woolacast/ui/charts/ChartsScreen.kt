package nl.woolacast.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.ChartRepository
import androidx.compose.foundation.lazy.LazyListScope
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.Movement
import nl.woolacast.ui.common.TextPill
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.domain.ChartLevel
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.MarkBar
import nl.woolacast.ui.common.MovementBadge
import nl.woolacast.ui.common.MovementPill
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.SmallChip
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.RankNumber
import nl.woolacast.ui.common.UnderlineTabs
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.theme.LocalChartColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel,
    repository: ChartRepository,
    playingId: String?,
    onOpenPodcast: (showId: String, feedUrl: String?, countryCode: String, title: String) -> Unit,
    onSearch: () -> Unit,
    onAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissToast()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            MarkBar {
                IconAction(WoolIcons.Search, "Zoeken", onSearch)
                IconAction(WoolIcons.Bell, "Chart-alerts", onAlerts)
            }

            // Variant E: kleinere kop met de verversingstijd rechts, drie kleine chips.
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Hitlijsten", style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp, lineHeight = 30.sp))
                Text(
                    when {
                        state.loading -> "Laden…"
                        state.chart?.cachedAt != null -> "Geen verbinding"
                        state.loadedAt != null -> "Bijgewerkt ${state.loadedAt}"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = LocalChartColors.current.muted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            var sourceMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    SmallChip(state.query.source.label.substringBefore(' '), dark = true, onClick = { sourceMenu = true })
                    DropdownMenu(expanded = sourceMenu, onDismissRequest = { sourceMenu = false }) {
                        repository.allSources().forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.id.label) },
                                onClick = { sourceMenu = false; viewModel.setSource(source.id) }
                            )
                        }
                    }
                }
                SmallChip(
                    state.query.country.code.uppercase(),
                    onClick = { filtersOpen = true },
                    leading = { Flag(state.query.country.code) }
                )
                if (state.query.level.isRanking) {
                    SmallChip(
                        if (state.query.category.isAll) "Categorie" else state.query.category.label,
                        onClick = { filtersOpen = true },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            val levels = ChartLevel.entries
            UnderlineTabs(
                labels = levels.map { it.label },
                selected = levels.indexOf(state.query.level),
                onSelect = { viewModel.setLevel(levels[it]) },
                height = 40.dp
            )

            val notice = state.notice
            val chart = state.chart

            PullToRefreshBox(
                isRefreshing = state.loading && chart != null,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    notice != null -> NoticePanel(
                        title = notice.title,
                        message = notice.message,
                        actionLabel = notice.suggestion?.label(),
                        onAction = notice.suggestion?.let { { viewModel.applySuggestion(it) } }
                    )

                    chart != null -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    ) {
                        // Geen verbinding? Dan staat dat erbij; anders begint de lijst meteen.
                        chart.cachedAt?.let { cachedAt ->
                            item {
                                Text(
                                    "Geen verbinding · lijst van ${shortDate(cachedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        if (!chart.query.level.isRanking) {
                            item {
                                Row(
                                    modifier = Modifier.height(30.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (chart.query.level == ChartLevel.TRENDING) WoolIcons.Trend else WoolIcons.Spark,
                                        null, tint = LocalChartColors.current.muted, modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        chart.updatedLabel.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = LocalChartColors.current.muted,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        chartRows(chart, state.resolvingId, onOpenPodcast, viewModel::play)
                    }

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (state.loading) CircularProgressIndicator()
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter)) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = LocalChartColors.current.panel,
                contentColor = LocalChartColors.current.onPanel,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    if (filtersOpen) {
        FilterSheet(
            query = state.query,
            repository = repository,
            onDismiss = { filtersOpen = false },
            onApply = { source, country, category ->
                filtersOpen = false
                viewModel.setSource(source)
                viewModel.setFilters(country, category)
            }
        )
    }
}

/** De rijen van een lijst, per soort: ranglijst, trending (sprong) of nieuw (per dag gegroepeerd). */
internal fun LazyListScope.chartRows(
    chart: Chart,
    resolvingId: String?,
    onOpenPodcast: (showId: String, feedUrl: String?, countryCode: String, title: String) -> Unit,
    onPlay: (ChartEntry) -> Unit
) {
    val level = chart.query.level
    val entries = chart.entries
    var lastDay: String? = null
    entries.forEachIndexed { index, entry ->
        val open = {
            entry.showId?.let { id ->
                // Op afleveringniveau is de titel die van de aflevering;
                // voor het opzoeken van een feed hebben we de show nodig.
                val showTitle = if (level == ChartLevel.EPISODES) entry.publisher else entry.title
                onOpenPodcast(id, entry.feedUrl, chart.query.country.code, showTitle)
            }
            Unit
        }
        if (level == ChartLevel.NEW && entry.enteredOn != null && entry.enteredOn != lastDay) {
            lastDay = entry.enteredOn
            item(key = "day-${entry.enteredOn}") { DayHeader(entry.enteredOn) }
        }
        item(key = "${entry.rank}-${entry.id}") {
            when (level) {
                ChartLevel.EPISODES -> EpisodeChartRow(
                    entry = entry,
                    resolving = resolvingId == entry.id,
                    onClick = open,
                    onPlay = { onPlay(entry) }
                )
                ChartLevel.TRENDING -> TrendingRow(entry, open)
                ChartLevel.NEW -> NewRow(entry, open)
                ChartLevel.SHOWS -> ChartRow(entry, open, compact = true)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DayHeader(day: String) {
    Text(
        (relativeDay(day) ?: day).replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/** Trending: rang, show, en de sprong als pilletje; eronder waar hij nu staat. */
@Composable
private fun TrendingRow(entry: ChartEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RankNumber(entry.rank, size = 22.dp, fontSize = 16.sp)
        Artwork(entry.artworkUrl, 44.dp, corner = 9.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(entry.publisher.takeIf { it.isNotBlank() }, entry.description).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        when (val move = entry.movement) {
            is Movement.Up -> TextPill(
                "▲ ${move.places ?: ""}".trim(),
                MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer
            )
            else -> MovementPill(move)
        }
    }
}

/** Nieuw: geen rang vooraan, wel de plek waarop de show binnenkwam. */
@Composable
private fun NewRow(entry: ChartEntry, onClick: () -> Unit) {
    val colors = LocalChartColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(entry.artworkUrl, 44.dp, corner = 9.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(entry.publisher.takeIf { it.isNotBlank() }, entry.genre).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (entry.enteredOn != null) {
            TextPill("NIEUW OP #${entry.rank}", colors.riseContainer, colors.onRiseContainer)
        } else {
            TextPill("NIEUW", colors.riseContainer, colors.onRiseContainer)
        }
    }
}

@Composable
internal fun ChartRow(entry: ChartEntry, onClick: () -> Unit, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(if (compact) 60.dp else 72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RankNumber(entry.rank, size = if (compact) 22.dp else 26.dp, fontSize = if (compact) 16.sp else 19.sp)
        Artwork(entry.artworkUrl, if (compact) 44.dp else 52.dp, corner = if (compact) 9.dp else 11.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = if (compact) 14.sp else 14.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                entry.publisher,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MovementBadge(entry.movement)
    }
}

/**
 * Een aflevering heeft meer te vertellen dan een show: twee regels titel, van
 * welke podcast hij komt, hoe lang en hoe oud hij is — en hij speelt direct.
 */
@Composable
internal fun EpisodeChartRow(
    entry: ChartEntry,
    resolving: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RankNumber(entry.rank, modifier = Modifier.padding(top = 3.dp), size = 24.dp)
        Artwork(entry.artworkUrl, 56.dp, corner = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                entry.publisher,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                val facts = listOfNotNull(minutes(entry.durationMillis), shortDate(entry.releaseDate))
                    .joinToString(" · ")
                if (facts.isNotEmpty()) {
                    Text(
                        facts,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = LocalChartColors.current.muted
                    )
                }
                MovementPill(entry.movement)
            }
        }
        PlayCircle(onClick = onPlay, loading = resolving, modifier = Modifier.padding(top = 6.dp))
    }
}

internal fun Suggestion.label(): String = when (this) {
    Suggestion.ALL_CATEGORIES -> "Toon alle categorieën"
    Suggestion.SWITCH_TO_APPLE -> "Toon Apple Podcasts-lijst"
    Suggestion.SWITCH_TO_SPOTIFY -> "Toon Spotify's trending-lijst"
    Suggestion.SWITCH_TO_SHOWS -> "Toon podcasts"
}
