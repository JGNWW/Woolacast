package nl.woolacast.data.maker

import nl.woolacast.domain.Chart
import nl.woolacast.domain.Maker
import nl.woolacast.domain.Movement

/** Eén rij in de makersweergave van een hitlijst. */
data class MakerRank(
    val rank: Int,
    val maker: Maker,
    /** Hoeveel shows van deze maker in de lijst staan. */
    val count: Int,
    /** Zijn hoogste plek in de lijst. */
    val best: Int,
    /** De hoezen van zijn drie hoogste shows. */
    val artworks: List<String?>,
    /** Het id van zijn hoogste show, om de podcastpagina te kunnen openen. */
    val bestShowId: String?,
    val movement: Movement = Movement.Unknown
)

/**
 * Telt een lijst per maker. De regels:
 * - elke show telt bij precies één maker (het kanaal, anders de eerste naam);
 * - alleen makers met twee of meer shows;
 * - bij een gelijk aantal gaat de maker met de hoogste plek voor;
 * - beweging is de verandering in rang van de maker ten opzichte van
 *   [previous], de makersranglijst van een eerdere dag (sleutel → rang).
 */
object MakerRanking {

    const val MIN_SHOWS = 2

    fun rank(chart: Chart, directory: MakerDirectory, previous: Map<String, Int>?): List<MakerRank> {
        val groups = chart.entries
            .map { entry -> directory.makerOf(entry.showId ?: entry.id, entry.publisher) to entry }
            .filter { (maker, _) -> maker.key.isNotEmpty() }
            .groupBy({ it.first.key }, { it })

        return groups.values
            .filter { it.size >= MIN_SHOWS }
            .map { members ->
                val entries = members.map { it.second }.sortedBy { it.rank }
                // Het kanaal wint van een losse naam als de groep beide kent.
                val maker = members.map { it.first }.firstOrNull { it.channel != null } ?: members.first().first
                MakerRank(
                    rank = 0,
                    maker = maker,
                    count = entries.size,
                    best = entries.first().rank,
                    artworks = entries.take(3).map { it.artworkUrl },
                    bestShowId = entries.first().showId ?: entries.first().id
                )
            }
            .sortedWith(compareByDescending<MakerRank> { it.count }.thenBy { it.best })
            .mapIndexed { index, row ->
                val rank = index + 1
                row.copy(rank = rank, movement = movement(rank, previous?.get(row.maker.key), previous != null))
            }
    }

    /** De rangen van vandaag, om morgen beweging mee te bepalen. */
    fun snapshot(rows: List<MakerRank>): Map<String, Int> = rows.associate { it.maker.key to it.rank }

    private fun movement(rank: Int, before: Int?, hasBaseline: Boolean): Movement = when {
        !hasBaseline -> Movement.Unknown
        before == null -> Movement.New
        before > rank -> Movement.Up(before - rank)
        before < rank -> Movement.Down(rank - before)
        else -> Movement.Flat
    }
}
