package nl.woolacast.ui.player

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import nl.woolacast.ui.common.AccentPlayButton
import nl.woolacast.ui.common.CoverBackdrop
import nl.woolacast.ui.common.Equalizer
import nl.woolacast.ui.common.GlassColor
import nl.woolacast.ui.common.GlassIconButton
import nl.woolacast.ui.common.OutlineCircleButton
import nl.woolacast.ui.common.outlineOnGlow
import nl.woolacast.ui.common.rememberCoverColors
import nl.woolacast.ui.common.softInk
import nl.woolacast.ui.theme.NightInk
import nl.woolacast.ui.theme.NightInkSoft
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import nl.woolacast.domain.Chapter
import nl.woolacast.ui.common.DownloadUi
import nl.woolacast.ui.common.label
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.player.PlaybackState
import nl.woolacast.player.SKIP_BACK_MS
import nl.woolacast.player.SKIP_FORWARD_MS
import nl.woolacast.player.SPEEDS
import nl.woolacast.player.SleepTimer
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.clock
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.common.speedLabel
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

/**
 * Het volledige spelerscherm, zoals in de mockup: artwork, titel, waar de
 * aflevering vandaan komt, de balk, transport en de vijf gereedschappen.
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
    /** Heeft de maker een transcriptie meegeleverd? Dan is er een knop Tekst. */
    hasTranscript: Boolean = false,
    transcript: TranscriptLoad = TranscriptLoad.Idle,
    onOpenTranscript: () -> Unit = {},
    download: DownloadUi = DownloadUi.None,
    onDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalChartColors.current
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

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

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // De hoes over de volle breedte; zijn kleur loopt door tot onderaan.
        CoverBackdrop(
            state.artworkUrl, cover,
            glowTo = 844f, blurTo = 850f,
            fade = listOf(440f to 0f, 620f to 0.35f, 844f to 0.7f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassIconButton(WoolIcons.ChevronDown, "Speler inklappen", onCollapse)
                // Geen "speelt nu uit"-label: de show staat al onder de titel, en zo
                // blijft de hoes bovenin vrij.
                Spacer(Modifier.weight(1f))
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

            // Wat onder de hoes staat, zakt naar beneden: de titel valt over de
            // oplossende onderrand, hoe hoog het scherm ook is.
            Spacer(Modifier.weight(1f))

            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.title,
                            fontFamily = DisplayFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 31.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
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

                state.chartLabel?.let { label ->
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.5.dp, outlineOnGlow(), RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenPodcast)
                            .padding(start = 12.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(WoolIcons.Bars, null, modifier = Modifier.size(15.dp))
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(12.dp))
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
                    Spacer(Modifier.height(12.dp))
                    ChapterButton(
                        index = chapterIndex,
                        count = chapters.size,
                        title = chapters[chapterIndex].title,
                        remainingMs = chapterEnd?.let { (it - positionMs).coerceAtLeast(0L) },
                        onClick = { sheet = Sheet.CHAPTERS }
                    )
                }

                Spacer(Modifier.height(18.dp))
                ScrubBar(
                    progress = progress,
                    buffering = state.isBuffering,
                    enabled = state.durationMs > 0L,
                    accent = cover.accent,
                    marks = if (state.durationMs > 0L) chapters.drop(1).map { it.startMs.toFloat() / state.durationMs } else emptyList(),
                    onScrub = { scrubbing = it },
                    onScrubEnd = { value ->
                        onSeekTo(value)
                        scrubbing = null
                    }
                )
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TimeLabel(clock(positionMs), soft)
                    TimeLabel(
                        if (state.durationMs > 0L) "-" + clock(state.durationMs - positionMs)
                        else if (state.isBuffering) "laden…" else "–:––",
                        soft
                    )
                }

                Spacer(Modifier.height(12.dp))
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
                        size = 76.dp
                    )
                    TransportButton(
                        WoolIcons.Next, "Volgende uit wachtrij", 26.dp, onNext,
                        enabled = queue.isNotEmpty()
                    )
                    SkipButton(WoolIcons.Forward30, SKIP_FORWARD_MS, "vooruit") { onSeekBy(SKIP_FORWARD_MS) }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Tool(WoolIcons.Speed, speedLabel(state.speed), active = state.speed != 1f) { sheet = Sheet.SPEED }
                    Tool(
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
                    Tool(
                        WoolIcons.Queue,
                        if (queue.isEmpty()) "Wachtrij" else "Wachtrij · ${queue.size}"
                    ) { sheet = Sheet.QUEUE }
                    // Met tekst neemt Tekst de plek van Delen in; delen staat ook in het menu.
                    if (hasTranscript) Tool(WoolIcons.Transcript, "Tekst", onClick = openText)
                    else Tool(WoolIcons.Share, "Delen", onClick = share)
                }
            }

            Spacer(Modifier.height(16.dp))
            queue.firstOrNull()?.let { next ->
                UpNextCard(next, onClick = { sheet = Sheet.QUEUE })
            }
            Spacer(Modifier.height(12.dp))
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

        Sheet.CHAPTERS -> ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
            SheetTitle("Hoofdstukken")
            ChapterList(
                chapters = chapters,
                current = chapterIndex,
                accent = cover.accent,
                onPick = { onSeekToMs(it.startMs); sheet = null }
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
 * Het huidige hoofdstuk boven de balk, als knop: "3/5 · titel", en rechts hoe
 * lang het nog duurt. Tikken opent de lijst.
 */
@Composable
private fun ChapterButton(index: Int, count: Int, title: String, remainingMs: Long?, onClick: () -> Unit) {
    val soft = softInk()
    val spoken = "Hoofdstuk ${index + 1} van $count, $title" +
        (remainingMs?.let { ", nog ${it / 60_000} minuten ${(it / 1000) % 60} seconden" } ?: "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, outlineOnGlow(), RoundedCornerShape(24.dp))
            .clickable(onClickLabel = "Hoofdstukken tonen", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(WoolIcons.Chapters, null, modifier = Modifier.size(16.dp))
        Text(
            "${index + 1}/$count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = soft
        )
        Text(
            title,
            fontSize = 14.sp,
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

@Composable
private fun ChapterList(chapters: List<Chapter>, current: Int?, accent: Color, onPick: (Chapter) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val scroll = rememberScrollState()
    val rowPx = with(LocalDensity.current) { 56.dp.toPx() }
    // Een lange lijst opent bij het hoofdstuk dat speelt.
    LaunchedEffect(Unit) { current?.let { scroll.scrollTo(((it - 2).coerceAtLeast(0) * rowPx).toInt()) } }
    Column(Modifier.verticalScroll(scroll)) {
        chapters.forEachIndexed { index, chapter ->
            val on = index == current
            val past = current != null && index < current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(if (on) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else Color.Transparent)
                    .selectable(selected = on, onClick = { onPick(chapter) })
                    .semantics { if (on) stateDescription = "Speelt nu" }
                    .padding(start = 20.dp, end = 22.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    clock(chapter.startMs),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) accent else muted,
                    modifier = Modifier.width(52.dp)
                )
                Text(
                    chapter.title,
                    fontSize = 15.5.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        on -> MaterialTheme.colorScheme.onSurface
                        past -> muted
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
                if (on) Equalizer(accent)
            }
            HorizontalDivider(modifier = Modifier.padding(start = 86.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** "Hierna": het eerste uit de wachtrij, onderaan de speler. Tikken opent de wachtrij. */
@Composable
private fun UpNextCard(next: SavedEpisode, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(next.artworkUrl, 40.dp, corner = 8.dp, elevation = 0.dp)
        Column(Modifier.weight(1f)) {
            Text(
                "HIERNA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.96.sp,
                color = LocalChartColors.current.muted
            )
            Text(
                listOfNotNull(next.title, next.showTitle.takeIf { it.isNotBlank() }).joinToString(" · "),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(WoolIcons.ChevronUp, "Wachtrij openen", tint = LocalChartColors.current.muted, modifier = Modifier.size(20.dp))
    }
}

private enum class Sheet { SPEED, TIMER, QUEUE, CHAPTERS, TEXT }

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
 * De voortgangsbalk uit de mockup: 5 dp spoor, accentvulling en een rond
 * handvat met een rand in de achtergrondkleur. Slepen en tikken zoeken beide.
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
    /** Plekken (0–1) waar een hoofdstuk begint; daar krijgt de balk een inkeping. */
    marks: List<Float> = emptyList()
) {
    var width by remember { mutableStateOf(1f) }
    var dragValue by remember { mutableStateOf(progress) }

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
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (buffering && progress == 0f) MaterialTheme.colorScheme.outline
                    else accent
                )
        )
        if (marks.isNotEmpty()) {
            // Een inkeping in de achtergrondkleur, zodat hij op vulling en spoor even goed te zien is.
            val notch = MaterialTheme.colorScheme.background
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val gap = 3.dp.toPx()
                marks.filter { it in 0.01f..0.99f }.forEach { at ->
                    drawRect(notch, topLeft = Offset(size.width * at - gap / 2, 0f), size = Size(gap, size.height))
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

/** Gereedschap: omlijnde cirkel met een label eronder; actief = wit vlak, zoals een gekozen chip. */
@Composable
private fun Tool(icon: ImageVector, label: String, active: Boolean = false, description: String? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(if (description != null) Modifier.clearAndSetSemantics { contentDescription = description; role = Role.Button } else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlineCircleButton(icon, null, onClick, filled = active)
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.onSurface else softInk(),
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
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
