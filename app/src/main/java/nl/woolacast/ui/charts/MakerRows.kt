package nl.woolacast.ui.charts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import nl.woolacast.domain.Movement
import nl.woolacast.ui.common.NoticePanel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.MakerTile
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
        // Even hoog in beide weergaven, zodat de lijst niet verspringt bij het wisselen.
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (byMaker) "${MakerRanking.MIN_SHOWS}+ podcasts · sinds gisteren" else "Top $listSize",
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
    onOpenMaker: (name: String, fromShowId: String?) -> Unit,
    onPerShow: () -> Unit
) {
    when {
        rows == null -> item(key = "makers-loading") {
            Box(Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        rows.isEmpty() -> item(key = "makers-empty") {
            NoticePanel(
                title = "Geen makers met ${MakerRanking.MIN_SHOWS}+ podcasts",
                message = "In deze lijst heeft geen maker twee of meer podcasts. Per show zie je de hele lijst.",
                actionLabel = "Toon per show",
                onAction = onPerShow,
                outerPadding = PaddingValues(vertical = 28.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(60.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken(row) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RankNumber(row.rank, size = 22.dp, fontSize = 16.sp)
        // Het gezicht van een maker: zijn podcasts. Daarom geen losse hoesjes meer rechts.
        MakerTile(row.artworks, 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.maker.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${row.count} podcasts · beste #${row.best}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MovementBadge(row.movement)
    }
}

/** Wat TalkBack voorleest: de plek, de maker, en de beweging in woorden. */
private fun spoken(row: MakerRank): String {
    val move = when (val m = row.movement) {
        is Movement.Up -> m.places?.let { if (it == 1) "1 plek gestegen" else "$it plekken gestegen" } ?: "gestegen"
        is Movement.Down -> m.places?.let { if (it == 1) "1 plek gedaald" else "$it plekken gedaald" } ?: "gedaald"
        Movement.New -> "nieuw"
        Movement.Flat -> "gelijk gebleven"
        Movement.Unknown -> null
    }
    return listOfNotNull("Plek ${row.rank}", row.maker.name, "${row.count} podcasts", "beste plek ${row.best}", move)
        .joinToString(", ")
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
        Column(Modifier.padding(bottom = 32.dp)) {
            // Dezelfde kop als "Lijst instellen".
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hoe tellen we?", style = MaterialTheme.typography.titleLarge)
                IconAction(WoolIcons.Close, "Sluiten", onDismiss)
            }
            listOf(
                "Elke podcast telt bij één maker. Kent Apple de podcast als deel van een kanaal, dan is dat kanaal de maker. Anders het eerste deel van de makersnaam: \"NPO Luister / BNNVARA\" telt bij NPO Luister.",
                "Alleen makers met twee of meer podcasts in deze lijst staan erin.",
                "Hebben twee makers evenveel podcasts, dan gaat de maker met de beste plek voor.",
                "De pijl is de verandering in de plek van de maker sinds gisteren, zoals in de rest van de app. De eerste dag is er nog geen pijl."
            ).forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                )
            }
        }
    }
}
