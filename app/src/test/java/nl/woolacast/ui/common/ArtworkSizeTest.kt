package nl.woolacast.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkSizeTest {

    private val apple = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts221/v4/15/9c/a3/x/mza_1.jpg/600x600bb.jpg"

    @Test
    fun appleCoverGetsTheNextStepUp() {
        assertEquals(apple.replace("600x600bb", "160x160bb"), sizedArtwork(apple, 132))
        assertEquals(apple.replace("600x600bb", "100x100bb"), sizedArtwork(apple, 100))
        assertEquals(apple.replace("600x600bb", "400x400bb"), sizedArtwork(apple, 348))
    }

    @Test
    fun largerThanTheStepsStaysAsItWas() {
        assertEquals(apple, sizedArtwork(apple, 1200))
    }

    @Test
    fun keepsTheFileType() {
        val png = apple.replace("600x600bb.jpg", "170x170bb.png")
        assertEquals(png.replace("170x170bb", "240x240bb"), sizedArtwork(png, 200))
    }

    @Test
    fun otherAddressesAreLeftAlone() {
        val feed = "https://example.org/art/600x600bb.jpg"
        assertEquals(feed, sizedArtwork(feed, 100))
        assertEquals("https://i.scdn.co/image/ab67656300005f1f", sizedArtwork("https://i.scdn.co/image/ab67656300005f1f", 100))
        assertNull(sizedArtwork(null, 100))
    }
}
