package nl.woolacast.ui.detail

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Catalog
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
import nl.woolacast.ui.common.UnderlineTabs
import nl.woolacast.ui.common.WoolButton
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.remaining
import nl.woolacast.ui.common.shortDate
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val queued by viewModel.queued.collectAsStateWithLifecycle()
    val stored by viewModel.stored.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(DetailTab.EPISODES) }
    var menuOpen by remember { mutableStateOf(false) }
    var descriptionOpen by remember { mutableStateOf(false) }
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
        val here = state.positions.filter { it.country == state.countryCode }.minByOrNull { it.rank }
            ?: return null
        return "Podcast op #${here.rank} · ${here.source.label} $countryLabel"
    }

    Column(modifier = modifier.fillMaxSize()) {

        TitleBar(title = null, onBack = onBack) {
            Box {
                IconAction(WoolIcons.More, "Meer", { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Podcast delen") }, onClick = { menuOpen = false; share() })
                    DropdownMenuItem(text = { Text("Vernieuwen") }, onClick = { menuOpen = false; viewModel.refresh() })
                    if (state.positions.isNotEmpty()) {
                        DropdownMenuItem(text = { Text("Chart-tracker") }, onClick = { menuOpen = false; onOpenTracker() })
                    }
                }
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> NoticePanel(
                title = "Podcast niet geladen",
                message = state.error.orEmpty(),
                actionLabel = "Opnieuw proberen",
                onAction = viewModel::refresh
            )

            podcast != null -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Artwork(podcast.artworkUrl, 112.dp, corner = 18.dp, elevation = 6.dp)
                        Column(Modifier.weight(1f).padding(bottom = 2.dp)) {
                            Text(
                                podcast.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                podcast.publisher,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val facts = listOfNotNull(
                                podcast.genre?.takeIf { it.isNotBlank() },
                                state.cadence,
                                podcast.episodeCount?.let { "$it afl." }
                            )
                            if (facts.isNotEmpty()) {
                                Spacer(Modifier.height(9.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        WoolIcons.Clock, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        facts.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        WoolButton(
                            text = if (isFollowed) "Gevolgd" else "Volgen",
                            onClick = viewModel::toggleFollow,
                            kind = if (isFollowed) ButtonKind.TONAL else ButtonKind.PRIMARY,
                            icon = if (isFollowed) WoolIcons.Check else WoolIcons.Plus,
                            modifier = Modifier.weight(1f)
                        )
                        val newest = state.episodes.firstOrNull()
                        if (newest != null) {
                            WoolButton(
                                text = "Nieuwste afl.",
                                onClick = { onPlay(newest, labelFor(newest)) },
                                kind = ButtonKind.OUTLINE,
                                icon = WoolIcons.Play
                            )
                        }
                        SquareIconButton(WoolIcons.Share, "Podcast delen", share)
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
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (descriptionOpen) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (descriptionOpen) "Minder" else "Meer",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    UnderlineTabs(
                        labels = DetailTab.entries.map { it.label },
                        selected = DetailTab.entries.indexOf(tab),
                        onSelect = { tab = DetailTab.entries[it] }
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
                        items(state.episodes.size, key = { "$it-${state.episodes[it].id}" }) { index ->
                            val episode = state.episodes[index]
                            EpisodeRow(
                                episode = episode,
                                positionMs = progress[episode.id],
                                chartRank = state.episodeRanks[episode.id],
                                countryLabel = countryLabel,
                                playing = playingId == episode.id,
                                inQueue = queued.any { it.id == episode.id },
                                onOpen = { sheetEpisode = episode },
                                onPlay = { onPlay(episode, labelFor(episode)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
                            val byCountry = state.positions.groupBy { it.country }.toList()
                                .sortedBy { it.second.minOf { p -> p.rank } }
                            item { SourceColumnsHeader(Modifier.padding(top = 12.dp)) }
                            items(byCountry.size) { index ->
                                val (country, ranks) = byCountry[index]
                                PositionRow(country, ranks.associate { it.source to it.rank })
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
            }
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
    inQueue: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val colors = LocalChartColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Artwork(episode.artworkUrl, 56.dp, corner = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    listOfNotNull(
                        shortDate(episode.releaseDate),
                        minutes(episode.durationMillis),
                        if (episode.audioUrl == null) "geen audio" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                    color = colors.muted
                )
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
            }

            // Waar je gebleven bent, maar alleen als je echt begonnen bent.
            val duration = episode.durationMillis
            if (positionMs != null && duration != null && duration > 0L) {
                Spacer(Modifier.height(9.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((positionMs.toFloat() / duration).coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    remaining(duration - positionMs),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        PlayCircle(onClick = onPlay, playing = playing, modifier = Modifier.padding(top = 6.dp))
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
    onShare: () -> Unit
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

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            WoolButton(
                text = if (inQueue) "In wachtrij" else "Wachtrij",
                onClick = onQueue,
                kind = ButtonKind.TONAL,
                icon = if (inQueue) WoolIcons.QueueAdded else WoolIcons.Queue,
                modifier = Modifier.weight(1f)
            )
            WoolButton(
                text = "Hierna",
                onClick = onPlayNext,
                kind = ButtonKind.TONAL,
                icon = WoolIcons.Next,
                modifier = Modifier.weight(1f)
            )
            WoolButton(
                text = if (isSaved) "Bewaard" else "Bewaar",
                onClick = onSave,
                kind = ButtonKind.TONAL,
                icon = if (isSaved) WoolIcons.Saved else WoolIcons.Save,
                modifier = Modifier.weight(1f)
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
private fun PositionRow(countryCode: String, ranks: Map<SourceId, Int>) {
    val country = Catalog.country(countryCode)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Flag(countryCode, width = 26.dp, height = 18.dp, corner = 4.dp)
        Text(
            country.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        listOf(SourceId.APPLE, SourceId.SPOTIFY).forEach { source ->
            val rank = ranks[source]
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
    val here = positions.groupBy { it.source }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(colors.panel)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(WoolIcons.Bars, null, tint = colors.onPanel, modifier = Modifier.size(20.dp))
        var first = true
        listOf(SourceId.APPLE, SourceId.SPOTIFY).forEach { source ->
            val best = here[source]?.minByOrNull { it.rank } ?: return@forEach
            if (!first) {
                Box(Modifier.width(1.dp).height(26.dp).background(colors.onPanel.copy(alpha = 0.18f)))
            }
            first = false
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${best.rank}",
                    color = colors.onPanel,
                    fontFamily = DisplayFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    source.label.substringBefore(' '),
                    color = colors.onPanelMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (countryCount == 1) "1 land" else "$countryCount landen",
            color = colors.onPanelMuted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(WoolIcons.ChevronRight, "Verloop bekijken", tint = colors.onPanelMuted, modifier = Modifier.size(17.dp))
    }
}

private typealias ShowPositionLike = nl.woolacast.data.dataset.ShowPosition
