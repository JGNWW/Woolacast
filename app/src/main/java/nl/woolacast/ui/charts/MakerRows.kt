package nl.woolacast.ui.charts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.data.maker.MakerRank
import nl.woolacast.data.maker.MakerRanking
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.MakerLogo
import nl.woolacast.ui.common.MovementBadge
import nl.woolacast.ui.common.RankNumber
import nl.woolacast.ui.common.SmallChip
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.theme.LocalChartColors

/**
 * De regel boven de showlijst: wat je ziet, en rechts de keuze tussen de lijst
 * per show en per maker. Eén regel, zodat de lijst er niet voor zakt.
 */
@Composable
internal fun ViewLine(
    byMaker: Boolean,
    listSize: Int,
    onByMaker: (Boolean) -> Unit,
    onExplain: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val muted = LocalChartColors.current.muted
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (byMaker) "${MakerRanking.MIN_SHOWS}+ shows · t.o.v. gisteren" else "Top $listSize",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (byMaker) {
                IconAction(WoolIcons.Info, "Hoe tellen we?", onExplain, tint = muted, iconSize = 16.dp)
            }
        }
        Box {
            SmallChip(if (byMaker) "Per maker" else "Per show", onClick = { open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                listOf(false to "Per show", true to "Per maker").forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label, fontWeight = if (value == byMaker) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { open = false; onByMaker(value) }
                    )
                }
            }
        }
    }
}

/** De lijst per maker: dezelfde rij als per show, maar met een rond logo en zijn hoesjes. */
internal fun LazyListScope.makerRows(
    rows: List<MakerRank>?,
    onOpenMaker: (name: String, fromShowId: String?) -> Unit
) {
    when {
        rows == null -> item(key = "makers-loading") {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        rows.isEmpty() -> item(key = "makers-empty") {
            Text(
                "Geen maker heeft twee of meer shows in deze lijst.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        else -> items(rows, key = { "maker-${it.maker.key}" }) { row ->
            MakerChartRow(row) { onOpenMaker(row.maker.name, row.bestShowId) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun MakerChartRow(row: MakerRank, onClick: () -> Unit) {
    val channel = row.maker.channel
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RankNumber(row.rank, size = 22.dp, fontSize = 16.sp)
        MakerLogo(row.maker.name, channel?.logoUrl, channel?.color, 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.maker.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${row.count} shows · hoogste #${row.best}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CoverStack(row.artworks)
        MovementBadge(row.movement)
    }
}

/** Drie hoesjes die over elkaar schuiven, elk met een rand in de achtergrondkleur. */
@Composable
private fun CoverStack(urls: List<String?>) {
    val ring = MaterialTheme.colorScheme.background
    Box(Modifier.width((26 + 20 * (urls.size - 1).coerceAtLeast(0)).dp).height(26.dp)) {
        urls.forEachIndexed { index, url ->
            Artwork(
                url, 26.dp, corner = 6.dp, elevation = 0.dp,
                modifier = Modifier
                    .offset(x = (20 * index).dp)
                    .border(2.dp, ring, RoundedCornerShape(6.dp))
            )
        }
    }
}

/** Uitleg bij de telling: wie telt, hoe, en wat de pijl betekent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CountingSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hoe tellen we?", style = MaterialTheme.typography.titleLarge)
                IconAction(WoolIcons.Close, "Sluiten", onDismiss)
            }
            listOf(
                "Elke show telt bij één maker. Kent Apple de show als deel van een kanaal, dan is dat kanaal de maker. Anders het eerste deel van de makersnaam: \"NPO Luister / BNNVARA\" telt bij NPO Luister.",
                "Alleen makers met twee of meer shows in deze lijst staan erin.",
                "Hebben twee makers evenveel shows, dan gaat de maker met de hoogste plek voor.",
                "De pijl is de verandering in de plek van de maker sinds gisteren, zoals in de rest van de app. De eerste dag is er nog geen pijl."
            ).forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}
