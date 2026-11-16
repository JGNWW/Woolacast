package nl.woolacast.ui.settings

import java.time.LocalDate
import nl.woolacast.data.reco.LibraryShow
import nl.woolacast.data.reco.Recommender
import nl.woolacast.player.tidySpeed
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {

    @Test
    fun `luistertijd in woorden`() {
        assertEquals("minder dan een minuut", listeningTime(40_000L))
        assertEquals("47 min", listeningTime(47 * 60_000L))
        assertEquals("2 u", listeningTime(120 * 60_000L))
        assertEquals("3 u 12 min", listeningTime(192 * 60_000L))
    }

    @Test
    fun `dagen zonder luisteren tellen als nul, de oudste eerst`() {
        val today = LocalDate.parse("2026-11-16")
        val days = lastDays(mapOf("2026-11-16" to 5L, "2026-11-14" to 7L, "2026-10-01" to 99L), 3, today)
        assertEquals(listOf(7L, 0L, 5L), days.map { it.second })
        assertEquals(LocalDate.parse("2026-11-14"), days.first().first)
    }

    @Test
    fun `een vrije snelheid wordt afgerond en begrensd`() {
        assertEquals(1.35f, tidySpeed(1.37f), 0.0001f)
        assertEquals(0.5f, tidySpeed(0.1f), 0.0001f)
        assertEquals(3f, tidySpeed(9f), 0.0001f)
    }

    @Test
    fun `waar je echt naar luistert weegt zwaarder in je smaak`() {
        val shows = listOf(LibraryShow("a", "A", "", weight = 3), LibraryShow("b", "B", "", weight = 3))
        val weighted = Recommender.withListening(
            shows,
            mapOf(
                "a" to ("A" to 12 * 3_600_000L),   // uren gehoord
                "c" to ("C" to 3 * 3_600_000L),    // niet gevolgd, wel gehoord
                "d" to ("D" to 5 * 60_000L)        // vijf minuten: een keer geprobeerd
            )
        )
        assertEquals(mapOf("a" to 6, "b" to 3, "c" to 2), weighted.associate { it.id to it.weight })
    }
}
