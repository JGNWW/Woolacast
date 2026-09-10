package nl.woolacast.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private val MONTHS = listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec")
private val WEEKDAYS = listOf("ma", "di", "wo", "do", "vr", "za", "zo")

/** "2026-09-08" → "8 sep"; wat niet parseert komt terug zoals het was. */
fun shortDate(iso: String?): String? {
    val date = parseDate(iso) ?: return iso
    val label = "${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"
    return if (date.year != LocalDate.now().year) "$label ${date.year}" else label
}

/** "di" voor een datum van deze week, anders "8 sep". */
fun relativeDay(iso: String?): String? {
    val date = parseDate(iso) ?: return iso
    val today = LocalDate.now()
    return when {
        date == today -> "vandaag"
        date == today.minusDays(1) -> "gisteren"
        !date.isBefore(today.minusDays(6)) -> WEEKDAYS[date.dayOfWeek.value - 1]
        else -> shortDate(iso)
    }
}

fun parseDate(iso: String?): LocalDate? =
    iso?.take(10)?.let { runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() }

fun minutes(millis: Long?): String? = millis?.let { "${TimeUnit.MILLISECONDS.toMinutes(it)} min" }

/** "nog 18 min", of "nog 1 u 12 min" boven het uur. */
fun remaining(millis: Long): String {
    val total = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = total / 60
    val mins = total % 60
    return when {
        hours > 0 -> "nog $hours u $mins min"
        total < 1 -> "bijna klaar"
        else -> "nog $total min"
    }
}

fun clock(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    val hours = TimeUnit.MILLISECONDS.toHours(safe)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}

/** 1.2f → "1,2×", 1f → "1×" */
fun speedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString()
    else String.format(Locale.ROOT, "%.2f", speed).trimEnd('0').replace('.', ',')
    return "$text×"
}

/** Hoe vaak een show verschijnt, afgeleid uit de tussenpozen van de laatste afleveringen. */
fun cadence(dates: List<String?>): String? {
    val days = dates.mapNotNull(::parseDate).sortedDescending().take(8)
    if (days.size < 3) return null
    val gaps = days.zipWithNext { later, earlier -> (later.toEpochDay() - earlier.toEpochDay()).toInt() }
        .filter { it >= 0 }.sorted()
    val median = gaps[gaps.size / 2]
    return when {
        median <= 1 -> "dagelijks"
        median <= 4 -> "enkele keren per week"
        median <= 9 -> "wekelijks"
        median <= 18 -> "tweewekelijks"
        median <= 40 -> "maandelijks"
        else -> "onregelmatig"
    }
}
