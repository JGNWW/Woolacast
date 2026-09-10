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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.ChartRepository
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.MarkBar
import nl.woolacast.ui.common.MovementBadge
import nl.woolacast.ui.common.MovementPill
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PageTitle
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.RankNumber
import nl.woolacast.ui.common.SourceChip
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

            PageTitle("Hitlijsten")

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(repository.allSources(), key = { it.id.name }) { source ->
                    SourceChip(
                        source = source.id,
                        selected = source.id == state.query.source,
                        onClick = { viewModel.setSource(source.id) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            ContextBar(
                state = state,
                categoriesAllowed = repository.source(state.query.source).capabilities
                    .supportsCategories(state.query.level),
                onOpen = { filtersOpen = true },
                onClearCategory = viewModel::clearCategory
            )

            Spacer(Modifier.height(2.dp))

            val levels = ChartLevel.entries
            UnderlineTabs(
                labels = levels.map { it.label },
                selected = levels.indexOf(state.query.level),
                onSelect = { viewModel.setLevel(levels[it]) }
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
                        item {
                            val cachedAt = chart.cachedAt
                            Text(
                                text = listOfNotNull(
                                    "Top ${chart.entries.size}",
                                    if (cachedAt != null) "geen verbinding · lijst van ${shortDate(cachedAt)}"
                                    else chart.updatedLabel?.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cachedAt != null) MaterialTheme.colorScheme.primary
                                else LocalChartColors.current.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.height(30.dp).padding(top = 6.dp)
                            )
                        }
                        items(chart.entries, key = { "${it.rank}-${it.id}" }) { entry ->
                            val open = {
                                entry.showId?.let { id ->
                                    // Op afleveringniveau is de titel die van de aflevering;
                                    // voor het opzoeken van een feed hebben we de show nodig.
                                    val showTitle = if (chart.query.level == ChartLevel.EPISODES) {
                                        entry.publisher
                                    } else {
                                        entry.title
                                    }
                                    onOpenPodcast(id, entry.feedUrl, chart.query.country.code, showTitle)
                                }
                                Unit
                            }
                            if (chart.query.level == ChartLevel.EPISODES) {
                                EpisodeChartRow(
                                    entry = entry,
                                    resolving = state.resolvingId == entry.id,
                                    onClick = open,
                                    onPlay = { viewModel.play(entry) }
                                )
                            } else {
                                ChartRow(entry, open)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
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

/** De balk met land · categorie die de filtersheet opent (.ctx). */
@Composable
private fun ContextBar(
    state: ChartsUiState,
    categoriesAllowed: Boolean,
    onOpen: () -> Unit,
    onClearCategory: () -> Unit
) {
    val muted = LocalChartColors.current.muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .height(46.dp)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Flag(state.query.country.code)
        Text(state.query.country.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Text("·", color = muted)
        Text(
            state.query.category.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (categoriesAllowed || state.query.category.isAll) MaterialTheme.colorScheme.onSurfaceVariant else muted,
            textDecoration = if (!categoriesAllowed && !state.query.category.isAll) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!state.query.category.isAll) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClearCategory),
                contentAlignment = Alignment.Center
            ) {
                Icon(WoolIcons.Close, "Categorie wissen", tint = muted, modifier = Modifier.size(14.dp))
            }
        } else {
            Icon(WoolIcons.ChevronDown, "Lijst instellen", tint = muted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.weight(1f))
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
        } else {
            Text(
                state.chart?.updatedLabel?.substringBefore(" · ")?.take(22).orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}

@Composable
internal fun ChartRow(entry: ChartEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RankNumber(entry.rank)
        Artwork(entry.artworkUrl, 52.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyLarge,
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
    Suggestion.SWITCH_TO_SHOWS -> "Toon podcasts"
}
