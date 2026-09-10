package nl.woolacast.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Category
import nl.woolacast.domain.Country
import nl.woolacast.data.dataset.DatasetMover
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.theme.LocalChartColors

/**
 * Ontdek is in deze eerste versie een snelle ingang op de hitlijsten: kies een
 * land of een categorie en de lijst staat er meteen op ingesteld.
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    countryCode: String,
    onPick: (Country?, Category?) -> Unit,
    onSearch: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(countryCode) { viewModel.load(countryCode) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Text(
                "WOOLACAST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp)
            )
            Text(
                "Ontdek",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 15.dp)
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Zoek podcasts en afleveringen",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        if (state.movers.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("Grootste stijgers", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Sinds gisteren",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.movers.take(20), key = { it.source + it.id }) { mover ->
                        MoverCard(mover) {
                            mover.showId?.let { onOpenPodcast(it, mover.feedUrl, mover.title) }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        item {
            Text(
                "Lijsten uit andere landen",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Catalog.countries, key = { it.code }) { country ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .clickable { onPick(country, null) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .width(96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(country.flag, fontSize = 24.sp)
                        Text(country.label, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }

        item {
            Text(
                "Categorieen",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)
            )
        }

        items(Catalog.categories.filterNot { it.isAll }.chunked(2)) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { category ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { onPick(null, category) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(category.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MoverCard(mover: DatasetMover, onClick: () -> Unit) {
    val colors = LocalChartColors.current
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Artwork(mover.artworkUrl, 136.dp, corner = 14.dp)
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.ArrowDropUp,
                contentDescription = null,
                tint = colors.rise,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "${mover.move} → #${mover.rank}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colors.rise
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            mover.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            listOfNotNull(
                mover.publisher.takeIf { it.isNotBlank() },
                mover.genreLabel.takeIf { it.isNotBlank() && it != "Alle categorieen" }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
