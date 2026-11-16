package nl.woolacast.data.alerts

import nl.woolacast.data.dataset.DatasetMover

/** Een beweging in de hitlijst die een melding waard is. */
data class ChartNews(val showId: String, val title: String, val line: String)

/**
 * Welke bewegingen van shows die je volgt een melding verdienen. Los van
 * Android, zodat het te testen is. Niet elke stap omhoog is nieuws: alleen de
 * nummer 1, de top tien binnenkomen, nieuw in de lijst staan, of een sprong
 * van [BIG_JUMP] plekken of meer.
 */
object ChartAlertRules {

    const val BIG_JUMP = 20

    fun news(
        movers: List<DatasetMover>,
        followed: Set<String>,
        countryCode: String,
        alreadyNotified: Set<String>,
        today: String
    ): List<ChartNews> =
        movers.asSequence()
            .filter { it.showId != null && it.showId in followed }
            // De lijst over alles gaat voor een categorielijst: daar is een plek het meeste waard.
            .sortedBy { if (it.genreId == null || it.genreId == 26) 0 else 1 }
            .mapNotNull { mover ->
                val id = mover.showId ?: return@mapNotNull null
                val list = mover.genreLabel.takeIf { it.isNotBlank() && mover.genreId != null && mover.genreId != 26 }
                val where = listOfNotNull(
                    if (mover.source == "spotify") "Spotify" else "Apple",
                    list,
                    countryCode.uppercase()
                ).joinToString(" ")
                val previous = mover.previousRank
                val line = when {
                    mover.rank == 1 && previous != 1 -> "Staat op 1 · $where"
                    previous == null -> "Nieuw binnen op #${mover.rank} · $where"
                    mover.rank <= 10 && previous > 10 -> "De top tien in: nu #${mover.rank} · $where"
                    mover.move >= BIG_JUMP -> "${mover.move} plekken omhoog naar #${mover.rank} · $where"
                    else -> return@mapNotNull null
                }
                ChartNews(id, mover.title, line)
            }
            .distinctBy { it.showId }
            .filter { key(today, it.showId) !in alreadyNotified }
            .toList()

    /** Eén melding per show per dag. */
    fun key(today: String, showId: String) = "$today|$showId"
}
