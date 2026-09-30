package nl.woolacast.ui.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import nl.woolacast.ui.common.AccentPlayButton
import nl.woolacast.ui.common.CoverBackdrop
import nl.woolacast.ui.common.Equalizer
import nl.woolacast.ui.common.GlassIconButton
import nl.woolacast.ui.common.OutlineCircleButton
import nl.woolacast.ui.common.OutlinePillButton
import nl.woolacast.ui.common.rememberCoverColors
import nl.woolacast.ui.common.rememberListScrollPx
import nl.woolacast.ui.common.softInk
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Catalog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import nl.woolacast.ui.common.DownloadUi
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.downloadUi
import nl.woolacast.ui.common.label
import nl.woolacast.domain.Episode
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.ButtonKind
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.SourceColumnsHeader
import nl.woolacast.ui.common.SourceDot
import nl.woolacast.ui.common.SquareIconButton
import nl.woolacast.ui.common.TextPill
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.SuggestionRow
import nl.woolacast.ui.common.UnderlineTabs
import nl.woolacast.ui.common.WoolButton
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.remaining
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.tips.OutletMark
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    playingId: String?,
    onBack: () -> Unit,
    onPlay: (Episode, String?) -> Unit,
    onOpenTracker: () -> Unit,
    onOpenMaker: (publisher: String) -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val queued by viewModel.queued.collectAsStateWithLifecycle()
    val stored by viewModel.stored.collectAsStateWithLifecycle()
    val downloadRecords by viewModel.downloadRecords.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val autoDownload by viewModel.autoDownload.collectAsStateWithLifecycle()
    val downloadSettings by viewModel.downloadSettings.collectAsStateWithLifecycle()
    var downloadSheet by remember { mutableStateOf(false) }
    fun downloadState(id: String) = downloadUi(downloadRecords[id], downloadProgress[id], viewModel.waitingForWifi())
    var tab by remember { mutableStateOf(DetailTab.EPISODES) }
    var menuOpen by remember { mutableStateOf(false) }
    var descriptionOpen by remember { mutableStateOf(false) }
    // Een feed van driehonderd afleveringen duwt alles eronder buiten beeld.
    // Twintig is genoeg om te zien wat er speelt; de rest komt op verzoek.
    var allEpisodes by remember(state.podcast?.id) { mutableStateOf(false) }
    var sheetEpisode by remember { mutableStateOf<Episode?>(null) }

    val podcast = state.podcast
    val isFollowed = podcast != null && follows.any { it.id == podcast.id }
    val countryLabel = state.countryCode.uppercase()

    val share = {
        if (podcast != null) {
            val url = "https://podcasts.apple.com/${state.countryCode}/podcast/id${podcast.id}"
                .takeIf { podcast.id.all { c -> c.isDigit() } } ?: podcast.feedUrl
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, podcast.title)
                putExtra(Intent.EXTRA_TEXT, listOfNotNull(podcast.title, podcast.publisher.takeIf { it.isNotBlank() }, url).joinToString("\n"))
            }
            context.startActivity(Intent.createChooser(intent, "Podcast delen"))
        }
    }

    // Het label dat de speler laat zien: eerst de aflevering zelf, anders de show.
    fun labelFor(episode: Episode): String? {
        state.episodeRanks[episode.id]?.let { return "#$it in Top afleveringen $countryLabel" }
        val here = state.positions.filter { it.country == state.countryCode }
            .minWithOrNull(compareBy({ it.genreId != null }, { it.rank }))
            ?: return null
        // Een categorienotering noemen we bij naam; anders lijkt hij van het land.
        val waar = Catalog.categoryOrNull(here.genreId)?.let { "${it.label} $countryLabel" }
            ?: countryLabel
        return "Podcast op #${here.rank} · ${here.source.label} $waar"
    }

    val listState = rememberLazyListState()
    val scrollPx = rememberListScrollPx(listState)
    val cover = rememberCoverColors(podcast?.artworkUrl)
    val soft = softInk()
    val density = LocalDensity.current
    // Waar de tabs staan, gemeten vanaf de bovenkant van de pagina. De kop is
    // per podcast anders lang (media, noteringen, omschrijving); zo loopt de
    // gloed altijd nog een paar afleveringen door, hoe lang de kop ook is.
    var tabsPageY by remember(podcast?.id) { mutableStateOf<Float?>(null) }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Box(modifier = modifier.fillMaxSize()) {

        if (podcast != null && !state.loading && state.error == null) {
            CoverBackdrop(
                podcast.artworkUrl, cover,
                scrollPx = scrollPx,
                extendTo = tabsPageY?.let { with(density) { it.toDp() } + GLOW_INTO_LIST }
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 52.dp)) {
                NoticePanel(
                    title = "Podcast niet geladen",
                    message = state.error.orEmpty(),
                    actionLabel = "Opnieuw proberen",
                    onAction = viewModel::refresh
                )
            }

            podcast != null -> LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                // De hoes staat erachter; de titel valt over zijn oplossende onderrand.
                item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val k = maxWidth / 390.dp
                        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = (294 * k).dp)) {
                            val facts = listOfNotNull(
                                podcast.genre?.takeIf { it.isNotBlank() },
                                state.cadence,
                                podcast.episodeCount?.let { "$it afl." }
                            )
                            if (facts.isNotEmpty()) {
                                Text(
                                    facts.joinToString(" · ").uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.96.sp),
                                    color = soft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(
                                podcast.title,
                                fontFamily = DisplayFamily,
                                fontSize = 32.sp,
                                lineHeight = 35.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            // De maker is een ingang: erop tikken laat alles
                            // zien wat hij uitgeeft.
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 32.dp)
                                    .clickable(enabled = podcast.publisher.isNotBlank()) {
                                        onOpenMaker(podcast.publisher)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    podcast.publisher,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = soft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (podcast.publisher.isNotBlank()) {
                                    Icon(WoolIcons.ChevronRight, null, tint = soft, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinePillButton(
                            text = if (isFollowed) "Gevolgd" else "Volgen",
                            icon = if (isFollowed) WoolIcons.Check else WoolIcons.Plus,
                            onClick = viewModel::toggleFollow,
                            selected = isFollowed
                        )
                        OutlineCircleButton(WoolIcons.Share, "Podcast delen", share)
                        Spacer(Modifier.weight(1f))
                        val newest = state.episodes.firstOrNull()
                        if (newest != null) {
                            AccentPlayButton(
                                playing = false,
                                colors = cover,
                                onClick = { onPlay(newest, labelFor(newest)) },
                                playLabel = "Nieuwste aflevering afspelen"
                            )
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
                        Spacer(Modifier.height(15.dp))
                    }
                }

                if (state.tips.isNotEmpty()) {
                    item {
                        TipBox(state.tips)
                        Spacer(Modifier.height(15.dp))
                    }
                }

                podcast.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item {
                        Column(
                            Modifier
                                .padding(horizontal = 20.dp)
                                .clickable { descriptionOpen = !descriptionOpen }
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                                color = soft,
                                maxLines = if (descriptionOpen) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (descriptionOpen) "Minder" else "Meer",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = cover.accent,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    UnderlineTabs(
                        labels = DetailTab.entries.map { it.label },
                        selected = DetailTab.entries.indexOf(tab),
                        onSelect = { tab = DetailTab.entries[it] },
                        indicator = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val scrolled = scrollPx()
                            if (scrolled != Float.MAX_VALUE) {
                                val y = coordinates.positionInRoot().y + scrolled
                                if (tabsPageY.let { it == null || kotlin.math.abs(it - y) > 2f }) tabsPageY = y
                            }
                        }
                    )
                }

                when (tab) {
                    DetailTab.EPISODES -> {
                        if (state.episodes.isEmpty()) {
                            item {
                                Text(
                                    "Geen afleveringen in de feed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                        val shown =
                            if (allEpisodes) state.episodes.size
                            else minOf(EPISODES_AT_FIRST, state.episodes.size)
                        items(shown, key = { "$it-${state.episodes[it].id}" }) { index ->
                            val episode = state.episodes[index]
                            EpisodeRow(
                                episode = episode,
                                positionMs = progress[episode.id],
                                chartRank = state.episodeRanks[episode.id],
                                countryLabel = countryLabel,
                                playing = playingId == episode.id,
                                accent = cover.accent,
                                inQueue = queued.any { it.id == episode.id },
                                download = downloadState(episode.id),
                                onOpen = { sheetEpisode = episode },
                                onPlay = { onPlay(episode, labelFor(episode)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        if (shown < state.episodes.size) {
                            item {
                                WoolButton(
                                    "Toon meer afleveringen",
                                    onClick = { allEpisodes = true },
                                    kind = ButtonKind.TONAL,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                                )
                            }
                        }
                    }

                    DetailTab.CHARTS -> {
                        if (state.positions.isEmpty()) {
                            item {
                                Text(
                                    "Deze show staat nog niet in de vastgelegde lijsten. Zodra hij ergens noteert, verschijnen zijn posities hier.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        } else {
                            // Een plek in de lijst over alles weegt zwaarder dan
                            // een plek in een categorie, hoe laag het getal daar ook is.
                            val byCountry = state.positions.groupBy { it.country }.toList()
                                .sortedWith(
                                    compareBy(
                                        { row -> row.second.none { it.genreId == null } },
                                        { row -> row.second.minOf { it.rank } }
                                    )
                                )
                            item { SourceColumnsHeader(Modifier.padding(top = 12.dp)) }
                            items(byCountry.size) { index ->
                                val (country, ranks) = byCountry[index]
                                PositionRow(country, ranks.associate { it.source to it })
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            item {
                                WoolButton(
                                    "Volledig verloop",
                                    onClick = onOpenTracker,
                                    kind = ButtonKind.TONAL,
                                    icon = WoolIcons.Bars,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }

                    DetailTab.ABOUT -> item {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                podcast.description ?: "Geen omschrijving in de feed.",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
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

                if (state.similar.isNotEmpty()) {
                    item {
                        SuggestionRow(
                            title = "Lijkt hierop",
                            suggestions = state.similar,
                            onOpen = onOpenPodcast,
                            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }

        // Voorbij de hoes wordt de balk dicht, met de titel erin: anders schuift
        // de pagina onder de klok en de knoppen door.
        if (podcast != null) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val coverPx = with(density) { maxWidth.toPx() }
                val barPx = with(density) { 120.dp.toPx() }
                val shown = {
                    val y = scrollPx()
                    if (y == Float.MAX_VALUE) 1f else ((y - (coverPx - 2 * barPx)) / barPx).coerceIn(0f, 1f)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = shown() }
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .height(52.dp)
                        .padding(start = 66.dp, end = 66.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        podcast.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Een dunne lijn als de balk dicht is.
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .graphicsLayer { alpha = shown() }
                        .statusBarsPadding()
                        .padding(top = 52.dp)
                )
            }
        }

        // Zwevende balk: glazen knoppen boven op de hoes.
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassIconButton(WoolIcons.Back, "Terug", onBack)
            Spacer(Modifier.weight(1f))
            Box {
                GlassIconButton(WoolIcons.More, "Meer", { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Podcast delen") }, onClick = { menuOpen = false; share() })
                    DropdownMenuItem(text = { Text("Vernieuwen") }, onClick = { menuOpen = false; viewModel.refresh() })
                    DropdownMenuItem(
                        text = { Text(autoDownload[podcast?.id]?.let { "Automatisch downloaden: nieuwste $it" } ?: "Automatisch downloaden…") },
                        onClick = { menuOpen = false; downloadSheet = true }
                    )
                    if (state.positions.isNotEmpty()) {
                        DropdownMenuItem(text = { Text("Chart-tracker") }, onClick = { menuOpen = false; onOpenTracker() })
                    }
                }
            }
        }
    }
    }

    if (downloadSheet && podcast != null) {
        ModalBottomSheet(onDismissRequest = { downloadSheet = false }) {
            AutoDownloadSheet(
                title = podcast.title,
                count = autoDownload[podcast.id],
                followed = isFollowed,
                settings = downloadSettings,
                onChange = viewModel::setAutoDownload
            )
        }
    }

    sheetEpisode?.let { episode ->
        ModalBottomSheet(onDismissRequest = { sheetEpisode = null }) {
            EpisodeSheet(
                episode = episode,
                chartRank = state.episodeRanks[episode.id],
                countryLabel = countryLabel,
                inQueue = queued.any { it.id == episode.id },
                isSaved = stored.any { it.id == episode.id },
                positionMs = progress[episode.id],
                onPlay = { sheetEpisode = null; onPlay(episode, labelFor(episode)) },
                onPlayNext = { viewModel.playNext(episode); sheetEpisode = null },
                onQueue = { viewModel.toggleQueue(episode) },
                onSave = { viewModel.toggleSaved(episode) },
                download = downloadState(episode.id),
                onDownload = { viewModel.download(episode) },
                onRemoveDownload = { viewModel.removeDownload(episode.id) },
                onRetryDownload = { viewModel.retryDownload(episode.id) },
                onShare = {
                    val url = episode.link ?: episode.audioUrl
                    if (url != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, episode.title)
                            putExtra(Intent.EXTRA_TEXT, "${episode.title} — ${episode.showTitle}\n$url")
                        }
                        context.startActivity(Intent.createChooser(intent, "Aflevering delen"))
                    }
                }
            )
        }
    }
}

/** Automatisch downloaden voor één show: aan of uit, en hoeveel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoDownloadSheet(
    title: String,
    count: Int?,
    followed: Boolean,
    settings: nl.woolacast.data.local.DownloadSettings,
    onChange: (Int?) -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Text("Downloaden", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 6.dp))
        nl.woolacast.ui.library.SettingSwitch(
            title = "Automatisch downloaden",
            detail = if (followed) "Nieuwe afleveringen van $title staan klaar, ook zonder verbinding."
                     else "Nieuwe afleveringen van $title staan klaar. Je gaat de show daarmee ook volgen.",
            checked = count != null,
            onChange = { on -> onChange(if (on) (count ?: 1) else null) }
        )
        if (count != null) {
            Text(
                "Hoeveel van de nieuwste",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5).forEach { n ->
                    FilterChipBox("$n", selected = count == n, onClick = { onChange(n) })
                }
            }
            Text(
                "Oudere automatische downloads van deze show ruimt de app op, behalve wat je bewaarde, in de wachtrij zette of al begon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        Text(
            (if (settings.wifiOnly) "Alleen op wifi" else "Ook via mobiele data") +
                " · ruimte ${nl.woolacast.ui.common.byteSize(settings.limitMb * 1024L * 1024L)}. Dat stel je in bij Bibliotheek → Gedownload.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

private enum class DetailTab(val label: String) {
    EPISODES("Afleveringen"), CHARTS("Noteringen"), ABOUT("Over")
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    positionMs: Long?,
    chartRank: Int?,
    countryLabel: String,
    playing: Boolean,
    accent: Color,
    inQueue: Boolean,
    download: DownloadUi,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val colors = LocalChartColors.current
    val soft = softInk()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
                // Wat nu speelt krijgt de hoeskleur, net als in de wachtrij.
                color = if (playing) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (playing) Equalizer(accent)
                val duration = episode.durationMillis
                val started = positionMs != null && duration != null && duration > 0L
                Text(
                    listOfNotNull(
                        shortDate(episode.releaseDate),
                        if (started) null else minutes(duration),
                        if (episode.audioUrl == null) "geen audio" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = soft,
                    maxLines = 1
                )
                // Waar je gebleven bent, maar alleen als je echt begonnen bent.
                if (started) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((positionMs!!.toFloat() / duration!!).coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(accent)
                        )
                    }
                    Text(
                        remaining(duration - positionMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = soft,
                        maxLines = 1
                    )
                }
                if (chartRank != null) {
                    TextPill(
                        "#$chartRank $countryLabel",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (inQueue) {
                    Icon(WoolIcons.QueueAdded, "In wachtrij", tint = colors.muted, modifier = Modifier.size(14.dp))
                }
                when (download) {
                    DownloadUi.None -> Unit
                    DownloadUi.Done -> Icon(WoolIcons.Downloaded, "Gedownload", tint = colors.rise, modifier = Modifier.size(15.dp))
                    is DownloadUi.Failed -> Icon(WoolIcons.Warning, "Download mislukt", tint = colors.fall, modifier = Modifier.size(15.dp))
                    else -> Icon(WoolIcons.Download, download.label(), tint = colors.muted, modifier = Modifier.size(15.dp))
                }
                if (episode.transcript != null) {
                    TextPill("Tekst", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        // Een ring in de hoeskleur; wat al speelt toont pauze.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(2.dp, accent, CircleShape)
                .clickable(enabled = episode.audioUrl != null, onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) WoolIcons.Pause else WoolIcons.Play,
                if (playing) "Pauzeren" else "Afspelen",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Alles over één aflevering, met de acties die niet op de rij passen. */
@Composable
private fun EpisodeSheet(
    episode: Episode,
    chartRank: Int?,
    countryLabel: String,
    inQueue: Boolean,
    isSaved: Boolean,
    positionMs: Long?,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onQueue: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    download: DownloadUi = DownloadUi.None,
    onDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
    onRetryDownload: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(episode.artworkUrl, 64.dp, corner = 13.dp)
            Column(Modifier.weight(1f)) {
                Text(episode.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(episode.showTitle.takeIf { it.isNotBlank() }, shortDate(episode.releaseDate), minutes(episode.durationMillis))
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (chartRank != null) {
            Spacer(Modifier.height(12.dp))
            TextPill("#$chartRank in Top afleveringen $countryLabel", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            WoolButton(
                text = if (positionMs != null) "Verder luisteren" else "Afspelen",
                onClick = onPlay,
                icon = WoolIcons.Play,
                modifier = Modifier.weight(1f),
                enabled = episode.audioUrl != null
            )
            SquareIconButton(WoolIcons.Share, "Delen", onShare)
        }

        // Drie knoppen naast elkaar knipten hun tekst af; als rijen past alles.
        Spacer(Modifier.height(14.dp))
        ActionRow(
            icon = if (inQueue) WoolIcons.QueueAdded else WoolIcons.Queue,
            label = if (inQueue) "Uit de wachtrij halen" else "Aan de wachtrij toevoegen",
            active = inQueue,
            onClick = onQueue
        )
        ActionRow(icon = WoolIcons.Next, label = "Hierna afspelen", active = false, onClick = onPlayNext)
        ActionRow(
            icon = if (isSaved) WoolIcons.Saved else WoolIcons.Save,
            label = if (isSaved) "Bewaard" else "Bewaren",
            active = isSaved,
            onClick = onSave
        )
        if (episode.audioUrl != null) {
            when (download) {
                DownloadUi.None -> ActionRow(WoolIcons.Download, "Downloaden", active = false, onClick = onDownload)
                DownloadUi.Done -> ActionRow(WoolIcons.Downloaded, "Gedownload · tik om te verwijderen", active = true, onClick = onRemoveDownload)
                is DownloadUi.Failed -> ActionRow(WoolIcons.Warning, "Download mislukt · opnieuw proberen", active = false, onClick = onRetryDownload)
                else -> ActionRow(WoolIcons.Download, "${download.label()} · tik om te stoppen", active = false, onClick = onRemoveDownload)
            }
        }
        if (episode.transcript != null || episode.inlineChapters.isNotEmpty() || episode.chaptersUrl != null) {
            Text(
                listOfNotNull(
                    if (episode.inlineChapters.isNotEmpty() || episode.chaptersUrl != null) "hoofdstukken" else null,
                    if (episode.transcript != null) "tekst om mee te lezen" else null
                ).joinToString(" en ", prefix = "Met ", postfix = " van de maker, in de speler."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        episode.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(Modifier.height(18.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        if (active) Icon(WoolIcons.Check, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/**
 * Wat de media over deze podcast schreven, als carrousel. Een regel tekst per
 * medium werd een rijtje linkjes; als kaart is te zien wie het schreef, wanneer,
 * en waar het over ging, en passen er meer in beeld dan drie.
 */
@Composable
private fun TipBox(tips: List<nl.woolacast.data.dataset.MediaTip>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                WoolIcons.News, null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(17.dp)
            )
            Text(
                // Eén kop voor alles wat de media over deze podcast schreven.
                // Of het artikel bij ons is gelezen of alleen als kop langskwam
                // is een verschil in herkomst, geen verschil in categorie.
                "Wat de media zegt",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // De sleutel moet uniek zijn: twee media kunnen dezelfde kop dragen
            // en een artikel zonder link zou twee lege sleutels opleveren.
            itemsIndexed(tips.take(MEDIA_IN_CARROUSEL)) { index, tip ->
                Column(
                    modifier = Modifier
                        .width(228.dp)
                        .height(136.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(tip.url))
                                )
                            }
                        }
                        .padding(13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutletMark(tip.outlet, size = 22.dp, logoUrl = tip.logo, host = tip.host)
                        Text(
                            tip.outlet,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tip.headline,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp, lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    shortDate(tip.date)?.let { datum ->
                        Text(
                            datum,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/** Meer dan dit veegt niemand af; de rest staat in het tabblad Tips. */
private const val MEDIA_IN_CARROUSEL = 10

/** Zoveel afleveringen staan er meteen; daaronder een knop voor de rest. */
private const val EPISODES_AT_FIRST = 20

/** Hoe ver de hoesgloed onder de tabs doorloopt: ongeveer drie afleveringen. */
private val GLOW_INTO_LIST = 260.dp

@Composable
private fun PositionRow(countryCode: String, ranks: Map<SourceId, ShowPositionLike>) {
    val country = Catalog.country(countryCode)
    // Staat een show alleen in een categorielijst, dan is dat een andere
    // notering dan een plek in de lijst over alles. Dat hoort erbij te staan,
    // anders lijkt zesde in Geschiedenis een zesde plek van het land.
    val category = ranks.values.firstNotNullOfOrNull { Catalog.categoryOrNull(it.genreId) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Flag(countryCode, width = 26.dp, height = 18.dp, corner = 4.dp)
        Column(Modifier.weight(1f)) {
            Text(
                country.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (category != null) {
                Text(
                    "in ${category.label}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = LocalChartColors.current.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        listOf(SourceId.APPLE, SourceId.SPOTIFY).forEach { source ->
            val rank = ranks[source]?.rank
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SourceDot(source, active = rank != null)
                Text(
                    rank?.toString() ?: "–",
                    fontFamily = DisplayFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = if (rank != null) MaterialTheme.colorScheme.onSurface else LocalChartColors.current.muted,
                    modifier = Modifier.width(26.dp)
                )
            }
        }
    }
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
    val colors = LocalChartColors.current
    val soft = softInk()
    val line = MaterialTheme.colorScheme.outlineVariant
    val here = positions.groupBy { it.source }
    // Een plek in de lijst over alles gaat voor een plek in een categorie;
    // alleen als er niets anders is telt de categorie.
    val best = listOf(SourceId.APPLE, SourceId.SPOTIFY).mapNotNull { source ->
        here[source]?.minWithOrNull(compareBy({ it.genreId != null }, { it.rank }))?.let { source to it }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick)
    ) {
        HorizontalDivider(color = line)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            best.forEachIndexed { index, (source, position) ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(line))
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = if (index > 0) 16.dp else 0.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        "${position.rank}",
                        fontFamily = DisplayFamily,
                        fontSize = 32.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(source.label.substringBefore(' '))
                            append(' ').append(position.country.uppercase())
                            append(" \u00b7 ")
                            append(Catalog.categoryOrNull(position.genreId)?.label ?: "Alle podcasts")
                        },
                        color = soft,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (best.size < 2) Spacer(Modifier.weight(1f))
        }
        HorizontalDivider(color = line)
        Row(
            Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(WoolIcons.Bars, null, tint = soft, modifier = Modifier.size(14.dp))
            Text(
                if (countryCount == 1) "Noteert in 1 land" else "Noteert in $countryCount landen",
                color = soft,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(WoolIcons.ChevronRight, "Verloop bekijken", tint = soft, modifier = Modifier.size(16.dp))
        }
    }
}

private typealias ShowPositionLike = nl.woolacast.data.dataset.ShowPosition
