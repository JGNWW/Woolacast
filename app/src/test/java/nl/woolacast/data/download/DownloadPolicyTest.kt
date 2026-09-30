package nl.woolacast.data.download

import java.time.Instant
import nl.woolacast.data.local.DownloadRecord
import nl.woolacast.data.local.DownloadSettings
import nl.woolacast.data.local.DownloadState
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.domain.Episode
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadPolicyTest {

    private val now = Instant.parse("2026-10-01T12:00:00Z")
    private val mb = 1024L * 1024L

    private fun record(id: String, mbs: Long, auto: Boolean, added: String, listened: String? = null) = DownloadRecord(
        episode = SavedEpisode(id = id, showId = "s", showTitle = "Show", title = id),
        fileName = "$id.mp3", state = DownloadState.DONE, bytes = mbs * mb, auto = auto,
        addedAt = added, listenedAt = listened
    )

    @Test
    fun `uitgeluisterd gaat na een dag weg, maar niet uit de wachtrij`() {
        val records = listOf(
            record("oud", 50, false, "2026-09-01T00:00:00Z", listened = "2026-09-30T10:00:00Z"),
            record("net", 50, false, "2026-09-01T00:00:00Z", listened = "2026-10-01T10:00:00Z"),
            record("wachtrij", 50, false, "2026-09-01T00:00:00Z", listened = "2026-09-29T10:00:00Z")
        )
        val doomed = DownloadPolicy.toDelete(records, DownloadSettings(), protected = setOf("wachtrij"), now = now)
        assertEquals(listOf("oud"), doomed)
    }

    @Test
    fun `boven de grens eerst de oudste automatische, nooit wat je zelf koos`() {
        val records = listOf(
            record("zelf", 400, false, "2026-09-01T00:00:00Z"),
            record("auto-oud", 400, true, "2026-09-02T00:00:00Z"),
            record("auto-nieuw", 400, true, "2026-09-03T00:00:00Z")
        )
        val doomed = DownloadPolicy.toDelete(records, DownloadSettings(limitMb = 1000), protected = emptySet(), now = now)
        assertEquals(listOf("auto-oud"), doomed)
    }

    @Test
    fun `automatisch klaarzetten pakt de nieuwste die je nog niet hoorde`() {
        fun ep(id: String) = Episode(id, "s", "Show", id, null, null, "https://x/$id.mp3", null, null)
        val feed = listOf(ep("e5"), ep("e4"), ep("e3"), ep("e2"))
        val existing = listOf(
            record("e3", 30, true, "2026-09-01T00:00:00Z").copy(listenedAt = null),
            record("e2", 30, true, "2026-09-01T00:00:00Z")
        )
        val (add, drop) = DownloadPolicy.autoPlan(feed, count = 2, existing = existing, listened = setOf("e5"), protected = emptySet())
        assertEquals(listOf("e4"), add.map { it.id })
        assertEquals(listOf("e2"), drop)
    }
}
