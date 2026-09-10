package nl.woolacast.ui.tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.theme.LocalChartColors

@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Terug")
            }
            Text("Chart-tracker", style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(state.artworkUrl, 60.dp, corner = 13.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            state.publisher,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

            if (state.positions.isNotEmpty()) {
                item {
                    Text(
                        "NOTEERT IN ${state.positions.map { it.country }.distinct().size} LANDEN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 4.dp)
                    )
                }
                items(state.positions.groupBy { it.country }.toList().size) { index ->
                    val (country, ranks) = state.positions.groupBy { it.country }
                        .toList().sortedBy { it.second.minOf { p -> p.rank } }[index]
                    CountryRow(country, ranks.associate { it.source to it.rank })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun StatTiles(lines: List<TrackLine>) {
    val colors = LocalChartColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { line ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(seriesColor(line.source))
                    )
                    Text(
                        line.source.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    "#${line.current ?: "–"}",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.9).sp
                )
                val change = line.change
                Text(
                    when {
                        change == null -> "nog één meting"
                        change > 0 -> "$change omhoog"
                        change < 0 -> "${-change} omlaag"
                        else -> "stabiel"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        change != null && change > 0 -> colors.rise
                        change != null && change < 0 -> colors.fall
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
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
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val palette = lines.associate { it.source to seriesColor(it.source) }
    val days = lines.flatMap { line -> line.points.map { it.first } }.distinct().sorted()
    val worst = lines.flatMap { line -> line.points.map { it.second } }.maxOrNull() ?: 1
    val ceiling = maxOf(worst, 10)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Positie in $countryLabel", style = MaterialTheme.typography.labelLarge)
            Text(
                if (days.size < 2) "één meting" else "${days.size} dagen",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
        Spacer(Modifier.height(10.dp))

        if (days.size < 2) {
            Text(
                "Er is pas één meting. Vanaf de tweede dag loopt hier een lijn.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val right = size.width - 34.dp.toPx()
                val bottom = size.height - 6.dp.toPx()
                val top = 8.dp.toPx()

                for (fraction in listOf(0f, 0.5f, 1f)) {
                    val y = top + (bottom - top) * fraction
                    drawLine(axis, Offset(0f, y), Offset(right, y), strokeWidth = 1f)
                }

                lines.forEach { line ->
                    val colour = palette[line.source] ?: ink
                    val path = Path()
                    line.points.forEachIndexed { index, (day, rank) ->
                        val x = right * days.indexOf(day) / (days.size - 1).toFloat()
                        val y = top + (bottom - top) * ((rank - 1f) / ceiling).coerceIn(0f, 1f)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, colour, style = Stroke(width = 5f))

                    line.points.lastOrNull()?.let { (_, rank) ->
                        val y = top + (bottom - top) * ((rank - 1f) / ceiling).coerceIn(0f, 1f)
                        drawCircle(colour, radius = 7f, center = Offset(right, y))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            lines.forEach { line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .width(14.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette[line.source] ?: ink)
                    )
                    Text(
                        "${line.source.label} #${line.current ?: "–"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ink
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(country.flag, fontSize = 18.sp)
        Text(
            country.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        listOf(SourceId.APPLE, SourceId.SPOTIFY).forEach { source ->
            val rank = ranks[source]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (rank != null) seriesColor(source)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        source.initial,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rank != null) MaterialTheme.colorScheme.surfaceContainerLowest
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    rank?.toString() ?: "–",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (rank != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp)
                )
            }
        }
    }
}

/** Getoetst op kleurenblindheid en contrast; identiteit staat altijd óók in tekst. */
@Composable
private fun seriesColor(source: SourceId): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    return when (source) {
        SourceId.APPLE -> if (dark) Color(0xFFDE7047) else Color(0xFFC4542B)
        SourceId.SPOTIFY -> if (dark) Color(0xFF35A05F) else Color(0xFF0E8A4E)
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
