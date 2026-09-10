package nl.woolacast.ui.discover

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.dataset.DatasetMover
import nl.woolacast.data.dataset.MediaTip
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Category
import nl.woolacast.domain.Country
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.MarkBar
import nl.woolacast.ui.common.PageTitle
import nl.woolacast.ui.common.SectionHeader
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.tips.OutletMark
import nl.woolacast.ui.theme.LocalChartColors

/* De tegelkleuren uit de mockup (de artwork-plaatshouders), rondgedeeld over de categorieën. */
private val TILE_COLORS = listOf(
    Color(0xFFB5482A), Color(0xFF14504E), Color(0xFF332F63), Color(0xFF3C5A2B),
    Color(0xFF7E3149), Color(0xFF2B4C7E), Color(0xFF7E5AA0), Color(0xFF1E1B16),
    Color(0xFFD2825A), Color(0xFFDFA83A)
)
private val TILE_INK = Color(0xFFFBF6EE)
private val TILE_INK_DARK = Color(0xFF211D17)

/**
 * Ontdek is een snelle ingang op de hitlijsten: wie steeg, welke landen er
 * nog meer zijn, en de categorieën — één tik en de lijst staat ingesteld.
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    countryCode: String,
    sourceCount: (Country) -> Int,
    onPick: (Country?, Category?) -> Unit,
    onSearch: () -> Unit,
    onAlerts: () -> Unit,
    onTips: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(countryCode) { viewModel.load(countryCode) }

    Column(modifier = modifier.fillMaxSize()) {
        MarkBar {
            IconAction(WoolIcons.Bell, "Chart-alerts", onAlerts)
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
            item {
                PageTitle("Ontdek", modifier = Modifier.padding(bottom = 0.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable(onClick = onSearch)
                        .height(50.dp)
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(WoolIcons.Search, null, tint = LocalChartColors.current.muted, modifier = Modifier.size(20.dp))
                    Text(
                        "Zoek podcasts en afleveringen",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = LocalChartColors.current.muted
                    )
                }
            }

            if (state.tips.isNotEmpty()) {
                item {
                    SectionHeader(
                        "Tips van de media",
                        action = if (state.tipCount > state.tips.size) "Alle ${state.tipCount}" else "Alles",
                        onAction = onTips
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.tips, key = { it.url + it.showId }) { tip ->
                            TipCard(tip) {
                                tip.showId?.let { onOpenPodcast(it, tip.feedUrl, tip.showTitle.orEmpty()) }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            item {
                SectionHeader("Grootste stijgers", action = "Sinds gisteren")
                if (state.movers.isEmpty()) {
                    Text(
                        if (state.loading) "Stijgers laden…"
                        else "Nog geen stijgers voor ${Catalog.country(countryCode).label}: daarvoor zijn twee vastgelegde dagen nodig.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                } else {
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
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionHeader("Lijsten uit andere landen")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(Catalog.countries.filterNot { it.code == countryCode }, key = { it.code }) { country ->
                        val sources = sourceCount(country)
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .clickable { onPick(country, null) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Flag(country.code, width = 30.dp, height = 21.dp, corner = 5.dp)
                            Column {
                                Text(
                                    country.label,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.1).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Top 200 · $sources ${if (sources == 1) "bron" else "bronnen"}",
                                    fontSize = 11.5.sp,
                                    color = LocalChartColors.current.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item { SectionHeader("Categorieën") }

            val categories = Catalog.categories.filterNot { it.isAll }
            items(categories.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { category ->
                        val colour = TILE_COLORS[categories.indexOf(category) % TILE_COLORS.size]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colour)
                                .clickable { onPick(null, category) }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                category.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.15).sp,
                                color = if (colour.luminance() > 0.45f) TILE_INK_DARK else TILE_INK,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/** Een tip als kaart: de podcast, het citaat uit het artikel, en wie het schreef. */
@Composable
private fun TipCard(tip: MediaTip, onClick: () -> Unit) {
    val colors = LocalChartColors.current
    Column(
        modifier = Modifier
            .width(236.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Artwork(tip.artworkUrl, 48.dp, corner = 11.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    tip.showTitle.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tip.publisher,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            tip.headline,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutletMark(tip.outlet, size = 18.dp, logoUrl = tip.logo)
            Text(
                listOfNotNull(tip.outlet, shortDate(tip.date)).joinToString(" · "),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoverCard(mover: DatasetMover, onClick: () -> Unit) {
    val colors = LocalChartColors.current
    Column(
        modifier = Modifier
            .width(124.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Artwork(mover.artworkUrl, 124.dp, corner = 14.dp)
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(WoolIcons.Up, null, tint = colors.onRiseContainer, modifier = Modifier.size(10.dp))
            Text(
                "${mover.move} → #${mover.rank} ${if (mover.source == "spotify") "Spotify" else "Apple"}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onRiseContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            mover.title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp,
            letterSpacing = (-0.1).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            listOfNotNull(
                mover.publisher.takeIf { it.isNotBlank() },
                mover.genreLabel.takeIf { it.isNotBlank() && !it.startsWith("Alle") }
            ).joinToString(" · "),
            fontSize = 11.5.sp,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
