package nl.woolacast.player

import nl.woolacast.ui.common.speedLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In de melding is er geen keuzelijst: één knop loopt de snelheden rond. Dat
 * rondlopen moet elke stap raken en nergens blijven hangen — met floats is dat
 * geen vanzelfsprekendheid.
 */
class SpeedsTest {

    @Test
    fun cycleVisitsEverySpeedAndWrapsAround() {
        val seen = mutableListOf<Float>()
        var speed = SPEEDS.first()
        repeat(SPEEDS.size) {
            seen += speed
            speed = nextSpeed(speed)
        }
        assertEquals(SPEEDS, seen)
        assertEquals("na de laatste weer de eerste", SPEEDS.first(), speed)
    }

    @Test
    fun cycleSnapsBackFromASpeedThatIsNoLongerOffered() {
        // 1,75× kwam uit een eerdere versie; de knop hoort door te lopen.
        assertEquals(1.8f, nextSpeed(1.75f))
    }

    @Test
    fun everySpeedHasItsOwnLabel() {
        val labels = SPEEDS.map(::speedLabel)
        assertEquals(labels.size, labels.toSet().size)
        assertTrue("1× hoort erbij te zitten", labels.contains("1×"))
    }
}

class ChapterStopTest {
    private val chapters = listOf(
        nl.woolacast.domain.Chapter(0, "a"),
        nl.woolacast.domain.Chapter(60_000, "b"),
        nl.woolacast.domain.Chapter(120_000, "c")
    )

    @org.junit.Test
    fun `het einde van het hoofdstuk waar je nu bent`() {
        org.junit.Assert.assertEquals(60_000L, chapterStop(chapters, 10_000, 200_000))
        // Na een sprong naar het begin van b telt het einde van b, niet van a.
        org.junit.Assert.assertEquals(120_000L, chapterStop(chapters, 60_000, 200_000))
        org.junit.Assert.assertEquals(200_000L, chapterStop(chapters, 130_000, 200_000))
    }
}
