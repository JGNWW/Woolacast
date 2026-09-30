package nl.woolacast.data.maker

import nl.woolacast.data.dataset.DatasetChannel
import nl.woolacast.data.dataset.DatasetMakers
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MakerRankingTest {

    private val query = ChartQuery(SourceId.APPLE, Catalog.defaultCountry, Catalog.defaultCategory, ChartLevel.SHOWS)

    private fun entry(rank: Int, id: String, publisher: String) =
        ChartEntry(rank = rank, id = id, title = "Show $id", publisher = publisher, artworkUrl = null, genre = null, storeUrl = null)

    private val directory = MakerDirectory(
        DatasetMakers(
            channels = listOf(DatasetChannel(id = "c1", name = "NPO Luister", color = "ff6e00")),
            // Show 7 hangt aan het kanaal, al zegt zijn makersnaam iets anders.
            showChannel = mapOf("7" to "c1")
        )
    )

    @Test
    fun `een show telt bij een maker, ook via het kanaal of een samengestelde naam`() {
        val chart = Chart(query, listOf(
            entry(1, "1", "NPO Luister / BNNVARA"),
            entry(2, "2", "BNR Nieuwsradio"),
            entry(3, "3", "NPO Luister / AVROTROS"),
            entry(4, "4", "BNR Nieuwsradio"),
            entry(5, "7", "VPRO"),
            entry(6, "8", "Losse Maker")
        ), null)

        val rows = MakerRanking.rank(chart, directory, previous = null)

        assertEquals(listOf("NPO Luister", "BNR Nieuwsradio"), rows.map { it.maker.name })
        assertEquals(3, rows[0].count)
        assertTrue(rows[0].maker.channel != null)
        // De som blijft binnen de lijst: niemand telt dubbel, en losse makers vallen af.
        assertEquals(5, rows.sumOf { it.count })
    }

    @Test
    fun `bij een gelijk aantal gaat de hoogste plek voor`() {
        val chart = Chart(query, listOf(
            entry(1, "a", "Studio Hemel"),
            entry(5, "b", "Radio Oost"),
            entry(6, "c", "Radio Oost"),
            entry(9, "d", "Studio Hemel")
        ), null)

        val rows = MakerRanking.rank(chart, MakerDirectory(null), previous = null)

        assertEquals(listOf("Studio Hemel", "Radio Oost"), rows.map { it.maker.name })
        assertEquals(listOf(1, 5), rows.map { it.best })
    }

    @Test
    fun `beweging is de rang van de maker ten opzichte van eerder`() {
        val chart = Chart(query, listOf(
            entry(1, "a", "A"), entry(2, "b", "A"),
            entry(3, "c", "B"), entry(4, "d", "B"),
            entry(5, "e", "C"), entry(6, "f", "C")
        ), null)

        val none = MakerRanking.rank(chart, MakerDirectory(null), previous = null)
        assertTrue(none.all { it.movement == Movement.Unknown })

        val rows = MakerRanking.rank(chart, MakerDirectory(null), previous = mapOf("a" to 2, "b" to 1))
        assertEquals(Movement.Up(1), rows[0].movement)
        assertEquals(Movement.Down(1), rows[1].movement)
        assertEquals(Movement.New, rows[2].movement)
    }
}
