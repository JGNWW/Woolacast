package nl.woolacast.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.ShowListening
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

/** "3 u 12 min", "47 min" of "minder dan een minuut". */
fun listeningTime(ms: Long): String {
    val minutes = ms / 60_000L
    return when {
        minutes < 1L -> "minder dan een minuut"
        minutes < 60L -> "$minutes min"
        minutes % 60L == 0L -> "${minutes / 60L} u"
        else -> "${minutes / 60L} u ${minutes % 60L} min"
    }
}

/** De luistertijd van de laatste [days] dagen, de oudste eerst; dagen zonder luisteren tellen als nul. */
fun lastDays(perDay: Map<String, Long>, days: Int, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Long>> =
    (days - 1 downTo 0).map { back -> today.minusDays(back.toLong()).let { it to (perDay[it.toString()] ?: 0L) } }

private val WEEKDAYS = listOf("ma", "di", "wo", "do", "vr", "za", "zo")

/**
 * Hoeveel je luisterde, en naar wat. Alles is op het toestel geteld terwijl de
 * speler liep; er is geen account en er gaat niets naar buiten.
 */
@Composable
fun StatsScreen(store: LocalStore, onBack: () -> Unit, onOpenPodcast: (showId: String, title: String) -> Unit = { _, _ -> }) {
    val perDay by store.listeningDays.collectAsStateWithLifecycle()
    val perShow by store.listening.collectAsStateWithLifecycle()
    val history by store.history.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TitleBar("Luisterstatistiek", onBack)
        if (perDay.isEmpty() && perShow.isEmpty()) {
            NoticePanel(
                title = "Nog niets geteld",
                message = "Zodra je iets afspeelt, telt de app hier hoe lang je luisterde en naar welke shows. " +
                    "Dat blijft op je toestel."
            )
            return@Column
        }
        val week = lastDays(perDay, 7).sumOf { it.second }
        val month = lastDays(perDay, 30).sumOf { it.second }
        val total = perShow.values.sumOf { it.ms }
        val finished = history.count { it.finished }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            // Het ene getal waar het om gaat, groot; de rest eronder als kleine feiten.
            Text(
                "DEZE WEEK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(listeningTime(week), fontFamily = DisplayFamily, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Fact("30 dagen", listeningTime(month))
                Fact("Alles bij elkaar", listeningTime(total))
                Fact("Uitgeluisterd", if (finished == 1) "1 aflevering" else "$finished afleveringen")
            }

            DayChart(lastDays(perDay, 14))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 22.dp))
            Text("Je meest beluisterde shows", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            val top = perShow.entries.sortedByDescending { it.value.ms }.take(12)
            val longest = top.firstOrNull()?.value?.ms?.coerceAtLeast(1L) ?: 1L
            top.forEach { (showId, show) -> ShowBar(showId, show, longest, onOpenPodcast) }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Veertien dagen als staafjes: één reeks, één kleur, vandaag rechts. Tik op een
 * dag om te lezen hoe lang het was; zonder tik staat de langste dag erboven.
 */
@Composable
private fun DayChart(days: List<Pair<LocalDate, Long>>) {
    val peak = days.maxOf { it.second }.coerceAtLeast(1L)
    var picked by remember { mutableStateOf<Int?>(null) }
    val shown = picked ?: days.indices.maxByOrNull { days[it].second } ?: 0
    val muted = LocalChartColors.current.muted
    val today = LocalDate.now()

    fun name(date: LocalDate) = when (date) {
        today -> "vandaag"
        today.minusDays(1) -> "gisteren"
        else -> "${WEEKDAYS[date.dayOfWeek.value - 1]} ${date.dayOfMonth}-${date.monthValue}"
    }

    Column(Modifier.padding(top = 26.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Per dag, laatste twee weken", style = MaterialTheme.typography.titleMedium)
            Text(
                "${name(days[shown].first)} · ${listeningTime(days[shown].second)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            Modifier.fillMaxWidth().height(132.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, (date, ms) ->
                // De hele kolom is het tikvlak, niet alleen het staafje.
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { picked = index }
                        .semantics { contentDescription = "${name(date)}: ${listeningTime(ms)}" },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.72f)
                            // Een dag zonder luisteren houdt een streepje, zodat de as leesbaar blijft.
                            .fillMaxHeight(if (ms == 0L) 0.015f else (ms.toFloat() / peak).coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    ms == 0L -> MaterialTheme.colorScheme.outlineVariant
                                    index == shown -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                }
                            )
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            days.forEachIndexed { index, (date, _) ->
                Text(
                    WEEKDAYS[date.dayOfWeek.value - 1].take(1).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = if (index == shown) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == shown) MaterialTheme.colorScheme.onSurface else muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Een show met zijn luistertijd als streep; de langste vult de breedte. */
@Composable
private fun ShowBar(showId: String, show: ShowListening, longest: Long, onOpenPodcast: (String, String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpenPodcast(showId, show.title) }.padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(show.artworkUrl, 40.dp, corner = 9.dp, elevation = 0.dp)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    show.title.ifBlank { "Onbekende show" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(listeningTime(show.ms), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Box(
                    Modifier
                        .fillMaxWidth((show.ms.toFloat() / longest).coerceIn(0.02f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.width(0.dp))
    }
}
