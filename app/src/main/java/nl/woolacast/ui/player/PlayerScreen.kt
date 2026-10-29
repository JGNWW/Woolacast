package nl.woolacast.ui.player

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.domain.Chapter
import nl.woolacast.player.PlaybackState
import nl.woolacast.player.SKIP_BACK_MS
import nl.woolacast.player.SKIP_FORWARD_MS
import nl.woolacast.player.SPEEDS
import nl.woolacast.player.SleepTimer
import nl.woolacast.ui.common.AccentPlayButton
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.CoverBackdrop
import nl.woolacast.ui.common.DownloadUi
import nl.woolacast.ui.common.Equalizer
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.GlassIconButton
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.OutlineCircleButton
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.clock
import nl.woolacast.ui.common.label
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.rememberCoverColors
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.common.softInk
import nl.woolacast.ui.common.speedLabel
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

/** Hoeveel van de eerste kaart onder de speler al te zien is: een teken dat er meer is. */
private val PEEK = 64.dp

/**
 * Het volledige spelerscherm. Bovenaan alles om te luisteren: de hoes, de
 * titel, het hoofdstuk boven een balk die in hoofdstukken is geknipt, transport
 * en één rij gereedschap. Daaronder, een veeg verder: meelezen, de hoofdstukken,
 * wat hierna komt en waar de aflevering over gaat. Wie naar beneden scrolt,
 * houdt bovenin een smalle balk met de hoes en de afspeelknop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlaybackState,
    queue: List<SavedEpisode>,
    isSaved: Boolean,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeed: (Float) -> Unit,
    onSleep: (SleepTimer) -> Unit,
    onToggleSave: () -> Unit,
    onPlayQueued: (SavedEpisode) -> Unit,
    onRemoveQueued: (String) -> Unit,
    onOpenPodcast: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    /** Hoofdstukken van deze aflevering; leeg als de maker er geen meegeeft. */
    chapters: List<Chapter> = emptyList(),
    onSeekToMs: (Long) -> Unit = {},
    /** Heeft de maker een transcriptie meegeleverd? Dan is er een kaart Meelezen. */
    hasTranscript: Boolean = false,
    transcript: TranscriptLoad = TranscriptLoad.Idle,
    onOpenTranscript: () -> Unit = {},
    download: DownloadUi = DownloadUi.None,
    onDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
    /** Speakers en tv's in het netwerk; alleen voor schermafdrukken van buitenaf, anders volgt het scherm zelf. */
    cast: CastRoutes? = null
) {
    val context = LocalContext.current
    val colors = LocalChartColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    // Waar de kaart met hoofdstukken begint en de titel ophoudt, gemeten in de scrollende inhoud.
    var chaptersAt by remember { mutableFloatStateOf(0f) }
    var titleEnd by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    val progress = scrubbing ?: state.progress
    val positionMs = (progress * state.durationMs).toLong()
    val episode = state.episode

    val share = {
        val url = episode?.link ?: episode?.audioUrl
        if (episode != null && url != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, episode.title)
                putExtra(Intent.EXTRA_TEXT, "${episode.title} — ${episode.showTitle}\n$url")
            }
            context.startActivity(Intent.createChooser(intent, "Aflevering delen"))
        }
    }

    val cover = rememberCoverColors(state.artworkUrl)
    val soft = softInk()
    val sleeping = state.sleepAtEnd || state.sleepRemainingMs != null || state.sleepAtMs != null
    val chapterIndex = currentChapter(chapters, positionMs)
    val chapterEnd = chapterIndex?.let { chapterEnd(chapters, it, state.durationMs) }
    val openText = { onOpenTranscript(); sheet = Sheet.TEXT }

    // De kaart Meelezen staat onder de speler, dus de tekst laadt mee zodra die opent.
    LaunchedEffect(episode?.id, hasTranscript) { if (hasTranscript) onOpenTranscript() }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Het eerste blad vult het scherm op een randje na; daar piekt de eerste kaart.
        val page = max(maxHeight - PEEK, 560.dp)
        // De smalle balk komt zodra de titel onder de bovenrand verdwijnt.
        val pinned by remember {
            derivedStateOf { scroll.value > titleEnd - with(density) { 64.dp.toPx() } }
        }

        // Gloed en vervaagde hoes achter alles; de scherpe hoes staat zelf in het blad.
        CoverBackdrop(
            state.artworkUrl, cover,
            scrollPx = { scroll.value.toFloat() },
            showCover = false,
            glowTo = 844f, blurTo = 850f,
            fade = listOf(440f to 0f, 620f to 0.35f, 844f to 0.7f)
        )

        val castRoutes = cast ?: rememberCastRoutes()
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(page)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassIconButton(WoolIcons.ChevronDown, "Speler inklappen", onCollapse)
                    Spacer(Modifier.weight(1f))
                    CastButton(castRoutes)
                    Box {
                        GlassIconButton(WoolIcons.More, "Meer", { menuOpen = true })
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Ga naar podcast") },
                                onClick = { menuOpen = false; onOpenPodcast() }
                            )
                            DropdownMenuItem(
                                text = { Text("Aflevering delen") },
                                onClick = { menuOpen = false; share() }
                            )
                            if (hasTranscript) {
                                DropdownMenuItem(text = { Text("Tekst meelezen") }, onClick = { menuOpen = false; openText() })
                            }
                            when (download) {
                                DownloadUi.None -> DropdownMenuItem(text = { Text("Downloaden") }, onClick = { menuOpen = false; onDownload() })
                                DownloadUi.Done -> DropdownMenuItem(text = { Text("Download verwijderen") }, onClick = { menuOpen = false; onRemoveDownload() })
                                is DownloadUi.Failed -> DropdownMenuItem(text = { Text("Opnieuw downloaden") }, onClick = { menuOpen = false; onDownload() })
                                else -> DropdownMenuItem(
                                    text = { Text("Download stoppen (${download.label()?.lowercase()})") },
                                    onClick = { menuOpen = false; onRemoveDownload() }
                                )
                            }
                        }
                    }
                }

                // De hoes neemt wat er overblijft, tot een vierkant van 320 dp.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 36.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val side = minOf(maxWidth, maxHeight, 320.dp)
                    if (side >= 96.dp) {
                        Artwork(state.artworkUrl, side, corner = 18.dp, elevation = 18.dp)
                    }
                }

                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.onGloballyPositioned { titleEnd = it.positionInRoot().y + scroll.value + it.size.height },
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.title,
                                fontFamily = DisplayFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 29.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.heightIn(min = 32.dp).clickable(onClick = onOpenPodcast),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    listOfNotNull(
                                        state.showTitle.takeIf { it.isNotBlank() },
                                        shortDate(episode?.releaseDate)
                                    ).joinToString(" · "),
                                    fontSize = 14.sp,
                                    color = soft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(WoolIcons.ChevronRight, null, tint = soft, modifier = Modifier.size(14.dp))
                            }
                        }
                        OutlineCircleButton(
                            if (isSaved) WoolIcons.Saved else WoolIcons.Save,
                            if (isSaved) "Niet meer bewaren" else "Bewaren",
                            onToggleSave,
                            filled = isSaved,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Speelt er een speaker of tv, dan staat dat hier; de knop zelf zit bovenin.
                    CastingLabel(castRoutes, Modifier.padding(top = 8.dp))

                    state.chartLabel?.let { label ->
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
                                .clickable(onClick = onOpenPodcast)
                                .padding(start = 10.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(WoolIcons.Bars, null, modifier = Modifier.size(13.dp))
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    state.error?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.fallContainer)
                                .clickable(onClick = onDismissError)
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Icon(WoolIcons.Info, null, tint = colors.onFallContainer, modifier = Modifier.size(18.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onFallContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(WoolIcons.Close, "Sluiten", tint = colors.onFallContainer, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (chapterIndex != null) {
                        Spacer(Modifier.height(8.dp))
                        ChapterLine(
                            index = chapterIndex,
                            count = chapters.size,
                            title = chapters[chapterIndex].title,
                            remainingMs = chapterEnd?.let { (it - positionMs).coerceAtLeast(0L) },
                            onClick = {
                                // Een veeg naar beneden, net boven de kaart met alle hoofdstukken.
                                val above = with(density) { 84.dp.toPx() }
                                scope.launch { scroll.animateScrollTo((chaptersAt - above).toInt().coerceAtLeast(0)) }
                            }
                        )
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }

                    ScrubBar(
                        progress = progress,
                        buffering = state.isBuffering,
                        enabled = state.durationMs > 0L,
                        accent = cover.accent,
                        marks = if (state.durationMs > 0L) chapters.drop(1).map { it.startMs.toFloat() / state.durationMs } else emptyList(),
                        wavy = state.isPlaying && scrubbing == null,
                        onScrub = { scrubbing = it },
                        onScrubEnd = { value ->
                            onSeekTo(value)
                            scrubbing = null
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TimeLabel(clock(positionMs), soft)
                        TimeLabel(
                            if (state.durationMs > 0L) "-" + clock(state.durationMs - positionMs)
                            else if (state.isBuffering) "laden…" else "–:––",
                            soft
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkipButton(WoolIcons.Back15, SKIP_BACK_MS, "terug") { onSeekBy(-SKIP_BACK_MS) }
                        TransportButton(WoolIcons.Previous, "Opnieuw beginnen", 26.dp, onPrevious)
                        AccentPlayButton(
                            playing = state.isPlaying,
                            colors = cover,
                            onClick = onTogglePlay,
                            size = 72.dp
                        )
                        TransportButton(
                            WoolIcons.Next, "Volgende uit wachtrij", 26.dp, onNext,
                            enabled = queue.isNotEmpty()
                        )
                        SkipButton(WoolIcons.Forward30, SKIP_FORWARD_MS, "vooruit") { onSeekBy(SKIP_FORWARD_MS) }
                    }

                    // Eén rij licht gereedschap in plaats van vier even zware rondjes.
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolPill(WoolIcons.Speed, speedLabel(state.speed), active = state.speed != 1f, description = "Snelheid ${speedLabel(state.speed)}") { sheet = Sheet.SPEED }
                        ToolPill(
                            WoolIcons.Timer,
                            when {
                                state.sleepAtEnd -> "Einde afl."
                                state.sleepAtMs != null -> "Einde hfst."
                                state.sleepRemainingMs != null -> "${(state.sleepRemainingMs / 60_000L) + 1} min"
                                else -> "Timer"
                            },
                            active = sleeping,
                            description = when {
                                state.sleepAtEnd -> "Slaaptimer: einde van de aflevering"
                                state.sleepAtMs != null -> "Slaaptimer: einde van dit hoofdstuk"
                                state.sleepRemainingMs != null -> "Slaaptimer: nog ${(state.sleepRemainingMs / 60_000L) + 1} minuten"
                                else -> "Slaaptimer"
                            }
                        ) { sheet = Sheet.TIMER }
                        ToolPill(
                            WoolIcons.Queue,
                            if (queue.isEmpty()) "Wachtrij" else "Wachtrij ${queue.size}",
                            description = if (queue.isEmpty()) "Wachtrij" else "Wachtrij, ${queue.size} afleveringen"
                        ) { sheet = Sheet.QUEUE }
                        ToolPill(WoolIcons.Share, null, description = "Aflevering delen", onClick = share)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Een veeg verder: wat er bij de aflevering hoort, als kaarten.
            Column(
                modifier = Modifier.padding(horizontal = 10.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasTranscript) {
                    ReadAlongCard(load = transcript, positionMs = positionMs, onOpen = openText)
                }
                if (chapters.isNotEmpty()) {
                    ChaptersCard(
                        chapters = chapters,
                        current = chapterIndex,
                        positionMs = positionMs,
                        durationMs = state.durationMs,
                        accent = cover.accent,
                        onPick = { onSeekToMs(it.startMs) },
                        modifier = Modifier.onGloballyPositioned { chaptersAt = it.positionInRoot().y + scroll.value }
                    )
                }
                if (queue.isNotEmpty()) {
                    UpNextCard(
                        queue = queue,
                        onPlay = onPlayQueued,
                        onRemove = onRemoveQueued,
                        onOpenQueue = { sheet = Sheet.QUEUE }
                    )
                }
                episode?.description?.takeIf { it.isNotBlank() }?.let { AboutCard(it) }
                Spacer(Modifier.height(14.dp))
            }
        }

        // Doorgescrold: hoes, titel en afspeelknop blijven bovenin in een smalle balk.
        AnimatedVisibility(
            visible = pinned,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PinnedBar(
                state = state,
                subtitle = chapterIndex?.let { chapters[it].title } ?: state.showTitle,
                progress = progress,
                accent = cover.accent,
                onCollapse = onCollapse,
                onTogglePlay = onTogglePlay,
                onTop = { scope.launch { scroll.animateScrollTo(0) } }
            )
        }
    }
    }

    when (sheet) {
        Sheet.SPEED -> ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
            SheetTitle("Snelheid")
            SpeedPicker(state.speed) { onSpeed(it); sheet = null }
            Spacer(Modifier.height(24.dp))
        }

        Sheet.TIMER -> ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
            SheetTitle("Slaaptimer")
            TimerPicker(state, hasChapters = chapters.isNotEmpty()) { onSleep(it); sheet = null }
            Spacer(Modifier.height(24.dp))
        }

        Sheet.QUEUE -> ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) { SheetTitle(if (queue.isEmpty()) "Wachtrij" else "Wachtrij · ${queue.size}") }
                if (queue.isNotEmpty()) {
                    Text(
                        "Wissen",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = softInk(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { queue.forEach { onRemoveQueued(it.id) } }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
            QueueList(
                state = state,
                accent = cover.accent,
                queue = queue,
                onPlay = { onPlayQueued(it); sheet = null },
                onRemove = onRemoveQueued
            )
            Spacer(Modifier.height(24.dp))
        }

        Sheet.TEXT -> ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            TranscriptView(
                state = state,
                load = transcript,
                onSeekToMs = onSeekToMs,
                onTogglePlay = onTogglePlay,
                onSeekBy = onSeekBy,
                onRetry = onOpenTranscript,
                onClose = { sheet = null }
            )
        }

        null -> Unit
    }
}

/** Het hoofdstuk waar [positionMs] in valt, of null zonder hoofdstukken. */
internal fun currentChapter(chapters: List<Chapter>, positionMs: Long): Int? {
    if (chapters.isEmpty()) return null
    val index = chapters.indexOfLast { it.startMs <= positionMs }
    return index.coerceAtLeast(0)
}

/** Waar hoofdstuk [index] ophoudt: het begin van het volgende, of het einde van de aflevering. */
internal fun chapterEnd(chapters: List<Chapter>, index: Int, durationMs: Long): Long? =
    chapters.getOrNull(index + 1)?.startMs ?: durationMs.takeIf { it > 0L }

/**
 * Het huidige hoofdstuk boven de balk: "3/5", de titel en hoe lang het nog
 * duurt. Tikken scrolt naar de kaart met alle hoofdstukken.
 */
@Composable
private fun ChapterLine(index: Int, count: Int, title: String, remainingMs: Long?, onClick: () -> Unit) {
    val soft = softInk()
    val spoken = "Hoofdstuk ${index + 1} van $count, $title" +
        (remainingMs?.let { ", nog ${it / 60_000} minuten ${(it / 1000) % 60} seconden" } ?: "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Hoofdstukken tonen", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("${index + 1}/$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = soft)
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (remainingMs != null) {
            Text("nog ${clock(remainingMs)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = soft)
        }
    }
}

/** Een kaart onder de speler: zelfde vlak, hoek en kop voor elk onderdeel. */
@Composable
private fun PlayerCard(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.96.sp,
                color = LocalChartColors.current.muted,
                modifier = Modifier.weight(1f)
            )
            if (action != null) {
                Text(
                    action,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }
        Column(Modifier.padding(end = 8.dp), content = content)
    }
}

/**
 * Meelezen in het klein: de zin die je hoort, met die ervoor en erna. Tikken
 * opent de volledige tekst, met zoeken en springen.
 */
@Composable
private fun ReadAlongCard(load: TranscriptLoad, positionMs: Long, onOpen: () -> Unit) {
    val muted = LocalChartColors.current.muted
    val soft = MaterialTheme.colorScheme.onSurfaceVariant
    PlayerCard("MEELEZEN", action = "Alles", onAction = onOpen, onClick = onOpen) {
        when (load) {
            TranscriptLoad.Idle, TranscriptLoad.Loading -> Text("Tekst laden…", fontSize = 15.sp, color = soft)
            is TranscriptLoad.Failed -> Text("De tekst laadt niet. Tik om het opnieuw te proberen.", fontSize = 15.sp, color = soft)
            is TranscriptLoad.Ready -> {
                val lines = load.transcript.lines
                val at = load.transcript.indexAt(positionMs)
                val from = if (at >= 0) at - 1 else 0
                Text(
                    buildAnnotatedString {
                        (from..from + 2).forEach { i ->
                            val line = lines.getOrNull(i) ?: return@forEach
                            val style = when {
                                at < 0 -> SpanStyle(color = soft)
                                i < at -> SpanStyle(color = muted)
                                i == at -> SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                else -> SpanStyle(color = soft)
                            }
                            withStyle(style) { append(line.text.trim()) }
                            append(" ")
                        }
                    },
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                if (!load.transcript.timed) {
                    Text(
                        "Deze tekst heeft geen tijden, dus hij loopt niet mee.",
                        fontSize = 13.sp,
                        color = soft,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Alle hoofdstukken: wat al voorbij is grijs met een vinkje, wat speelt in de
 * hoeskleur met een eigen balkje, de rest met hoe lang het duurt.
 */
@Composable
private fun ChaptersCard(
    chapters: List<Chapter>,
    current: Int?,
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    onPick: (Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val muted = LocalChartColors.current.muted
    PlayerCard("HOOFDSTUKKEN · ${chapters.size}", modifier = modifier) {
        chapters.forEachIndexed { index, chapter ->
            val on = index == current
            val past = current != null && index < current
            val end = chapterEnd(chapters, index, durationMs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = 0.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) accent.copy(alpha = 0.12f) else Color.Transparent)
                    .selectable(selected = on, onClick = { onPick(chapter) })
                    .semantics { if (on) stateDescription = "Speelt nu" else if (past) stateDescription = "Beluisterd" }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    clock(chapter.startMs),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) accent else muted,
                    modifier = Modifier.width(46.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        chapter.title,
                        fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            on -> accent
                            past -> muted
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (on && end != null && end > chapter.startMs) {
                        val done = ((positionMs - chapter.startMs).toFloat() / (end - chapter.startMs)).coerceIn(0f, 1f)
                        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(accent.copy(alpha = 0.25f))) {
                            Box(Modifier.fillMaxWidth(done).height(3.dp).background(accent))
                        }
                    }
                }
                when {
                    on && end != null -> Text("nog ${clock((end - positionMs).coerceAtLeast(0L))}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
                    past -> Icon(WoolIcons.Check, null, tint = muted, modifier = Modifier.size(16.dp))
                    end != null -> Text("${((end - chapter.startMs) / 60_000L).coerceAtLeast(1L)} min", fontSize = 13.sp, color = muted)
                }
            }
        }
    }
}

/** Hierna: de eerste drie uit de wachtrij; de rest en wissen zitten achter "Wachtrij". */
@Composable
private fun UpNextCard(
    queue: List<SavedEpisode>,
    onPlay: (SavedEpisode) -> Unit,
    onRemove: (String) -> Unit,
    onOpenQueue: () -> Unit
) {
    val muted = LocalChartColors.current.muted
    PlayerCard("HIERNA · ${queue.size}", action = "Wachtrij", onAction = onOpenQueue) {
        queue.take(3).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Nu afspelen") { onPlay(item) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Artwork(item.artworkUrl, 44.dp, corner = 8.dp, elevation = 0.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(item.showTitle.takeIf { it.isNotBlank() }, minutes(item.durationMillis)).joinToString(" · "),
                        fontSize = 13.sp,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconAction(WoolIcons.Close, "Uit wachtrij", { onRemove(item.id) }, tint = muted, iconSize = 18.dp)
            }
        }
        if (queue.size > 3) {
            Text(
                "en nog ${queue.size - 3}",
                fontSize = 13.sp,
                color = muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Waar de aflevering over gaat, in zes regels; tikken klapt de rest uit. */
@Composable
private fun AboutCard(description: String) {
    var open by remember { mutableStateOf(false) }
    PlayerCard(
        "OVER DEZE AFLEVERING",
        action = if (open) "Minder" else "Meer",
        onAction = { open = !open },
        onClick = { open = !open }
    ) {
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (open) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** De smalle balk bovenin als je onder de speler zit: hoes, titel, en afspelen met een ring als voortgang. */
@Composable
private fun PinnedBar(
    state: PlaybackState,
    subtitle: String,
    progress: Float,
    accent: Color,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onTop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
            .clickable(onClickLabel = "Terug naar de speler", onClick = onTop)
            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconAction(WoolIcons.ChevronDown, "Speler inklappen", onCollapse)
        Artwork(state.artworkUrl, 40.dp, corner = 8.dp, elevation = 0.dp)
        Column(Modifier.weight(1f)) {
            Text(state.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RingPlayButton(playing = state.isPlaying, progress = progress, accent = accent, onClick = onTogglePlay)
    }
}

/** Afspelen of pauzeren, met de voortgang als ring eromheen. */
@Composable
private fun RingPlayButton(playing: Boolean, progress: Float, accent: Color, onClick: () -> Unit) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(42.dp)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(accent, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Icon(
            if (playing) WoolIcons.Pause else WoolIcons.Play,
            if (playing) "Pauzeren" else "Afspelen",
            modifier = Modifier.size(20.dp)
        )
    }
}

private enum class Sheet { SPEED, TIMER, QUEUE, TEXT }

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun TimeLabel(text: String, color: Color) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
}

/**
 * De voortgangsbalk, in stukken geknipt waar een hoofdstuk begint. De vulling
 * golft in het stuk dat speelt zolang de aflevering loopt (zoals Material 3
 * Expressive); gepauzeerd is ze recht. Slepen en tikken zoeken beide.
 */
@Composable
fun ScrubBar(
    progress: Float,
    buffering: Boolean,
    enabled: Boolean,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    /** Plekken (0–1) waar een hoofdstuk begint; daar krijgt de balk een opening. */
    marks: List<Float> = emptyList(),
    /** Golft de vulling in het stuk dat speelt? */
    wavy: Boolean = false
) {
    var width by remember { mutableStateOf(1f) }
    var dragValue by remember { mutableStateOf(progress) }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val fill = if (buffering && progress == 0f) MaterialTheme.colorScheme.outline else accent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                width = size.width.toFloat()
                detectTapGestures { offset ->
                    onScrubEnd((offset.x / width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                width = size.width.toFloat()
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragValue = (offset.x / width).coerceIn(0f, 1f)
                        onScrub(dragValue)
                    },
                    onDragEnd = { onScrubEnd(dragValue) },
                    onDragCancel = { onScrubEnd(dragValue) },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        dragValue = (dragValue + delta / width).coerceIn(0f, 1f)
                        onScrub(dragValue)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            val stroke = 4.dp.toPx()
            val cap = stroke / 2
            val gap = 4.dp.toPx()
            val y = size.height / 2
            val at = size.width * progress.coerceIn(0f, 1f)
            val bounds = listOf(0f) + marks.filter { it in 0.01f..0.99f }.sorted() + 1f
            bounds.zipWithNext().forEach { (a, b) ->
                // Ronde uiteinden steken een halve lijndikte uit; de opening blijft zo echt open.
                val start = size.width * a + (if (a > 0f) gap / 2 else 0f) + cap
                val end = size.width * b - (if (b < 1f) gap / 2 else 0f) - cap
                if (end <= start) return@forEach
                drawLine(track, Offset(start, y), Offset(end, y), stroke, StrokeCap.Round)
                val filledTo = minOf(end, at)
                if (filledTo < start) return@forEach
                if (wavy && at < end + cap) {
                    drawPath(wave(start, filledTo, y, amplitude = 2.5.dp.toPx(), length = 12.dp.toPx()), fill, style = Stroke(stroke, cap = StrokeCap.Round))
                } else {
                    drawLine(fill, Offset(start, y), Offset(filledTo, y), stroke, StrokeCap.Round)
                }
            }
        }
        if (enabled) {
            // Het handvat is altijd een hele stip: op 0 staat hij links tegen het
            // begin, op 1 rechts tegen het eind, en daartussen schuift hij mee.
            // (Eerst zat hij in een vak zo breed als de vulling; aan het begin
            // werd hij daardoor tot een streepje geperst.)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val thumb = 16.dp
                Box(
                    Modifier
                        .offset(x = (maxWidth - thumb) * progress.coerceIn(0f, 1f))
                        .size(thumb)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

/** Een golflijn van [from] tot [to] rond hoogte [y]: halve golven als kwadratische bogen. */
private fun wave(from: Float, to: Float, y: Float, amplitude: Float, length: Float): Path = Path().apply {
    moveTo(from, y)
    val half = length / 2
    var x = from
    var up = true
    while (x < to) {
        val next = minOf(x + half, to)
        // Een afgebroken laatste halve golf wordt evenredig lager, zodat hij netjes uitloopt.
        val peak = amplitude * 2 * ((next - x) / half)
        quadraticTo((x + next) / 2, if (up) y - peak else y + peak, next, y)
        x = next
        up = !up
    }
}

/** Gereedschap als lichte pil; actief = wit vlak, zoals een gekozen chip. Zonder label alleen het icoon. */
@Composable
private fun ToolPill(icon: ImageVector, label: String?, active: Boolean = false, description: String, onClick: () -> Unit) {
    val fg = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .height(44.dp)
            .then(if (label == null) Modifier.width(44.dp) else Modifier)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button }
            .padding(horizontal = if (label == null) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        if (label != null) {
            Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Het rondje-met-pijl plus het getal erin, zoals 15 en 30 in de mockup. Het
 * getal en wat de schermlezer voorleest komen allebei uit de sprong zelf, zodat
 * ze niet uit elkaar kunnen lopen.
 */
@Composable
private fun SkipButton(icon: ImageVector, skipMs: Long, direction: String, onClick: () -> Unit) {
    val seconds = skipMs / 1000
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, "$seconds seconden $direction", modifier = Modifier.size(32.dp))
        Text(
            "$seconds",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedPicker(current: Float, onPick: (Float) -> Unit) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SPEEDS.forEach { speed ->
            FilterChipBox(speedLabel(speed), selected = speed == current, onClick = { onPick(speed) })
        }
    }
    Text(
        "Spraak blijft verstaanbaar; de toonhoogte verandert niet mee.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerPicker(state: PlaybackState, hasChapters: Boolean, onPick: (SleepTimer) -> Unit) {
    val running = state.sleepAtEnd || state.sleepRemainingMs != null || state.sleepAtMs != null
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipBox("Uit", selected = !running, onClick = { onPick(SleepTimer.Off) })
        listOf(15, 30, 45, 60).forEach { minutes ->
            FilterChipBox("$minutes min", selected = false, onClick = { onPick(SleepTimer.After(minutes)) })
        }
        if (hasChapters) {
            FilterChipBox("Einde hoofdstuk", selected = state.sleepAtMs != null, onClick = { onPick(SleepTimer.EndOfChapter) })
        }
        FilterChipBox("Einde aflevering", selected = state.sleepAtEnd, onClick = { onPick(SleepTimer.EndOfEpisode) })
    }
    state.sleepAtMs?.let { at ->
        Text(
            "Stopt op ${clock(at)}, aan het eind van dit hoofdstuk. Spring je naar een ander hoofdstuk, dan telt dat einde.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
    state.sleepRemainingMs?.let { remaining ->
        Text(
            "Stopt over ${clock(remaining)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun QueueList(
    state: PlaybackState,
    accent: Color,
    queue: List<SavedEpisode>,
    onPlay: (SavedEpisode) -> Unit,
    onRemove: (String) -> Unit
) {
    val muted = LocalChartColors.current.muted
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (state.hasEpisode) {
            QueueLabel("NU")
            // Wat nu speelt: hoeskleur en equalizer, net als op de podcastpagina.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .padding(start = 20.dp, end = 26.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Artwork(state.artworkUrl, 48.dp, corner = 8.dp, elevation = 0.dp)
                Column(Modifier.weight(1f)) {
                    Text(state.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(
                            state.showTitle.takeIf { it.isNotBlank() },
                            if (state.durationMs > 0L) nl.woolacast.ui.common.remaining(state.remainingMs) else null
                        ).joinToString(" · "),
                        fontSize = 14.sp,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Equalizer(accent)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        QueueLabel("HIERNA")
        if (queue.isEmpty()) {
            Text(
                "Niets in de wachtrij. Zet afleveringen erin vanaf een podcastpagina; " +
                    "ze spelen vanzelf door na de huidige.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        queue.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(item) }
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Artwork(item.artworkUrl, 48.dp, corner = 8.dp, elevation = 0.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(item.showTitle.takeIf { it.isNotBlank() }, minutes(item.durationMillis)).joinToString(" · "),
                        fontSize = 14.sp,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconAction(WoolIcons.Close, "Uit wachtrij", { onRemove(item.id) }, tint = muted, iconSize = 18.dp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun QueueLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.96.sp,
        color = LocalChartColors.current.muted,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp)
    )
}
