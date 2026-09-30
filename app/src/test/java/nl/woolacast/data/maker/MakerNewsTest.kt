package nl.woolacast.data.maker

import java.time.LocalDate
import nl.woolacast.data.local.FollowedMaker
import nl.woolacast.domain.Channel
import nl.woolacast.domain.ChannelShow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MakerNewsTest {

    private val today = LocalDate.of(2026, 9, 30)

    private fun channel(vararg shows: ChannelShow) =
        Channel("c", "Kade Media", null, null, null, 3, listOf("1", "2"), shows.toList())

    private val maker = FollowedMaker(key = "kademedia", name = "Kade Media", knownShowIds = listOf("1", "2"))

    @Test
    fun `een jonge show die er bij het volgen nog niet was, is een melding waard`() {
        val fresh = ChannelShow("3", "Nieuw", null, null, "2026-09-27", 2)
        assertEquals(listOf(fresh), MakerNews.toNotify(maker, channel(fresh), today))
    }

    @Test
    fun `oud, al bekend of al gemeld is geen melding`() {
        val old = ChannelShow("4", "Oud maar in de lijst", null, null, "2018-04-24", 195)
        val known = ChannelShow("1", "Al bekend", null, null, "2026-09-29", 1)
        val told = ChannelShow("5", "Al gemeld", null, null, "2026-09-29", 1)
        val result = MakerNews.toNotify(maker.copy(notifiedShowIds = listOf("5")), channel(old, known, told), today)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `zonder kanaal of zonder eerste blik geen melding`() {
        val fresh = ChannelShow("3", "Nieuw", null, null, "2026-09-29", 1)
        assertTrue(MakerNews.toNotify(maker, null, today).isEmpty())
        assertTrue(MakerNews.toNotify(maker.copy(knownShowIds = emptyList()), channel(fresh), today).isEmpty())
    }
}
