package nl.woolacast.data.inbox

import java.time.LocalDate
import nl.woolacast.data.local.FeedCheck
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.SavedEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewRulesTest {

    private val today = LocalDate.parse("2026-10-01")

    private fun show(id: String, followedAt: String? = "2026-09-20", lastOpened: String? = null, notify: Boolean = false) =
        FollowedShow(id = id, title = id, publisher = "", feedUrl = "https://example.com/$id.xml",
            followedAt = followedAt, lastOpened = lastOpened, notifyNew = notify)

    private fun ep(id: String, show: String, date: String, minutes: Long? = 40) =
        SavedEpisode(id = id, showId = show, showTitle = show, title = id, audioUrl = "https://example.com/$id.mp3",
            durationMillis = minutes?.let { it * 60_000 }, releaseDate = date)

    @Test
    fun `nieuw is na de volgdag, niet uitgeluisterd en niet weggeveegd`() {
        val deadline = show("deadline")
        assertFalse("op de dag van volgen", NewRules.isNew(deadline, ep("a", "deadline", "2026-09-20"), emptySet(), emptySet(), today))
        assertTrue(NewRules.isNew(deadline, ep("b", "deadline", "2026-09-21"), emptySet(), emptySet(), today))
        assertFalse("uitgeluisterd", NewRules.isNew(deadline, ep("b", "deadline", "2026-09-21"), setOf("b"), emptySet(), today))
        assertFalse("weggeveegd", NewRules.isNew(deadline, ep("b", "deadline", "2026-09-21"), emptySet(), setOf("b"), today))
        assertFalse("zonder datum", NewRules.isNew(deadline, ep("c", "deadline", ""), emptySet(), emptySet(), today))
    }

    @Test
    fun `ouder dan dertig dagen is geen nieuws meer`() {
        val old = show("oud", followedAt = "2026-01-01")
        assertTrue(NewRules.isNew(old, ep("a", "oud", "2026-09-01"), emptySet(), emptySet(), today))
        assertFalse(NewRules.isNew(old, ep("b", "oud", "2026-08-31"), emptySet(), emptySet(), today))
    }

    @Test
    fun `wie volgde van voor deze regel telt vanaf zijn laatste bezoek, of een week`() {
        val visited = show("bezocht", followedAt = null, lastOpened = "2026-09-28")
        assertFalse(NewRules.isNew(visited, ep("a", "bezocht", "2026-09-28"), emptySet(), emptySet(), today))
        assertTrue(NewRules.isNew(visited, ep("b", "bezocht", "2026-09-29"), emptySet(), emptySet(), today))
        val never = show("nooit", followedAt = null)
        assertFalse(NewRules.isNew(never, ep("c", "nooit", "2026-09-24"), emptySet(), emptySet(), today))
        assertTrue(NewRules.isNew(never, ep("d", "nooit", "2026-09-25"), emptySet(), emptySet(), today))
    }

    @Test
    fun `de lijst is de nieuwste eerst, over alle shows`() {
        val follows = listOf(show("a"), show("b"))
        val checks = mapOf(
            "a" to FeedCheck("t", latest = listOf(ep("a2", "a", "2026-09-30"), ep("a1", "a", "2026-09-22"))),
            "b" to FeedCheck("t", latest = listOf(ep("b1", "b", "2026-10-01"), ep("b0", "b", "2026-09-10")))
        )
        val inbox = NewRules.inbox(follows, checks, listened = setOf("a1"), hidden = emptySet(), today = today)
        assertEquals(listOf("b1", "a2"), inbox.map { it.id })
    }

    @Test
    fun `een ontvolgde show telt niet mee`() {
        val checks = mapOf("weg" to FeedCheck("t", latest = listOf(ep("x", "weg", "2026-09-30"))))
        assertTrue(NewRules.inbox(listOf(show("a")), checks, emptySet(), emptySet(), today).isEmpty())
    }

    @Test
    fun `de chips stapelen`() {
        val items = listOf(
            ep("kort-oud", "a", "2026-09-22", minutes = 20),
            ep("kort-nieuw", "a", "2026-09-30", minutes = 20),
            ep("lang-nieuw", "a", "2026-09-29", minutes = 55),
            ep("begonnen", "a", "2026-09-30", minutes = 10),
            ep("onbekend", "a", "2026-09-30", minutes = null)
        )
        val progress = mapOf("begonnen" to 60_000L)
        assertEquals(listOf("kort-oud", "kort-nieuw", "begonnen"),
            NewRules.applyFilters(items, setOf(NewFilter.SHORT), progress, today).map { it.id })
        assertEquals(listOf("kort-nieuw"),
            NewRules.applyFilters(items, setOf(NewFilter.SHORT, NewFilter.THIS_WEEK, NewFilter.UNSTARTED), progress, today).map { it.id })
        assertEquals(items, NewRules.applyFilters(items, emptySet(), progress, today))
    }

    @Test
    fun `de eerste keer is er niets binnengekomen`() {
        val latest = listOf(ep("a", "s", "2026-09-30"))
        assertTrue(NewRules.arrivals(null, latest).isEmpty())
        assertTrue(NewRules.arrivals(FeedCheck(checkedAt = ""), latest).isEmpty())
        val before = FeedCheck("2026-09-29T10:00:00Z", latest = listOf(ep("b", "s", "2026-09-28")))
        assertEquals(listOf("a"), NewRules.arrivals(before, latest + ep("b", "s", "2026-09-28")).map { it.id })
    }

    @Test
    fun `alleen een melding bij shows met de melding aan, recent en nog niet gemeld`() {
        val follows = listOf(show("aan", notify = true), show("uit"))
        val arrivals = mapOf(
            "aan" to listOf(ep("nieuw", "aan", "2026-09-30"), ep("gemeld", "aan", "2026-09-30"), ep("teruggezet", "aan", "2026-09-25")),
            "uit" to listOf(ep("stil", "uit", "2026-10-01"))
        )
        val fresh = NewRules.toNotify(arrivals, follows, notified = setOf("gemeld"), listened = emptySet(), hidden = emptySet(), today = today)
        assertEquals(listOf("nieuw"), fresh.map { it.id })
    }

    @Test
    fun `alles in de wachtrij slaat over wat er al staat en stopt bij twintig`() {
        val items = (1..30).map { ep("e$it", "s", "2026-09-30") }
        val picked = NewRules.forQueue(items, queued = setOf("e1", "e2"))
        assertEquals(NewRules.QUEUE_LIMIT, picked.size)
        assertEquals("e3", picked.first().id)
    }
}
