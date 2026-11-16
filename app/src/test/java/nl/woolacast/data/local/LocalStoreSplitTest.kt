package nl.woolacast.data.local

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** De opslag in losse bestanden: de bibliotheek klein, de lijsten elk apart, en wat erbij kwam. */
class LocalStoreSplitTest {

    private val dir: File = Files.createTempDirectory("store").toFile()
    private val file = File(dir, "store.json")

    @After fun clean() {
        dir.deleteRecursively()
    }

    private fun opened() = LocalStore(file).also { runBlocking { it.load() } }

    private fun episode(id: String, show: String = "s") = SavedEpisode(id, show, "Show", "Aflevering $id", audioUrl = "https://example.com/$id.mp3")

    @Test
    fun `een lijst staat in zijn eigen bestand en niet in de bibliotheek`() = runBlocking {
        val store = opened()
        store.setTheme("dark")
        store.recordChart("APPLE:nl:all:SHOWS", mapOf("a" to 1, "b" to 2), CachedChart("2026-01-01", entries = listOf(CachedEntry(1, "a", "A", "Maker"))))
        store.flush()

        assertFalse("de bibliotheek draagt geen momentopnames meer", file.readText().contains("snapshots"))
        assertEquals(1, File(dir, "store-lists").listFiles()?.size)
        // Een nieuwe start leest de lijst terug uit zijn eigen bestand.
        assertEquals(listOf("a"), opened().cachedChart("APPLE:nl:all:SHOWS")?.entries?.map { it.id })
        assertNull(opened().cachedChart("SPOTIFY:nl:all:SHOWS"))
    }

    @Test
    fun `een bestand van voor de splitsing wordt uit elkaar gehaald`() = runBlocking {
        file.writeText(
            """{"follows":[{"id":"s","title":"S","publisher":"P"}],"theme":"dark",
               "snapshots":{"APPLE:nl:all:SHOWS":[{"date":"2020-01-01","ranks":{"a":3}}]},
               "charts":{"APPLE:nl:all:SHOWS":{"fetchedAt":"2020-01-02","entries":[{"rank":1,"id":"a","title":"A","publisher":"M"}]}},
               "liveTips":{"nl":{"fetchedAt":"2020-01-02T00:00:00Z"}}}"""
        )
        val store = opened()
        assertEquals(listOf("s"), store.follows.value.map { it.id })
        assertEquals(mapOf("a" to 3), store.baseline("APPLE:nl:all:SHOWS"))
        assertEquals(listOf("a"), store.cachedChart("APPLE:nl:all:SHOWS")?.entries?.map { it.id })
        assertEquals("2020-01-02T00:00:00Z", store.cachedTips("nl")?.fetchedAt)

        store.flush()
        assertFalse(file.readText().contains("snapshots"))
        // En een tweede start vindt alles nog steeds.
        val again = opened()
        assertEquals("dark", again.theme.value)
        assertEquals(mapOf("a" to 3), again.baseline("APPLE:nl:all:SHOWS"))
    }

    @Test
    fun `de momentopname van vandaag is geen basislijn`() = runBlocking {
        val store = opened()
        store.record("k", mapOf("a" to 1))
        assertNull(store.baseline("k"))
    }

    @Test
    fun `keuzes en instellingen blijven staan`() = runBlocking {
        val store = opened()
        store.setPrefs { it.copy(chartCountry = "de", chartLevel = "EPISODES", openPlayerOnPlay = true) }
        store.setPlayback { it.copy(skipSilence = true, speed = 1.35f) }
        store.setShowPrefs("s") { it.copy(speed = 1.5f, autoQueue = true) }
        store.flush()

        val again = opened()
        assertEquals("de", again.prefs.value.chartCountry)
        assertTrue(again.prefs.value.openPlayerOnPlay)
        assertEquals(1.35f, again.playback.value.speed)
        assertEquals(1.5f, again.showPrefs.value["s"]?.speed)

        // Is er niets bijzonders meer aan een show, dan verdwijnt zijn regel.
        again.setShowPrefs("s") { ShowPrefs() }
        assertTrue(again.showPrefs.value.isEmpty())
    }

    @Test
    fun `de wachtrij laat zich herschikken`() = runBlocking {
        val store = opened()
        store.enqueueAll(listOf(episode("a"), episode("b"), episode("c")))
        store.moveInQueue("c", 0)
        assertEquals(listOf("c", "a", "b"), store.queue.value.map { it.id })
        store.moveInQueue("c", 99)
        assertEquals(listOf("a", "b", "c"), store.queue.value.map { it.id })
        store.clearQueue()
        assertTrue(store.queue.value.isEmpty())
    }

    @Test
    fun `geschiedenis, uitluisteren en met de hand markeren`() = runBlocking {
        val store = opened()
        store.touchHistory(episode("a"))
        store.touchHistory(episode("b"))
        store.touchHistory(episode("a"))
        assertEquals("de laatst gespeelde achteraan, zonder dubbele", listOf("b", "a"), store.history.value.map { it.episode.id })

        store.rememberProgress("a", 3_590_000L, 3_600_000L)
        assertTrue(store.history.value.first { it.episode.id == "a" }.finished)
        assertTrue("a" in store.listened.value)

        // Opnieuw beginnen maakt hem weer bezig.
        store.touchHistory(episode("a"))
        assertFalse("a" in store.listened.value)

        store.rememberProgress("b", 120_000L, 3_600_000L)
        store.setListened(listOf("b"), true)
        assertTrue("b" in store.listened.value)
        assertNull("beluisterd markeren wist waar je was", store.progress.value["b"])
        store.setListened(listOf("b"), false)
        assertFalse("b" in store.listened.value)
    }

    @Test
    fun `luistertijd telt op per show en per dag`() = runBlocking {
        val store = opened()
        store.addListening("s", "Show", null, 60_000L)
        store.addListening("s", "Show", "https://example.com/a.jpg", 30_000L)
        store.addListening("s", "Show", null, 0L)
        assertEquals(90_000L, store.listening.value["s"]?.ms)
        assertEquals("https://example.com/a.jpg", store.listening.value["s"]?.artworkUrl)
        assertEquals(90_000L, store.listeningDays.value.values.sum())
    }

    @Test
    fun `een bladwijzer op dezelfde seconde komt er maar een keer in`() = runBlocking {
        val store = opened()
        store.addBookmark(episode("a"), 61_200L)
        store.addBookmark(episode("a"), 61_900L)
        store.addBookmark(episode("a"), 95_000L)
        assertEquals(2, store.bookmarks.value.size)
        store.removeBookmark(store.bookmarks.value.first().id)
        assertEquals(listOf(95_000L), store.bookmarks.value.map { it.positionMs })
    }

    @Test
    fun `een reservekopie zet de bibliotheek terug en laat downloads met rust`() = runBlocking {
        val store = opened()
        store.toggleFollow(FollowedShow("s", "S", "P", feedUrl = "https://example.com/feed"))
        store.enqueueAll(listOf(episode("a")))
        store.rememberProgress("a", 90_000L, 3_600_000L)
        store.setPrefs { it.copy(chartCountry = "be") }
        store.putDownload(DownloadRecord(episode("oud"), "oud.mp3", DownloadState.DONE))
        val backup = store.exportBackup()
        assertFalse("downloads gaan niet mee", backup.contains("oud.mp3"))

        val other = LocalStore(File(dir, "ander.json")).apply { load() }
        other.putDownload(DownloadRecord(episode("hier"), "hier.mp3", DownloadState.DONE))
        other.toggleFollow(FollowedShow("weg", "Weg", ""))
        assertTrue(other.importBackup(backup))
        assertEquals(listOf("s"), other.follows.value.map { it.id })
        assertEquals(listOf("a"), other.queue.value.map { it.id })
        assertEquals(90_000L, other.progress.value["a"])
        assertEquals("be", other.prefs.value.chartCountry)
        assertEquals("wat hier gedownload is, blijft", setOf("hier"), other.downloads.value.keys)

        assertFalse("een willekeurig bestand is geen reservekopie", other.importBackup("""{"follows":[]}"""))
        assertFalse(other.importBackup("geen json"))
        assertEquals(listOf("s"), other.follows.value.map { it.id })
    }
}
