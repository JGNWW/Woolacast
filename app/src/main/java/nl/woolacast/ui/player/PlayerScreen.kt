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
    modifier: Modifier = Modifier
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
    val sleeping = state.sleepAtEnd || state.sleepRemainingMs != null

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

                Spacer(Modifier.height(18.dp))
                ScrubBar(
                    progress = progress,
                    buffering = state.isBuffering,
                    enabled = state.durationMs > 0L,
                    accent = cover.accent,
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
                            state.sleepRemainingMs != null -> "${(state.sleepRemainingMs / 60_000L) + 1} min"
                            else -> "Timer"
                        },
                        active = sleeping
                    ) { sheet = Sheet.TIMER }
                    Tool(
                        WoolIcons.Queue,
                        if (queue.isEmpty()) "Wachtrij" else "Wachtrij · ${queue.size}"
                    ) { sheet = Sheet.QUEUE }
                    Tool(WoolIcons.Share, "Delen", onClick = share)
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
            TimerPicker(state) { onSleep(it); sheet = null }
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

        null -> Unit
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

private enum class Sheet { SPEED, TIMER, QUEUE }

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
    accent: Color = MaterialTheme.colorScheme.primary
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
        if (enabled) {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))) {
                    // Het handvat staat gecentreerd op het einde van de vulling.
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 8.dp)
                            .size(16.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
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
private fun Tool(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
private fun TimerPicker(state: PlaybackState, onPick: (SleepTimer) -> Unit) {
    val running = state.sleepAtEnd || state.sleepRemainingMs != null
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipBox("Uit", selected = !running, onClick = { onPick(SleepTimer.Off) })
        listOf(15, 30, 45, 60).forEach { minutes ->
            FilterChipBox("$minutes min", selected = false, onClick = { onPick(SleepTimer.After(minutes)) })
        }
        FilterChipBox("Einde aflevering", selected = state.sleepAtEnd, onClick = { onPick(SleepTimer.EndOfEpisode) })
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
