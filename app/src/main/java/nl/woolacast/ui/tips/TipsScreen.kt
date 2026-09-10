package nl.woolacast.ui.tips

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.dataset.MediaTip
import nl.woolacast.domain.Catalog
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.PlayCircle
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.parseDate
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.theme.LocalChartColors

/**
 * Tips van kranten, omroepen en podcastgidsen, per land. Elke tip wijst naar
 * het artikel én naar de podcast, zodat je meteen kunt luisteren.
 */
@Composable
fun TipsScreen(
    viewModel: TipsViewModel,
    countryCode: String,
    onBack: () -> Unit,
    onPickCountry: () -> Unit,
    onOpenPodcast: (showId: String, feedUrl: String?, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(countryCode) { viewModel.load(countryCode) }
    val country = Catalog.country(state.countryCode.ifEmpty { countryCode })

    Column(modifier = modifier.fillMaxSize()) {
        TitleBar("Tips van de media", onBack)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChipBox(
                    label = country.code.uppercase(),
                    selected = true,
                    onClick = onPickCountry,
                    leading = { Flag(country.code) }
                )
            }
            item {
                FilterChipBox("Alle", selected = state.outlet == null, onClick = { viewModel.setOutlet(null) })
            }
            items(state.outlets) { outlet ->
                FilterChipBox(
                    label = outlet,
                    selected = state.outlet == outlet,
                    onClick = { viewModel.setOutlet(if (state.outlet == outlet) null else outlet) },
                    leading = { OutletMark(outlet, size = 16.dp, logoUrl = state.logos[outlet]) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.all.isEmpty() -> NoticePanel(
                title = "Nog geen tips",
                message = "Voor ${country.label} is nog geen medium met een leesbare podcastrubriek " +
                    "gevonden. In Nederland, het Verenigd Koninkrijk en de Verenigde Staten staan er wel."
            )

            else -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                item {
                    Text(
                        "${state.visible.size} tips · ${state.outlets.size} media · uit hun podcastrubrieken",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = LocalChartColors.current.muted,
                        modifier = Modifier.height(30.dp).padding(top = 8.dp)
                    )
                }
                var lastGroup: String? = null
                state.visible.forEach { tip ->
                    val group = groupFor(tip.date)
                    if (group != lastGroup) {
                        lastGroup = group
                        item(key = "h-$group-${tip.url}") {
                            Text(
                                group,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                            )
                        }
                    }
                    item(key = "${tip.url}-${tip.showId}") {
                        TipRow(tip, onOpenPodcast)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/** Tips ouder dan een maand belanden onder "Eerder". */
private fun groupFor(date: String?): String {
    val day = parseDate(date) ?: return "EERDER"
    val today = java.time.LocalDate.now()
    return when {
        !day.isBefore(today.minusDays(7)) -> "DEZE WEEK"
        !day.isBefore(today.minusDays(14)) -> "VORIGE WEEK"
        !day.isBefore(today.minusDays(31)) -> "DEZE MAAND"
        else -> "EERDER"
    }
}

@Composable
private fun TipRow(tip: MediaTip, onOpenPodcast: (String, String?, String) -> Unit) {
    val context = LocalContext.current
    val colors = LocalChartColors.current
    val open = {
        tip.showId?.let { onOpenPodcast(it, tip.feedUrl, tip.showTitle.orEmpty()) }
        Unit
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = open)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Artwork(tip.artworkUrl, 56.dp, corner = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutletMark(tip.outlet, logoUrl = tip.logo)
                Text(
                    tip.outlet,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                shortDate(tip.date)?.let {
                    Text("· $it", fontSize = 11.5.sp, color = colors.muted, maxLines = 1)
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                tip.showTitle.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                tip.headline,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (tip.url.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(tip.url)))
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "Lees het artikel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        WoolIcons.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        PlayCircle(onClick = open, modifier = Modifier.padding(top = 6.dp))
    }
}
