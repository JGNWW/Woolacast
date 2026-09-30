package nl.woolacast.ui.maker

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.maker.Placement
import nl.woolacast.domain.MakerShow
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.OutlineCircleButton
import nl.woolacast.ui.common.OutlinePillButton
import nl.woolacast.ui.common.softInk
import nl.woolacast.ui.common.CoverBackdrop
import nl.woolacast.ui.common.GlassIconButton
import nl.woolacast.ui.common.MakerWall
import nl.woolacast.ui.common.rememberCoverColors
import nl.woolacast.ui.common.rememberListScrollPx
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.SectionLabel
import nl.woolacast.ui.common.TextPill
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

/**
 * De pagina van een maker. Een maker heeft geen eigen beeld: bovenaan staat een
 * muur van zijn podcasts, met dezelfde gloed en oplossende rand als de hoes op
 * de podcastpagina. Daaronder al zijn shows, te ordenen op waar ze in de
 * hitlijst staan, op nieuwste aflevering of op naam.
 */
@Composable
fun MakerScreen(
    viewModel: MakerViewModel,
    onBack: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val following by viewModel.following.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val channel = state.maker.channel
    val density = LocalDensity.current

    val listState = rememberLazyListState()
    val scrollPx = rememberListScrollPx(listState)
    // De hoezen in de volgorde waarin ze binnenkomen: bij een kanaal is dat
    // Apple's eigen volgorde van populair naar minder, dus de muur verspringt
    // niet als de plekken in de hitlijst later binnenkomen.
    val covers = remember(state.shows) { state.shows.mapNotNull { it.podcast.artworkUrl }.distinct() }
    val cover = rememberCoverColors(covers.firstOrNull())
    val share: (() -> Unit)? = channel?.url?.let { url ->
        {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, state.maker.name)
                putExtra(Intent.EXTRA_TEXT, "${state.maker.name}\n$url")
            }
            context.startActivity(Intent.createChooser(intent, "Maker delen"))
        }
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Box(modifier = modifier.fillMaxSize()) {
        val ready = !state.loading && state.error == null && state.shows.isNotEmpty()
        if (ready) {
            CoverBackdrop(
                url = covers.firstOrNull(),
                colors = cover,
                scrollPx = scrollPx,
                // Eén hoes: die hoes zelf, zoals bij een podcast. Meer: de muur.
                art = if (covers.size >= 2) { heroModifier -> MakerWall(covers, heroModifier) } else null
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 52.dp)) {
                NoticePanel(
                    title = "Niet geladen",
                    message = state.error.orEmpty(),
                    actionLabel = "Opnieuw proberen",
                    onAction = viewModel::load
                )
            }

            state.shows.isEmpty() -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 52.dp)) {
                NoticePanel(
                    title = "Niets gevonden",
                    message = "Apple kent geen podcasts van ${state.maker.name}."
                )
            }

            else -> LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                // De muur staat erachter; de naam valt over zijn oplossende onderrand.
                item { MakerHeader(state, following, viewModel::toggleFollow, share) }
                item {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MakerSort.entries.forEach { sort ->
                            FilterChipBox(sort.label, selected = state.sort == sort, onClick = { viewModel.setSort(sort) })
                        }
                    }
                }
                item { PlacementLine(state, viewModel::setSource) }
                showRows(state, onOpenPodcast)
            }
        }

        // Voorbij de muur wordt de balk dicht, met de naam erin, zoals op de podcastpagina.
        if (ready) {
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
                        state.maker.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Onzichtbaar is ook stil: de naam staat dan al in de kop.
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .graphicsLayer { alpha = shown() }
                        .statusBarsPadding()
                        .padding(top = 52.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            GlassIconButton(WoolIcons.Back, "Terug", onBack)
        }
    }
    }
}

@Composable
private fun MakerHeader(state: MakerUiState, following: Boolean, onToggleFollow: () -> Unit, onShare: (() -> Unit)?) {
    val soft = softInk()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val k = maxWidth / 390.dp
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = (294 * k).dp)) {
            Text(
                eyebrow(state).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.96.sp),
                color = soft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.maker.name,
                fontFamily = DisplayFamily,
                fontSize = 32.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(18.dp))
            // Dezelfde knoppen als op de podcastpagina: volgen als pil, delen als rondje ernaast.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinePillButton(
                    text = if (following) "Gevolgd" else "Volg maker",
                    icon = if (following) WoolIcons.Check else WoolIcons.Plus,
                    onClick = onToggleFollow,
                    selected = following
                )
                if (onShare != null) OutlineCircleButton(WoolIcons.Share, "Maker delen", onShare)
            }
        }
    }
}

/** "Maker · 31 podcasts" als het kanaal compleet is; anders zeggen we dat het gevonden podcasts zijn. */
private fun eyebrow(state: MakerUiState): String {
    val channel = state.maker.channel
    val count = when {
        channel != null && state.complete -> if (channel.showCount == 1) "1 podcast" else "${channel.showCount} podcasts"
        state.shows.size == 1 -> "1 podcast gevonden"
        else -> "${state.shows.size} podcasts gevonden"
    }
    return "Maker · $count"
}

/** De regel die zegt welke lijst "Populair" is. Tik erop om van bron te wisselen. */
@Composable
private fun PlacementLine(state: MakerUiState, onSource: (SourceId) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val country = state.countryCode
    Box(Modifier.padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = "Andere bron kiezen", role = Role.DropdownList) { open = true }
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val dim = MaterialTheme.colorScheme.onSurfaceVariant
            val strong = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            Text("Plek in", style = MaterialTheme.typography.bodySmall, color = dim)
            Text(state.source.label.substringBefore(' '), style = strong)
            if (country.isNotEmpty()) {
                Text("·", style = MaterialTheme.typography.bodySmall, color = dim)
                Flag(country)
                Text(country.uppercase(), style = strong)
            }
            Text("·", style = MaterialTheme.typography.bodySmall, color = dim)
            Text("Top ${state.placement?.listSize ?: 200}", style = MaterialTheme.typography.bodySmall, color = dim)
            Icon(WoolIcons.ChevronDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SourceId.entries.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "Plek in ${source.label}",
                            fontWeight = if (source == state.source) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { open = false; onSource(source) }
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.showRows(
    state: MakerUiState,
    onOpenPodcast: (String, String?, String) -> Unit
) {
    val ranks = state.placement?.ranks.orEmpty()
    val sorted = when (state.sort) {
        MakerSort.POPULAR -> state.shows.sortedWith(
            compareBy<MakerShow> { ranks[it.podcast.id] ?: Int.MAX_VALUE }
                .thenByDescending { it.latestRelease.orEmpty() }
        )
        MakerSort.RECENT -> state.shows.sortedByDescending { it.latestRelease.orEmpty() }
        MakerSort.NAME -> state.shows.sortedBy { it.podcast.title.lowercase() }
    }

    if (state.sort == MakerSort.POPULAR && state.placement != null) {
        val (inList, outList) = sorted.partition { ranks.containsKey(it.podcast.id) }
        items(inList, key = { it.podcast.id }) { ShowRow(it, state, ranks[it.podcast.id], onOpenPodcast) }
        if (outList.isNotEmpty()) {
            item { SectionLabel("Niet in de Top ${state.placement.listSize} · op nieuwste aflevering") }
            items(outList, key = { it.podcast.id }) { ShowRow(it, state, null, onOpenPodcast) }
        }
    } else {
        items(sorted, key = { it.podcast.id }) { ShowRow(it, state, ranks[it.podcast.id], onOpenPodcast) }
    }

    footnote(state.placement)?.let { text ->
        item {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
            )
        }
    }
}

private fun footnote(placement: Placement?): String? {
    val n = placement?.unmatched ?: 0
    return when {
        n == 1 -> "1 Spotify-show is niet aan Apple te koppelen."
        n > 1 -> "$n Spotify-shows zijn niet aan Apple te koppelen."
        else -> null
    }
}

/** Een rij zoals op het makerscherm van altijd: 72 dp, hoes van 52, chevron. */
@Composable
private fun ShowRow(
    show: MakerShow,
    state: MakerUiState,
    rank: Int?,
    onOpenPodcast: (String, String?, String) -> Unit
) {
    val podcast = show.podcast
    val muted = LocalChartColors.current.muted
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPodcast(podcast.id, podcast.feedUrl, podcast.title) }
                .padding(horizontal = 20.dp)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Artwork(podcast.artworkUrl, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    podcast.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        "Deze podcast".takeIf { podcast.id == state.currentId },
                        relativeDay(show.latestRelease?.take(10))?.let { "Nieuwe afl. $it" },
                        // Bij de show waar je vandaan komt is het genre bekend; zo past de regel.
                        podcast.genre.takeIf { podcast.id != state.currentId }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val source = state.placement?.source?.label?.substringBefore(' ')
            val country = state.placement?.country?.code?.uppercase()
            if (rank != null) {
                TextPill(
                    "#$rank",
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { contentDescription = "plek $rank in $source $country" }
                )
            } else if (state.sort == MakerSort.POPULAR && state.placement != null) {
                Text(
                    "–",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "niet in de lijst" }
                )
            }
            Icon(WoolIcons.ChevronRight, null, tint = muted, modifier = Modifier.size(18.dp))
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
