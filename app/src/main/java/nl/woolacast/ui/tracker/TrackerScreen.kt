package nl.woolacast.ui.tracker

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.LinkText
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.SourceDot
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.shortDate
import nl.woolacast.ui.theme.BodyFamily
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

private const val COUNTRY_SHORTLIST = 3

/** Eén show, alle bronnen, alle landen, over tijd. */
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val follows by viewModel.follows.collectAsStateWithLifecycle()
    val isFollowed = follows.any { it.id == viewModel.showId }
    var allCountries by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TitleBar("Chart-tracker", onBack) {
            IconAction(WoolIcons.Share, "Delen", {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                }
                context.startActivity(Intent.createChooser(intent, "Noteringen delen"))
            })
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(state.artworkUrl, 60.dp, corner = 13.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.title,
                            fontFamily = DisplayFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                            lineHeight = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOfNotNull(state.publisher.takeIf { it.isNotBlank() }, state.genre).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilterChipBox(
                        label = if (isFollowed) "Gevolgd" else "Volgen",
                        selected = isFollowed,
                        onClick = viewModel::toggleFollow,
                        modifier = Modifier.height(38.dp)
                    )
                }
            }

            if (!state.available) {
                item {
                    NoticePanel(
                        title = "Nog niet gevolgd",
                        message = "Deze show staat nog niet in de vastgelegde lijsten. " +
                            "Zodra hij ergens noteert en de lijsten zijn bijgewerkt, " +
                            "verschijnt zijn verloop hier."
                    )
                }
                return@LazyColumn
            }

            if (state.lines.isNotEmpty()) {
                item { StatTiles(state.lines) }
                item { RankChart(state.lines, state.countryLabel) }
            }

            state.peak?.let { peak ->
                item {
                    Row(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            WoolIcons.Trophy, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            buildString {
                                append("Hoogste notering: #${peak.rank} op ${peak.source.label} ")
                                append(state.countryCode.uppercase())
                                shortDate(peak.date)?.let { append(", $it") }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (state.positions.isNotEmpty()) {
                val byCountry = state.positions.groupBy { it.country }.toList()
                    .sortedBy { it.second.minOf { p -> p.rank } }
                val shown = if (allCountries) byCountry else byCountry.take(COUNTRY_SHORTLIST)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "NOTEERT IN ${byCountry.size} ${if (byCountry.size == 1) "LAND" else "LANDEN"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (byCountry.size > COUNTRY_SHORTLIST) {
                            LinkText(if (allCountries) "Minder" else "Alles") { allCountries = !allCountries }
                        }
                    }
                }
                items(shown.size) { index ->
                    val (country, ranks) = shown[index]
                    CountryRow(country, ranks.associate { it.source to it.rank })
                    if (index < shown.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTiles(lines: List<TrackLine>) {
    val colors = LocalChartColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { line ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 11.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(seriesColor(line.source)))
                    Text(
                        line.source.label.substringBefore(' '),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    "#${line.current ?: "–"}",
                    fontFamily = DisplayFamily,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.9).sp,
                    lineHeight = 28.sp
                )
                val change = line.change
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val tint = when {
                        change != null && change > 0 -> colors.onRiseContainer
                        change != null && change < 0 -> colors.onFallContainer
                        else -> colors.muted
                    }
                    when {
                        change == null -> Unit
                        change > 0 -> Icon(WoolIcons.Up, null, tint = tint, modifier = Modifier.size(9.dp))
                        change < 0 -> Icon(WoolIcons.Down, null, tint = tint, modifier = Modifier.size(9.dp))
                        else -> Icon(WoolIcons.Flat, null, tint = tint, modifier = Modifier.size(9.dp))
                    }
                    Text(
                        when {
                            change == null -> "nog één meting"
                            change == 0 -> "stabiel"
                            else -> "${kotlin.math.abs(change)} in ${line.days} dgn"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Rang over tijd. Y loopt omgekeerd — plek 1 hoort bovenaan. De lijnen dragen
 * hun laatste plek als label, zodat kleur nooit het enige onderscheid is.
 */
@Composable
private fun RankChart(lines: List<TrackLine>, countryLabel: String) {
    val axis = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = LocalChartColors.current.muted
    val paper = MaterialTheme.colorScheme.surfaceContainerLowest
    val measurer = rememberTextMeasurer()

    val palette = lines.associate { it.source to seriesColor(it.source) }
    val days = lines.flatMap { line -> line.points.map { it.first } }.distinct().sorted()
    val worst = lines.flatMap { line -> line.points.map { it.second } }.maxOrNull() ?: 1
    // Assen op ronde getallen: tot 10, 20, 30 … afhankelijk van hoe diep de show zakte.
    val ceiling = listOf(10, 20, 30, 50, 100, 200).firstOrNull { it >= worst } ?: 200
    val gridRanks = when (ceiling) {
        10 -> listOf(1, 5, 10)
        20 -> listOf(1, 10, 20)
        30 -> listOf(1, 10, 20, 30)
        50 -> listOf(1, 25, 50)
        100 -> listOf(1, 50, 100)
        else -> listOf(1, 100, 200)
    }

    val axisStyle = TextStyle(fontFamily = BodyFamily, fontSize = 9.sp, color = muted)
    val labelStyle = TextStyle(fontFamily = BodyFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ink)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(paper)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(start = 12.dp, end = 12.dp, top = 13.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Positie in $countryLabel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (days.size < 2) "één meting" else "${days.size} dagen",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = muted
            )
        }
        Spacer(Modifier.height(8.dp))

        if (days.size < 2) {
            Text(
                "Er is pas één meting. Vanaf de tweede dag loopt hier een lijn.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(128.dp)) {
                val left = 22.dp.toPx()
                val right = size.width - 40.dp.toPx()
                val top = 10.dp.toPx()
                val bottom = size.height - 22.dp.toPx()

                fun yFor(rank: Int) = top + (bottom - top) * ((rank - 1f) / (ceiling - 1f)).coerceIn(0f, 1f)
                fun xFor(day: String) = left + (right - left) * days.indexOf(day) / (days.size - 1).toFloat()

                gridRanks.forEach { rank ->
                    val y = yFor(rank)
                    drawLine(axis, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                    val text = measurer.measure(rank.toString(), axisStyle)
                    drawText(text, topLeft = Offset(left - 6.dp.toPx() - text.size.width, y - text.size.height / 2f))
                }

                lines.forEach { line ->
                    val colour = palette[line.source] ?: ink
                    val path = Path()
                    line.points.forEachIndexed { index, (day, rank) ->
                        val point = Offset(xFor(day), yFor(rank))
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, colour, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // Eindpunten en hun waarde, met een stukje papier eromheen zodat lijnen elkaar niet bijten.
                lines.forEach { line ->
                    val (day, rank) = line.points.lastOrNull() ?: return@forEach
                    val centre = Offset(xFor(day), yFor(rank))
                    drawCircle(paper, radius = 6.5.dp.toPx(), center = centre)
                    drawCircle(palette[line.source] ?: ink, radius = 4.5.dp.toPx(), center = centre)
                    val text = measurer.measure(rank.toString(), labelStyle)
                    drawText(text, topLeft = Offset(centre.x + 9.dp.toPx(), centre.y - text.size.height / 2f))
                }

                val first = measurer.measure(shortDate(days.first()).orEmpty(), axisStyle)
                drawText(first, topLeft = Offset(left, bottom + 6.dp.toPx()))
                val last = measurer.measure(shortDate(days.last()).orEmpty(), axisStyle)
                drawText(last, topLeft = Offset(right - last.size.width, bottom + 6.dp.toPx()))
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(palette[line.source] ?: ink))
                    Text(
                        line.source.label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryRow(countryCode: String, ranks: Map<SourceId, Int>) {
    val country = Catalog.country(countryCode)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Flag(countryCode, width = 26.dp, height = 18.dp, corner = 4.dp)
        Text(
            country.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        listOf(SourceId.APPLE, SourceId.SPOTIFY).forEach { source ->
            val rank = ranks[source]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SourceDot(source, active = rank != null)
                Text(
                    rank?.toString() ?: "–",
                    fontFamily = DisplayFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    textAlign = TextAlign.Start,
                    color = if (rank != null) MaterialTheme.colorScheme.onSurface else LocalChartColors.current.muted,
                    modifier = Modifier.width(26.dp)
                )
            }
        }
    }
}

/** Getoetst op kleurenblindheid en contrast; identiteit staat altijd óók in tekst. */
@Composable
private fun seriesColor(source: SourceId): Color = when (source) {
    SourceId.APPLE -> LocalChartColors.current.seriesApple
    SourceId.SPOTIFY -> LocalChartColors.current.seriesSpotify
}
