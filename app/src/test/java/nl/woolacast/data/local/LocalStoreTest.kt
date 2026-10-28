package nl.woolacast.data.local

import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalStoreTest {

    private val file = File.createTempFile("store", ".json").apply { delete(); deleteOnExit() }

    private fun reopened() = LocalStore(file).also { runBlocking { it.load() } }

    @Test
    fun `een wijziging is meteen te zien en staat na flush op schijf`() = runBlocking {
        val store = LocalStore(file).apply { load() }
        store.toggleFollow(FollowedShow("s", "S", ""))
        assertEquals(listOf("s"), store.follows.value.map { it.id })
        assertFalse("nog niet weggeschreven", file.exists())

        store.flush()
        assertEquals(listOf("s"), reopened().follows.value.map { it.id })
        assertFalse(File(file.path + ".tmp").exists())
    }

    @Test
    fun `wat tegelijk gebeurt, gaat vanzelf in een keer naar schijf`() = runBlocking {
        val store = LocalStore(file).apply { load() }
        store.toggleFollow(FollowedShow("a", "A", ""))
        store.toggleFollow(FollowedShow("b", "B", ""))
        delay(3_000)
        assertEquals(listOf("a", "b"), reopened().follows.value.map { it.id })
    }

    @Test
    fun `voortgang tijdens het spelen wacht, tot er iets anders verandert`() = runBlocking {
        val store = LocalStore(file).apply { load() }
        store.rememberProgress("e", 60_000L, 3_600_000L, whilePlaying = true)
        delay(3_000)
        assertFalse("de tik tijdens het spelen mag wachten", file.exists())

        store.setTheme("dark")
        delay(3_000)
        assertEquals(60_000L, reopened().progress.value["e"])
    }

    @Test
    fun `niets veranderd is niets te schrijven`() = runBlocking {
        val store = LocalStore(file).apply { load() }
        store.markOpened("onbekend")
        store.flush()
        assertFalse(file.exists())

        store.setTheme("dark")
        store.flush()
        assertTrue(file.exists())
    }
}
