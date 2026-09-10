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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.TimeUnit
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel

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

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Terug")
            }
            OutlinedTextField(
                value = state.term,
                onValueChange = viewModel::onTermChanged,
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = { Text("Zoek podcasts en afleveringen") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.term.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onTermChanged("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Wissen")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
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

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (state.results.podcasts.isNotEmpty()) {
                    item { SectionLabel("PODCASTS") }
                    items(state.results.podcasts.size) { index ->
                        val podcast = state.results.podcasts[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenPodcast(podcast.id, podcast.feedUrl, podcast.title)
                                }
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
                                    podcast.publisher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                if (state.results.episodes.isNotEmpty()) {
                    item { SectionLabel("AFLEVERINGEN") }
                    items(state.results.episodes.size) { index ->
                        val episode = state.results.episodes[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(episode) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Artwork(episode.artworkUrl, 52.dp, corner = 12.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    episode.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    listOfNotNull(
                                        episode.showTitle.takeIf { it.isNotBlank() },
                                        episode.durationMillis?.let {
                                            "${TimeUnit.MILLISECONDS.toMinutes(it)} min"
                                        }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Afspelen")
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
