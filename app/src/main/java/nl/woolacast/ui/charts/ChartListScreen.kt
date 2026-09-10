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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.ChartRepository
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.theme.LocalChartColors

/**
 * Eén lijst op zichzelf, zoals het artboard "Top afleveringen": terug, titel,
 * filterknop, en de drie chips die zeggen wat je ziet — bron, land, categorie.
 */
@Composable
fun ChartListScreen(
    viewModel: ChartsViewModel,
    repository: ChartRepository,
    onBack: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, countryCode: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val colors = LocalChartColors.current
    val query = state.query

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissToast()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TitleBar(
                title = when (query.level) {
                    ChartLevel.EPISODES -> "Top afleveringen"
                    ChartLevel.TRENDING -> "Trending"
                    ChartLevel.NEW -> "Nieuw"
                    ChartLevel.SHOWS -> "Top podcasts"
                },
                onBack = onBack
            ) {
                IconAction(WoolIcons.Filter, "Lijst instellen", { filtersOpen = true })
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(dark = true, onClick = { filtersOpen = true }) {
                        Text(query.source.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onPanel)
                    }
                }
                item {
                    FilterChip(dark = false, onClick = { filtersOpen = true }) {
                        Flag(query.country.code)
                        Text(query.country.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                if (!query.category.isAll) {
                    item {
                        FilterChip(dark = false, onClick = viewModel::clearCategory) {
                            Text(query.category.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Icon(WoolIcons.Close, "Categorie wissen", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val chart = state.chart
            val notice = state.notice

            Row(
                modifier = Modifier.padding(horizontal = 20.dp).height(30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.loading && chart == null) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                Text(
                    listOfNotNull(
                        chart?.let { "Top ${it.entries.size}" },
                        chart?.cachedAt?.let { "geen verbinding · lijst van ${shortDate(it)}" }
                            ?: chart?.updatedLabel?.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

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
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                    chartRows(chart, state.resolvingId, onOpenPodcast, viewModel::play)
                }

                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.loading) CircularProgressIndicator()
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter)) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = colors.panel,
                contentColor = colors.onPanel,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    if (filtersOpen) {
        FilterSheet(
            query = query,
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

/** De chips boven de lijst (.fchip): donker voor de bron, accent-container voor land en categorie. */
@Composable
private fun FilterChip(dark: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = LocalChartColors.current
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (dark) colors.panel else MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        content()
    }
}
