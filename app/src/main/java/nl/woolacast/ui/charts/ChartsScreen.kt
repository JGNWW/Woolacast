package nl.woolacast.ui.charts

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import nl.woolacast.data.ChartRepository
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.MovementBadge
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.RankNumber
import nl.woolacast.ui.common.SourceChip

@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel,
    repository: ChartRepository,
    onOpenPodcast: (showId: String, feedUrl: String?, countryCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "WOOLACAST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Vernieuwen")
            }
        }

        Text(
            "Hitlijsten",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
        )

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { filtersOpen = true }
                .padding(start = 12.dp, end = 12.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(state.query.country.flag)
            Text(state.query.country.label, style = MaterialTheme.typography.labelLarge)
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                state.query.category.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = "Lijst instellen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.weight(1f))
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        val levels = ChartLevel.entries
        TabRow(
            selectedTabIndex = levels.indexOf(state.query.level),
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            levels.forEach { level ->
                Tab(
                    selected = level == state.query.level,
                    onClick = { viewModel.setLevel(level) },
                    text = { Text(level.label, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }

        val notice = state.notice
        val chart = state.chart

        when {
            notice != null -> NoticePanel(
                title = notice.title,
                message = notice.message,
                actionLabel = notice.suggestion?.label(),
                onAction = notice.suggestion?.let { { viewModel.applySuggestion(it) } }
            )

            chart != null -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
            ) {
                item {
                    val cachedAt = chart.cachedAt
                    Text(
                        text = if (cachedAt != null) {
                            "Geen verbinding · lijst van $cachedAt"
                        } else {
                            chart.updatedLabel.orEmpty()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (cachedAt != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(chart.entries, key = { "${it.rank}-${it.id}" }) { entry ->
                    ChartRow(entry) {
                        entry.showId?.let { id ->
                            onOpenPodcast(id, entry.feedUrl, chart.query.country.code)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
            }
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

@Composable
private fun ChartRow(entry: ChartEntry, onClick: () -> Unit) {
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
        Spacer(Modifier.width(4.dp))
    }
}

private fun Suggestion.label(): String = when (this) {
    Suggestion.ALL_CATEGORIES -> "Toon alle categorieen"
    Suggestion.SWITCH_TO_APPLE -> "Wissel naar Apple Podcasts"
    Suggestion.SWITCH_TO_SHOWS -> "Toon podcasts"
}
