package nl.woolacast.data.alerts

import nl.woolacast.data.dataset.DatasetMover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartAlertRulesTest {

    private fun mover(show: String, rank: Int, previous: Int?, source: String = "apple", genreId: Int? = null, genre: String = "") =
        DatasetMover(
            id = show, title = "Show $show", showId = show, rank = rank, previousRank = previous,
            move = previous?.let { it - rank } ?: 0, source = source, genreId = genreId, genreLabel = genre
        )

    private fun news(vararg movers: DatasetMover, notified: Set<String> = emptySet()) =
        ChartAlertRules.news(movers.toList(), setOf("a", "b", "c", "d", "e"), "nl", notified, "2026-11-16")

    @Test
    fun `alleen wat opvalt is nieuws`() {
        val found = news(
            mover("a", 1, 4),          // de nummer 1
            mover("b", 8, 14),         // de top tien in
            mover("c", 60, null),      // nieuw binnen
            mover("d", 40, 70),        // een grote sprong
            mover("e", 30, 33)         // drie plekken: geen melding
        )
        assertEquals(listOf("a", "b", "c", "d"), found.map { it.showId })
        assertEquals("Staat op 1 · Apple NL", found[0].line)
        assertEquals("De top tien in: nu #8 · Apple NL", found[1].line)
        assertEquals("Nieuw binnen op #60 · Apple NL", found[2].line)
        assertEquals("30 plekken omhoog naar #40 · Apple NL", found[3].line)
    }

    @Test
    fun `een show die je niet volgt meldt niets`() {
        assertTrue(news(mover("x", 1, 9)).isEmpty())
    }

    @Test
    fun `een melding per show per dag, en de lijst over alles gaat voor`() {
        val found = news(
            mover("a", 1, 3, genreId = 1303, genre = "Comedy"),
            mover("a", 5, 40)
        )
        assertEquals(1, found.size)
        assertEquals("De top tien in: nu #5 · Apple NL", found.single().line)

        assertTrue(news(mover("a", 1, 3), notified = setOf(ChartAlertRules.key("2026-11-16", "a"))).isEmpty())
        // Morgen mag het weer.
        assertEquals(1, news(mover("a", 1, 3), notified = setOf(ChartAlertRules.key("2026-11-15", "a"))).size)
    }

    @Test
    fun `een categorielijst en Spotify staan erbij`() {
        assertEquals("Staat op 1 · Apple Comedy NL", news(mover("a", 1, 3, genreId = 1303, genre = "Comedy")).single().line)
        assertEquals("Staat op 1 · Spotify NL", news(mover("a", 1, 3, source = "spotify")).single().line)
    }
}
