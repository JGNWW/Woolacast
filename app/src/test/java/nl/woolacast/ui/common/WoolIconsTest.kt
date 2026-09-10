package nl.woolacast.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * De iconen worden bij het opstarten uit padstrings gebouwd; een tikfout
 * daarin zou de app pas op een toestel laten crashen. Hier parsen ze op de JVM.
 */
class WoolIconsTest {

    @Test
    fun allIconsParse() {
        val icons = WoolIcons::class.java.declaredFields
            .filter { it.type.simpleName == "ImageVector" }
            .onEach { it.isAccessible = true }
            .map { it.name to it.get(WoolIcons) }
        assertTrue("geen iconen gevonden", icons.size > 30)
        icons.forEach { (name, vector) ->
            assertTrue("icoon $name is leeg", vector != null)
        }
    }

    @Test
    fun datesAndDurations() {
        assertEquals("8 sep", shortDate("2026-09-08"))
        assertEquals("15 min", minutes(15 * 60_000L + 30_000L))
        assertEquals("nog 18 min", remaining(18 * 60_000L + 5_000L))
        assertEquals("nog 1 u 12 min", remaining(72 * 60_000L))
        assertEquals("52:18", clock(52 * 60_000L + 18_000L))
        assertEquals("1:02:03", clock(3600_000L + 2 * 60_000L + 3_000L))
        assertEquals("1,2×", speedLabel(1.2f))
        assertEquals("1×", speedLabel(1f))
        assertEquals("1,75×", speedLabel(1.75f))
    }

    @Test
    fun cadenceFromGaps() {
        assertEquals("wekelijks", cadence(listOf("2026-09-08", "2026-09-01", "2026-08-25", "2026-08-18")))
        assertEquals("dagelijks", cadence(listOf("2026-09-08", "2026-09-07", "2026-09-06", "2026-09-05")))
        assertNull(cadence(listOf("2026-09-08", "2026-09-01")))
    }
}
