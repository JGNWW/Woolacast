package nl.woolacast.player

import nl.woolacast.domain.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterStopTest {
    private val chapters = listOf(Chapter(0, "a"), Chapter(60_000, "b"), Chapter(120_000, "c"))

    @Test
    fun `het einde van het hoofdstuk waar je nu bent`() {
        assertEquals(60_000L, chapterStop(chapters, 10_000, 200_000))
        // Na een sprong naar het begin van b telt het einde van b, niet van a.
        assertEquals(120_000L, chapterStop(chapters, 60_000, 200_000))
        assertEquals(200_000L, chapterStop(chapters, 130_000, 200_000))
    }
}
