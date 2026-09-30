package nl.woolacast.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.data.transcript.Transcript
import nl.woolacast.player.PlaybackState
import nl.woolacast.player.SKIP_BACK_MS
import nl.woolacast.player.SKIP_FORWARD_MS
import nl.woolacast.ui.common.AccentPlayButton
import nl.woolacast.ui.common.ButtonKind
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.WoolButton
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.clock
import nl.woolacast.ui.common.rememberCoverColors
import nl.woolacast.ui.theme.LocalChartColors

/** Waar de transcriptie van de aflevering die speelt staat. */
sealed interface TranscriptLoad {
    data object Idle : TranscriptLoad
    data object Loading : TranscriptLoad
    data class Ready(val transcript: Transcript) : TranscriptLoad
    data class Failed(val message: String) : TranscriptLoad
}

/**
 * Meelezen: de zin die je hoort licht op en schuift mee naar boven. Tik op een
 * zin en de speler springt ernaartoe. Scroll je zelf, dan stopt het meeschuiven
 * tot je op "Terug naar nu" tikt; anders trekt de tekst aan je vinger.
 */
@Composable
fun TranscriptView(
    state: PlaybackState,
    load: TranscriptLoad,
    onSeekToMs: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxWidth().fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tekst", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Van de maker · ${state.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconAction(WoolIcons.Close, "Tekst sluiten", onClose)
        }
        Spacer(Modifier.height(8.dp))
        when (load) {
            TranscriptLoad.Idle, TranscriptLoad.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is TranscriptLoad.Failed -> Column(
                Modifier.weight(1f).fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("De tekst laadt niet", style = MaterialTheme.typography.titleLarge)
                Text(load.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                WoolButton("Opnieuw proberen", onClick = onRetry, kind = ButtonKind.TONAL)
            }
            is TranscriptLoad.Ready -> Lines(
                transcript = load.transcript,
                positionMs = state.positionMs,
                onSeekToMs = onSeekToMs,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MiniTransport(state, onTogglePlay, onSeekBy)
    }
}

@Composable
private fun Lines(
    transcript: Transcript,
    positionMs: Long,
    onSeekToMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChartColors.current
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    var hit by remember { mutableIntStateOf(0) }
    var following by remember { mutableStateOf(true) }

    val current = transcript.indexAt(positionMs)
    val matches by remember(query, transcript) {
        derivedStateOf {
            val needle = query.trim()
            if (needle.length < 2) emptyList()
            else transcript.lines.indices.filter { transcript.lines[it].text.contains(needle, ignoreCase = true) }
        }
    }

    // Zelf scrollen zet het meeschuiven uit.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) following = false }
    }
    LaunchedEffect(current, following) {
        if (following && current >= 0 && query.isBlank()) {
            listState.animateScrollToItem(current, scrollOffset = -120)
        }
    }
    LaunchedEffect(matches, hit) {
        matches.getOrNull(hit)?.let {
            following = false
            listState.animateScrollToItem(it, scrollOffset = -120)
        }
    }

    Column(modifier) {
        SearchField(
            query = query,
            onQuery = { query = it; hit = 0 },
            count = matches.size,
            index = hit,
            onPrevious = { if (matches.isNotEmpty()) hit = (hit - 1 + matches.size) % matches.size },
            onNext = { if (matches.isNotEmpty()) hit = (hit + 1) % matches.size }
        )
        if (!transcript.timed) {
            Text(
                "Deze tekst heeft geen tijden, dus hij loopt niet mee met de speler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(state = listState, contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 80.dp)) {
                items(transcript.lines.size) { index ->
                    val line = transcript.lines[index]
                    val previousSpeaker = transcript.lines.getOrNull(index - 1)?.speaker
                    val on = index == current
                    val isHit = matches.getOrNull(hit) == index
                    Column {
                        if (line.speaker != null && line.speaker != previousSpeaker) {
                            Text(
                                line.speaker.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.96.sp,
                                color = colors.muted,
                                modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 2.dp)
                            )
                        }
                        Text(
                            highlight(line.text, query, MaterialTheme.colorScheme.tertiaryContainer, isHit),
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on || !transcript.timed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f))
                                .then(
                                    if (transcript.timed) Modifier
                                        .clickable { following = true; onSeekToMs(line.startMs) }
                                        .semantics {
                                            stateDescription = if (on) "Speelt nu" else clock(line.startMs)
                                            onClick("Spring naar ${clock(line.startMs)}") { following = true; onSeekToMs(line.startMs); true }
                                        }
                                    else Modifier
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (transcript.timed && !following) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.panel)
                        .clickable { following = true; query = "" }
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(WoolIcons.ChevronDown, null, tint = colors.onPanel, modifier = Modifier.size(16.dp))
                    Text("Terug naar nu", color = colors.onPanel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQuery: (String) -> Unit,
    count: Int,
    index: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(WoolIcons.Search, null, tint = muted, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Zoek in de tekst", style = MaterialTheme.typography.bodyLarge, color = muted)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Zoek in de tekst" }
            )
        }
        if (query.trim().length >= 2) {
            Text(
                if (count == 0) "Geen" else "${index + 1} van $count",
                style = MaterialTheme.typography.labelLarge,
                color = muted
            )
            IconAction(WoolIcons.ChevronUp, "Vorige treffer", onPrevious, enabled = count > 0, iconSize = 18.dp)
            IconAction(WoolIcons.ChevronDown, "Volgende treffer", onNext, enabled = count > 0, iconSize = 18.dp)
        }
    }
}

/** Zet elke treffer van [query] in een markeerkleur; de gekozen treffer ook onderstreept. */
private fun highlight(text: String, query: String, color: androidx.compose.ui.graphics.Color, chosen: Boolean): AnnotatedString {
    val needle = query.trim()
    if (needle.length < 2) return AnnotatedString(text)
    return buildAnnotatedString {
        var from = 0
        while (true) {
            val at = text.indexOf(needle, from, ignoreCase = true)
            if (at < 0) { append(text.substring(from)); break }
            append(text.substring(from, at))
            withStyle(
                SpanStyle(
                    background = color,
                    textDecoration = if (chosen) androidx.compose.ui.text.style.TextDecoration.Underline else null
                )
            ) { append(text.substring(at, at + needle.length)) }
            from = at + needle.length
        }
    }
}

/** Onder de tekst: terug, afspelen, vooruit en de tijd, zodat je niet terug hoeft naar de speler. */
@Composable
private fun MiniTransport(state: PlaybackState, onTogglePlay: () -> Unit, onSeekBy: (Long) -> Unit) {
    val cover = rememberCoverColors(state.artworkUrl)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "${clock(state.positionMs)} / ${if (state.durationMs > 0) clock(state.durationMs) else "–:––"}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SkipIcon(WoolIcons.Back15, SKIP_BACK_MS, "terug") { onSeekBy(-SKIP_BACK_MS) }
        AccentPlayButton(playing = state.isPlaying, colors = cover, onClick = onTogglePlay, size = 56.dp)
        SkipIcon(WoolIcons.Forward30, SKIP_FORWARD_MS, "vooruit") { onSeekBy(SKIP_FORWARD_MS) }
    }
}

/** Het rondje met het getal erin, net als in de speler. */
@Composable
private fun SkipIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, skipMs: Long, direction: String, onClick: () -> Unit) {
    val seconds = skipMs / 1000
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick)
            .semantics { contentDescription = "$seconds seconden $direction" },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(30.dp))
        Text("$seconds", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
    }
}

