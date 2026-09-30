package nl.woolacast.ui.maker

import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import nl.woolacast.ui.common.MakerGlow
import nl.woolacast.ui.common.MakerLogo
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.SectionLabel
import nl.woolacast.ui.common.TextPill
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.relativeDay
import nl.woolacast.ui.theme.LocalChartColors

/**
 * De pagina van een maker: logo en kleur als Apple een kanaal kent, en al zijn
 * shows, te ordenen op waar ze in de hitlijst staan, op nieuwste aflevering of
 * op naam.
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

    val listState = rememberLazyListState()
    // Voorbij de kop staat de naam in de balk, zoals de titel op de podcastpagina.
    val titleShown by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
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

    Box(modifier = modifier.fillMaxSize()) {
        MakerGlow(channel?.color)
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box {
                TitleBar(null, onBack)
                Text(
                    state.maker.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.5.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 60.dp, end = 20.dp)
                        .graphicsLayer { alpha = if (titleShown) 1f else 0f }
                )
            }

            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                item { MakerHeader(state, following, viewModel::toggleFollow, share) }

                when {
                    state.loading -> item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    state.error != null -> item {
                        NoticePanel(
                            title = "Niet geladen",
                            message = state.error.orEmpty(),
                            actionLabel = "Opnieuw proberen",
                            onAction = viewModel::load
                        )
                    }

                    state.shows.isEmpty() -> item {
                        NoticePanel(
                            title = "Niets gevonden",
                            message = "Apple kent geen podcasts van ${state.maker.name}."
                        )
                    }

                    else -> {
                        item {
                            Row(
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
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
            }
        }
    }
}

@Composable
private fun MakerHeader(state: MakerUiState, following: Boolean, onToggleFollow: () -> Unit, onShare: (() -> Unit)?) {
    val channel = state.maker.channel
    val soft = softInk()
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MakerLogo(state.maker.name, channel?.logoUrl, channel?.color, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "MAKER",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.96.sp),
                    color = soft
                )
                Text(
                    state.maker.name,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp, lineHeight = 30.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                countLine(state)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = soft)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
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

/** "31 podcasts" als het kanaal compleet is; anders zeggen we waar het getal vandaan komt. */
private fun countLine(state: MakerUiState): String? {
    val channel = state.maker.channel
    return when {
        channel != null && state.complete -> if (channel.showCount == 1) "1 podcast" else "${channel.showCount} podcasts"
        state.loading -> null
        state.shows.size == 1 -> "1 gevonden in de Apple-catalogus"
        else -> "${state.shows.size} gevonden in de Apple-catalogus"
    }
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
