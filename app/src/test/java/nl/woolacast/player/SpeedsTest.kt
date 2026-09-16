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
