package nl.woolacast.ui.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.MarkBar
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PageTitle
import nl.woolacast.ui.common.PanelCard
import nl.woolacast.ui.common.PanelRow
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.TextPill
import nl.woolacast.ui.common.UnderlineTabs
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.ui.theme.LocalChartColors

private enum class LibraryTab(val label: String) {
    FOLLOWED("Gevolgd"), MAKERS("Makers"), QUEUE("Wachtrij"), SAVED("Bewaard"), DOWNLOADS("Gedownload")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    countryCode: String,
    playingId: String?,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    onSearch: () -> Unit,
    onPlay: (Episode) -> Unit,
    onOpenMaker: (name: String) -> Unit = {},
    modifier: Modifier = Modifier,
    downloadsViewModel: DownloadsViewModel? = null,
    /** Een gekozen OPML-bestand: het importscherm neemt het over. */
    onImportFile: (Uri) -> Unit = {},
    /** Voor wat langer duurt dan dit scherm: het koppelen van een nieuwe feed aan de catalogus. */
    appScope: CoroutineScope? = null
) {
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val makers by viewModel.makers.collectAsStateWithLifecycle()
    val makerStatus by viewModel.makerStatus.collectAsStateWithLifecycle()
    val makerFaces by viewModel.makerFaces.collectAsStateWithLifecycle()
    val newShows by viewModel.newShows.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val makersRefreshing by viewModel.makersRefreshing.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(LibraryTab.FOLLOWED) }
    var sortOpen by remember { mutableStateOf(false) }
    var showsOpen by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    val addFeed by viewModel.addFeed.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Veel bestandskiezers kennen .opml niet als type; daarom alles tonen en daarna zelf kijken.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportFile(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-opml")) { uri ->
        if (uri != null) viewModel.export(uri, context.contentResolver) { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(addFeed.added) {
        addFeed.added?.let { show ->
            addOpen = false
            viewModel.resetAddFeed()
            onOpenPodcast(show.id, show.feedUrl, show.title)
        }
    }

    LaunchedEffect(countryCode, follows.size) {
        viewModel.loadAlerts(countryCode)
        viewModel.refreshFeeds()
    }
    LaunchedEffect(countryCode, makers.size, follows.size) {
        viewModel.refreshMakers(countryCode)
    }

    Column(modifier = modifier.fillMaxSize()) {
        MarkBar {
            IconAction(WoolIcons.Search, "Zoeken", onSearch)
            IconAction(WoolIcons.More, "Shows importeren of exporteren", { showsOpen = true })
            Box {
                IconAction(WoolIcons.Filter, "Sorteren", { sortOpen = true })
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    LibrarySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { viewModel.setSort(option); sortOpen = false }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    listOf("light" to "Licht thema", "dark" to "Donker thema", "system" to "Thema van het toestel").forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontWeight = if (mode == theme) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { viewModel.setTheme(mode); sortOpen = false }
                        )
                    }
                }
            }
        }
        PageTitle("Bibliotheek")

        UnderlineTabs(
            labels = LibraryTab.entries.map { it.label },
            selected = LibraryTab.entries.indexOf(tab),
            onSelect = { tab = LibraryTab.entries[it] }
        )

        when (tab) {
            LibraryTab.FOLLOWED -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    viewModel.loadAlerts(countryCode)
                    viewModel.refreshFeeds(force = true)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                FollowedTab(
                    follows = when (sort) {
                        LibrarySort.NAME -> follows.sortedBy { it.title.lowercase() }
                        LibrarySort.RECENT -> follows.sortedByDescending { feeds[it.id]?.latestDate ?: "" }
                    },
                    feeds = feeds,
                    alerts = alerts,
                    onOpenPodcast = onOpenPodcast,
                    onUnfollow = viewModel::unfollow
                )
            }
            LibraryTab.MAKERS -> PullToRefreshBox(
                isRefreshing = makersRefreshing,
                onRefresh = { viewModel.refreshMakers(countryCode, force = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                MakersTab(
                    makers = makers,
                    status = makerStatus,
                    faces = makerFaces,
                    newShows = newShows,
                    suggestions = suggestions,
                    onOpenMaker = onOpenMaker,
                    onOpenNewShow = { show ->
                        viewModel.seen(show)
                        onOpenPodcast(show.showId, show.feedUrl, show.title)
                    },
                    onFollow = { viewModel.followSuggestion(it, countryCode) },
                    onUnfollow = viewModel::unfollowMaker
                )
            }
            LibraryTab.QUEUE -> EpisodeTab(
                episodes = queue,
                empty = "Nog niets in de wachtrij. Zet een aflevering erin vanaf een podcastpagina; " +
                    "de speler gaat er vanzelf mee door.",
                playingId = playingId,
                numbered = true,
                onPlay = onPlay,
                onRemove = viewModel::removeFromQueue
            )
            LibraryTab.SAVED -> EpisodeTab(
                episodes = saved,
                empty = "Nog niets bewaard. Bewaar een aflevering om hem hier terug te vinden.",
                playingId = playingId,
                numbered = false,
                onPlay = onPlay,
                onRemove = viewModel::removeSaved
            )
            LibraryTab.DOWNLOADS -> if (downloadsViewModel != null) {
                DownloadsTab(viewModel = downloadsViewModel, playingId = playingId, onPlay = onPlay)
            }
        }
    }

    if (showsOpen) {
        val (exportable, missing) = remember(follows) { viewModel.exportCounts() }
        ModalBottomSheet(
            onDismissRequest = { showsOpen = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            ShowsSheet(
                followCount = exportable + missing,
                notExportable = missing,
                onImport = {
                    showsOpen = false
                    importLauncher.launch(arrayOf("*/*"))
                },
                onAddFeed = {
                    showsOpen = false
                    viewModel.resetAddFeed()
                    addOpen = true
                },
                onExport = {
                    showsOpen = false
                    exportLauncher.launch("toadcast-shows.opml")
                }
            )
        }
    }

    if (addOpen) {
        AddFeedDialog(
            busy = addFeed.busy,
            error = addFeed.error,
            onAdd = { url -> appScope?.let { viewModel.addFeed(url, countryCode, it) } },
            onDismiss = { addOpen = false; viewModel.resetAddFeed() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FollowedTab(
    follows: List<FollowedShow>,
    feeds: Map<String, FeedStatus>,
    alerts: List<ChartAlert>,
    onOpenPodcast: (String, String?, String) -> Unit,
    onUnfollow: (FollowedShow) -> Unit
) {
    if (follows.isEmpty()) {
        NoticePanel(
            title = "Nog niets gevolgd",
            message = "Open een podcast uit een hitlijst en tik op Volgen. " +
                "Wat je volgt komt hier te staan, met wat er nieuw is."
        )
        return
    }
    var menuFor by remember { mutableStateOf<FollowedShow?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (alerts.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                AlertCard(alerts, onOpenPodcast)
            }
        }
        items(follows, key = { it.id }) { show ->
            val status = feeds[show.id]
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .combinedClickable(
                            onClick = { onOpenPodcast(show.id, show.feedUrl, show.title) },
                            onLongClickLabel = "Niet meer volgen",
                            onLongClick = { menuFor = show }
                        )
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        if (show.artworkUrl != null) {
                            AsyncImage(model = show.artworkUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        show.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    when {
                        status == null -> Unit
                        status.newCount > 0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(
                                "${status.newCount} nieuw",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        status.latestDate != null -> Text(
                            "Bijgewerkt ${relativeDay(status.latestDate)}",
                            fontSize = 11.sp,
                            color = LocalChartColors.current.muted
                        )
                    }
                }
                DropdownMenu(expanded = menuFor?.id == show.id, onDismissRequest = { menuFor = null }) {
                    DropdownMenuItem(
                        text = { Text("Niet meer volgen") },
                        onClick = { menuFor = null; onUnfollow(show) }
                    )
                }
            }
        }
    }
}

/** Van de shows die je volgt: wie er bewoog. Alleen mogelijk dankzij de dagelijkse metingen. */
@Composable
private fun AlertCard(alerts: List<ChartAlert>, onOpenPodcast: (String, String?, String) -> Unit) {
    val colors = LocalChartColors.current
    PanelCard("Chart-alerts", "Sinds gisteren", Modifier.padding(bottom = 4.dp)) {
        alerts.forEach { alert ->
            PanelRow(
                artworkUrl = alert.artworkUrl,
                title = alert.title,
                subtitle = when {
                    alert.reachedTop -> "Bereikte #1 · ${alert.source} ${alert.country}"
                    alert.isNew -> "Nieuw binnen op #${alert.rank} · ${alert.source} ${alert.country}"
                    else -> "Nu #${alert.rank} · ${alert.source} ${alert.country}"
                },
                onClick = { onOpenPodcast(alert.showId, alert.feedUrl, alert.title) }
            ) {
                if (alert.reachedTop) {
                    TextPill("TOP", MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary)
                } else {
                    Row(
                        modifier = Modifier
                            .height(19.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.rise.copy(alpha = 0.18f))
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(WoolIcons.Up, null, tint = colors.rise, modifier = Modifier.size(9.dp))
                        Text("${alert.move}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = colors.rise)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeTab(
    episodes: List<SavedEpisode>,
    empty: String,
    playingId: String?,
    numbered: Boolean,
    onPlay: (Episode) -> Unit,
    onRemove: (String) -> Unit
) {
    if (episodes.isEmpty()) {
        NoticePanel(title = "Leeg", message = empty)
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp, horizontal = 20.dp)) {
        items(episodes.size, key = { episodes[it].id }) { index ->
            val episode = episodes[index]
            val playing = episode.id == playingId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(episode.toEpisode()) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (numbered) {
                    Text(
                        "${index + 1}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LocalChartColors.current.muted,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
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
                            minutes(episode.durationMillis)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PlayCircle(onClick = { onPlay(episode.toEpisode()) }, playing = playing)
                IconAction(
                    WoolIcons.Close, "Verwijderen", { onRemove(episode.id) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, iconSize = 18.dp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
