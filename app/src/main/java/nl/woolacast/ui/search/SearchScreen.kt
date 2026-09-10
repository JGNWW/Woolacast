package nl.woolacast.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.theme.LocalChartColors

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    onPlay: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val muted = LocalChartColors.current.muted

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconAction(WoolIcons.Back, "Terug", onBack)
            Spacer(Modifier.size(4.dp))
            // Het zoekveld uit de mockup: 50 dp, papier, dunne rand.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .height(50.dp)
                    .padding(start = 15.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(WoolIcons.Search, null, tint = muted, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (state.term.isEmpty()) {
                        Text(
                            "Zoek podcasts en afleveringen",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                            color = muted
                        )
                    }
                    BasicTextField(
                        value = state.term,
                        onValueChange = viewModel::onTermChanged,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                }
                if (state.term.isNotEmpty()) {
                    IconAction(WoolIcons.Close, "Wissen", { viewModel.onTermChanged("") }, tint = muted, iconSize = 18.dp, modifier = Modifier.size(36.dp))
                }
            }
        }

        when {
            state.loading && state.results.isEmpty ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            state.searched && state.results.isEmpty -> NoticePanel(
                title = "Niets gevonden",
                message = "Geen podcast of aflevering met \"${state.term.trim()}\". " +
                    "Probeer een kortere zoekterm."
            )

            !state.searched && state.term.length < 2 -> Text(
                "Zoekt in de Apple Podcasts-catalogus van je gekozen land.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (state.results.podcasts.isNotEmpty()) {
                    item { SectionLabel("PODCASTS") }
                    items(state.results.podcasts.size) { index ->
                        val podcast = state.results.podcasts[index]
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
                                    listOfNotNull(podcast.publisher.takeIf { it.isNotBlank() }, podcast.genre).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(WoolIcons.ChevronRight, null, tint = muted, modifier = Modifier.size(18.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                if (state.results.episodes.isNotEmpty()) {
                    item { SectionLabel("AFLEVERINGEN") }
                    items(state.results.episodes.size) { index ->
                        val episode = state.results.episodes[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPodcast(episode.showId, null, episode.showTitle) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Artwork(episode.artworkUrl, 56.dp, corner = 12.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    episode.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    episode.showTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    listOfNotNull(minutes(episode.durationMillis), shortDate(episode.releaseDate)).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = muted
                                )
                            }
                            PlayCircle(onClick = { onPlay(episode) }, modifier = Modifier.padding(top = 6.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
    )
}
