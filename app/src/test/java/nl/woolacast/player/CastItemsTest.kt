package nl.woolacast.player

import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.woolacast.domain.Episode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Wat er naar een speaker gaat: nooit een bestand op de telefoon. */
@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CastItemsTest {

    private val episode = Episode(
        id = "afl-1", showId = "deadline", showTitle = "De Deadline", title = "Live vanuit Paradiso",
        description = null, artworkUrl = "https://example.com/hoes.jpg",
        audioUrl = "https://op3.dev/e/example.com/afl1.m4a?src=rss", durationMillis = 60_000L, releaseDate = "2026-09-08"
    )

    @Test
    fun `een gedownloade aflevering gaat met het adres uit de feed`() {
        val local = episode.toMediaItem("file:///data/user/0/nl.woolacast/files/downloads/abc.m4a")
        val remote = local.forRemote()
        assertEquals("https://op3.dev/e/example.com/afl1.m4a?src=rss", remote.localConfiguration?.uri.toString())
        assertEquals(MimeTypes.AUDIO_MP4, remote.localConfiguration?.mimeType)
        assertEquals("afl-1", remote.mediaId)
    }

    @Test
    fun `een gestreamde aflevering houdt zijn adres en krijgt een soort`() {
        val item = episode.copy(audioUrl = "https://example.com/afl.mp3").let { it.toMediaItem(it.audioUrl!!) }
        assertEquals("https://example.com/afl.mp3", item.forRemote().localConfiguration?.uri.toString())
        assertEquals(MimeTypes.AUDIO_MPEG, item.forRemote().localConfiguration?.mimeType)
    }

    @Test
    fun `zonder extensie is het mp3`() {
        assertEquals(MimeTypes.AUDIO_MPEG, audioMimeFor("https://dts.podtrac.com/redirect/abc"))
        assertEquals(MimeTypes.AUDIO_OGG, audioMimeFor("https://example.com/a.ogg?x=1"))
    }

    @Test
    fun `de omzetter geeft de speaker het feedadres`() {
        val local = episode.toMediaItem("file:///data/user/0/nl.woolacast/files/downloads/abc.m4a")
        val queued = FeedUrlConverter().toMediaQueueItem(local)
        assertEquals("https://op3.dev/e/example.com/afl1.m4a?src=rss", queued.media?.contentUrl ?: queued.media?.contentId)
        assertEquals(MimeTypes.AUDIO_MP4, queued.media?.contentType)
    }
}
