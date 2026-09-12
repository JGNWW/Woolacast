package nl.woolacast.ui.maker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.SectionLabel
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.theme.LocalChartColors

/** Alles wat deze maker uitgeeft, in één lijst. */
@Composable
fun MakerScreen(
    viewModel: MakerViewModel,
    onBack: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val muted = LocalChartColors.current.muted

    Column(modifier = modifier.fillMaxSize()) {
        TitleBar(state.publisher.ifBlank { "Maker" }, onBack)
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> NoticePanel(
                title = "Niet geladen",
                message = state.error.orEmpty(),
                actionLabel = "Opnieuw proberen",
                onAction = viewModel::load
            )

            state.shows.isEmpty() -> NoticePanel(
                title = "Niets gevonden",
                message = "Apple kent geen andere podcasts van ${state.publisher}."
            )

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    SectionLabel(
                        if (state.shows.size == 1) "1 PODCAST" else "${state.shows.size} PODCASTS"
                    )
                }
                items(state.shows, key = { it.id }) { podcast ->
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
                                    podcast.genre,
                                    podcast.episodeCount?.let { "$it afl." }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
        }
    }
}
