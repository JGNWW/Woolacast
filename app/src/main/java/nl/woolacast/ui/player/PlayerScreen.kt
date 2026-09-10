package nl.woolacast.ui.player

import android.content.Intent
import androidx.compose.foundation.background
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

private val SPEEDS = listOf(0.8f, 1f, 1.2f, 1.5f, 1.75f, 2f)

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

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconAction(WoolIcons.ChevronDown, "Speler inklappen", onCollapse, iconSize = 24.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SPEELT NU UIT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.showTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box {
                    IconAction(WoolIcons.More, "Meer", { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Ga naar podcast") },
                            onClick = { menuOpen = false; onOpenPodcast() }
                        )
                        DropdownMenuItem(
                            text = { Text("Aflevering delen") },
                            onClick = { menuOpen = false; share() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Niet meer bewaren" else "Bewaren") },
                            onClick = { menuOpen = false; onToggleSave() }
                        )
                    }
                }
            }

            // Statisch, geen scrollen: het artwork krimpt mee op een klein scherm,
            // de rest houdt zijn maat.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(Modifier.height(14.dp))
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val side = minOf(298.dp, maxWidth, maxHeight)
                    Artwork(state.artworkUrl, side, corner = 24.dp, elevation = 18.dp)
                }

                Spacer(Modifier.height(26.dp))
                Text(
                    state.title,
                    fontFamily = DisplayFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.6).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(
                        state.showTitle.takeIf { it.isNotBlank() },
                        shortDate(episode?.releaseDate),
                        minutes(state.durationMs.takeIf { it > 0L } ?: episode?.durationMillis)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                state.chartLabel?.let { label ->
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            WoolIcons.Bars, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(14.dp))
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

                Spacer(Modifier.height(24.dp))
                ScrubBar(
                    progress = progress,
                    buffering = state.isBuffering,
                    enabled = state.durationMs > 0L,
                    onScrub = { scrubbing = it },
                    onScrubEnd = { value ->
                        onSeekTo(value)
                        scrubbing = null
                    }
                )
                Spacer(Modifier.height(9.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TimeLabel(clock(positionMs))
                    TimeLabel(
                        if (state.durationMs > 0L) "-" + clock(state.durationMs - positionMs)
                        else if (state.isBuffering) "laden…" else "–:––"
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkipButton(WoolIcons.Back15, "15", "15 seconden terug") { onSeekBy(-15_000L) }
                    TransportButton(WoolIcons.Previous, "Opnieuw beginnen", 26.dp, onPrevious)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(6.dp, CircleShape, clip = false, ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (state.isPlaying) WoolIcons.Pause else WoolIcons.Play,
                            contentDescription = if (state.isPlaying) "Pauzeren" else "Afspelen",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    TransportButton(
                        WoolIcons.Next, "Volgende uit wachtrij", 26.dp, onNext,
                        enabled = queue.isNotEmpty()
                    )
                    SkipButton(WoolIcons.Forward30, "30", "30 seconden vooruit") { onSeekBy(30_000L) }
                }

                Spacer(Modifier.height(26.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tool(WoolIcons.Speed, speedLabel(state.speed), active = state.speed != 1f) { sheet = Sheet.SPEED }
                    Tool(
                        WoolIcons.Timer,
                        when {
                            state.sleepAtEnd -> "Einde afl."
                            state.sleepRemainingMs != null -> "${(state.sleepRemainingMs / 60_000L) + 1} min"
                            else -> "Timer"
                        },
                        active = state.sleepAtEnd || state.sleepRemainingMs != null
                    ) { sheet = Sheet.TIMER }
                    Tool(
                        WoolIcons.Queue,
                        if (queue.isEmpty()) "Wachtrij" else "Wachtrij · ${queue.size}",
                        active = queue.isNotEmpty()
                    ) { sheet = Sheet.QUEUE }
                    Tool(WoolIcons.Share, "Delen", onClick = share)
                    Tool(
                        if (isSaved) WoolIcons.Saved else WoolIcons.Save,
                        if (isSaved) "Bewaard" else "Bewaar",
                        active = isSaved,
                        onClick = onToggleSave
                    )
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

    when (sheet) {
        Sheet.SPEED -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            SheetTitle("Snelheid")
            SpeedPicker(state.speed) { onSpeed(it); sheet = null }
            Spacer(Modifier.height(24.dp))
        }

        Sheet.TIMER -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            SheetTitle("Slaaptimer")
            TimerPicker(state) { onSleep(it); sheet = null }
            Spacer(Modifier.height(24.dp))
        }

        Sheet.QUEUE -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            SheetTitle(if (queue.isEmpty()) "Wachtrij" else "Wachtrij · ${queue.size}")
            QueueList(
                queue = queue,
                onPlay = { onPlayQueued(it); sheet = null },
                onRemove = onRemoveQueued
            )
            Spacer(Modifier.height(24.dp))
        }

        null -> Unit
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
private fun TimeLabel(text: String) {
    Text(
        text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalChartColors.current.muted
    )
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
    modifier: Modifier = Modifier
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
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    if (buffering && progress == 0f) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary
                )
        )
        if (enabled) {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))) {
                    // Het handvat staat gecentreerd op het einde van de vulling.
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 7.dp)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
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

/** Het rondje-met-pijl plus het getal erin, zoals 15 en 30 in de mockup. */
@Composable
private fun SkipButton(icon: ImageVector, seconds: String, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(32.dp))
        Text(
            seconds,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun Tool(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(21.dp))
        Text(
            label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
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
    queue: List<SavedEpisode>,
    onPlay: (SavedEpisode) -> Unit,
    onRemove: (String) -> Unit
) {
    if (queue.isEmpty()) {
        Text(
            "Niets in de wachtrij. Zet afleveringen erin vanaf een podcastpagina; " +
                "ze spelen vanzelf door na de huidige.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        return
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        queue.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(item) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${index + 1}",
                    fontFamily = DisplayFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LocalChartColors.current.muted,
                    modifier = Modifier.width(18.dp),
                    textAlign = TextAlign.End
                )
                Artwork(item.artworkUrl, 44.dp, corner = 9.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(item.showTitle.takeIf { it.isNotBlank() }, minutes(item.durationMillis)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconAction(WoolIcons.Close, "Uit wachtrij", { onRemove(item.id) }, tint = MaterialTheme.colorScheme.onSurfaceVariant, iconSize = 18.dp)
            }
            if (index < queue.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
