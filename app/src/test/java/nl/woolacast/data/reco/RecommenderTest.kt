package nl.woolacast.data.reco

import nl.woolacast.domain.ChartEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderTest {

    private fun entry(
        rank: Int,
        id: String,
        title: String,
        publisher: String = "Iemand",
        genre: String? = "True crime"
    ) = ChartEntry(
        rank = rank, id = id, title = title, publisher = publisher,
        artworkUrl = null, genre = genre, storeUrl = null
    )

    private val bibliotheek = listOf(
        LibraryShow("1", "De Hippiemoord", "Podimo", "True crime", weight = 3),
        LibraryShow("2", "Zwijgen aan Zee", "NPO Luister", "True crime", weight = 3),
        LibraryShow("3", "Bandsplain", "The Ringer", "Muziek", weight = 1)
    )

    @Test
    fun `het profiel weegt volgen zwaarder dan bewaren`() {
        val profiel = Recommender.profile(bibliotheek)
        assertEquals(listOf("True crime", "Muziek"), profiel.topGenres(2))
        assertTrue("podimo" in profiel.publishers)
        assertTrue("hippiemoord" in profiel.words)
    }

    @Test
    fun `een lege bibliotheek levert geen voorstellen`() {
        val leeg = Recommender.profile(emptyList())
        assertTrue(leeg.isEmpty)
        assertTrue(Recommender.rank(leeg, listOf(entry(1, "9", "Wat dan ook"))).isEmpty())
    }

    @Test
    fun `wat je al hebt wordt niet voorgesteld`() {
        val profiel = Recommender.profile(bibliotheek)
        val voorstellen = Recommender.rank(profiel, listOf(entry(1, "1", "De Hippiemoord")))
        assertTrue(voorstellen.isEmpty())
    }

    @Test
    fun `het zwaarste genre komt bovenaan`() {
        val profiel = Recommender.profile(bibliotheek)
        val voorstellen = Recommender.rank(
            profiel,
            listOf(
                entry(1, "10", "Een muziekverhaal", genre = "Muziek"),
                entry(2, "11", "Een moordzaak", genre = "True crime")
            )
        )
        assertEquals("11", voorstellen.first().entry.id)
    }

    @Test
    fun `dezelfde maker weegt mee en staat in de reden`() {
        val profiel = Recommender.profile(bibliotheek)
        val voorstellen = Recommender.rank(
            profiel,
            listOf(
                entry(1, "20", "Iets anders", publisher = "Onbekend", genre = "Muziek"),
                entry(9, "21", "Nog een reeks", publisher = "Podimo", genre = "Muziek")
            )
        )
        assertEquals("21", voorstellen.first().entry.id)
        assertEquals("Ook van Podimo", voorstellen.first().reason)
    }

    @Test
    fun `verwant aan een podcast zet dezelfde maker vooraan`() {
        val voorstellen = Recommender.similarTo(
            title = "De Hippiemoord",
            publisher = "Podimo",
            genre = "True crime",
            description = "Een moord in een jongerencentrum in de jaren zeventig",
            showId = "1",
            candidates = listOf(
                entry(1, "30", "Populaire show", publisher = "Anders"),
                entry(40, "31", "Andere reeks", publisher = "Podimo"),
                entry(5, "1", "De Hippiemoord", publisher = "Podimo")
            )
        )
        assertEquals("31", voorstellen.first().entry.id)
        assertEquals("Van dezelfde maker", voorstellen.first().reason)
        // De show zelf hoort er niet bij te staan.
        assertFalse(voorstellen.any { it.entry.id == "1" })
    }

    @Test
    fun `woorden uit een titel tellen mee, loze woorden niet`() {
        val woorden = Recommender.words("De podcast over de Zwarte Dood")
        assertTrue("zwarte" in woorden)
        assertTrue("dood" in woorden)
        assertFalse("podcast" in woorden)
        assertFalse("over" in woorden)
    }
}
