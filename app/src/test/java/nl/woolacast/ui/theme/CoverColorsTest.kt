package nl.woolacast.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * De kleuren die met de hoes meekleuren moeten bij elke hoes leesbaar blijven
 * en mogen bij een warme hoes niet bruin worden: dat zijn de twee dingen die de
 * criticus bij het ontwerp het vaakst afkeurde.
 */
class CoverColorsTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    private fun solid(argb: Int, n: Int = 400) = IntArray(n) { argb }

    private val red = 0xFFC8372D.toInt()
    private val teal = 0xFF1F8A8A.toInt()
    private val amber = 0xFFE0A526.toInt()
    private val neon = 0xFF39FF14.toInt()

    @Test
    fun seedFindsTheHueOfTheCover() {
        val seed = coverSeedFromPixels(solid(teal))
        assertNotNull(seed)
        seed!!
        assertEquals(argbToOklch(teal)[2], seed.hue, 3f)
        val redSeed = coverSeedFromPixels(solid(red))!!
        assertEquals(argbToOklch(red)[2], redSeed.hue, 3f)
    }

    @Test
    fun dominantColourWinsOverASmallSplash() {
        val pixels = IntArray(1000) { if (it < 850) teal else red }
        val seed = coverSeedFromPixels(pixels)!!
        assertEquals(argbToOklch(teal)[2], seed.hue, 5f)
    }

    @Test
    fun greyBlackAndWhiteCoversHaveNoSeed() {
        assertNull(coverSeedFromPixels(solid(0xFF808080.toInt())))
        assertNull(coverSeedFromPixels(solid(0xFF000000.toInt())))
        assertNull(coverSeedFromPixels(solid(0xFFFFFFFF.toInt())))
        // Een zwart-wit portret met een piepklein logo blijft grijs.
        assertNull(coverSeedFromPixels(IntArray(1000) { if (it < 20) red else 0xFF303030.toInt() }))
    }

    @Test
    fun accentIsReadableOnTheDarkBaseForEveryCover() {
        listOf(red, teal, amber, neon, 0xFF1B2A8C.toInt(), 0xFF6A1B9A.toInt()).forEach { argb ->
            val colors = coverColors(coverSeedFromPixels(solid(argb)), dark = true)
            assertTrue("accent op basis ${Integer.toHexString(argb)}", contrast(colors.accent, NightBg) >= 4.5f)
            assertTrue("accent op gloed ${Integer.toHexString(argb)}", contrast(colors.accent, colors.glow) >= 3f)
            assertTrue("tekst op accent ${Integer.toHexString(argb)}", contrast(colors.onAccent, colors.accent) >= 4.5f)
            assertTrue("tekst op mini-speler ${Integer.toHexString(argb)}", contrast(NightInk, colors.mini) >= 7f)
            assertTrue("tekst op gloed ${Integer.toHexString(argb)}", contrast(NightInk, colors.glow) >= 7f)
        }
    }

    @Test
    fun accentIsReadableOnTheLightBaseForEveryCover() {
        listOf(red, teal, amber, neon).forEach { argb ->
            val colors = coverColors(coverSeedFromPixels(solid(argb)), dark = false)
            assertTrue("accent op papier ${Integer.toHexString(argb)}", contrast(colors.accent, PaperBg) >= 4.5f)
            assertTrue("tekst op accent ${Integer.toHexString(argb)}", contrast(colors.onAccent, colors.accent) >= 4.5f)
        }
    }

    @Test
    fun warmCoversDoNotTurnDarkSurfacesBrown() {
        val colors = coverColors(coverSeedFromPixels(solid(amber)), dark = true)
        // De oude bruine vlakken hadden een chroma rond 0,02; de gloed mag daar net
        // omheen zitten, de mini-speler moet vrijwel neutraal zijn.
        assertTrue(argbToOklch(colors.glow.toArgbInt())[1] <= 0.03f)
        assertTrue(argbToOklch(colors.mini.toArgbInt())[1] <= 0.014f)
        assertTrue(colors.bleedAlpha < coverColors(coverSeedFromPixels(solid(teal)), dark = true).bleedAlpha)
    }

    @Test
    fun withoutASeedTheBrandTakesOver() {
        val colors = coverColors(null, dark = true)
        assertEquals(EmberLight, colors.accent)
        assertTrue(contrast(colors.onAccent, colors.accent) >= 4.5f)
    }

    @Test
    fun oklchRoundTrips() {
        listOf(red, teal, amber).forEach { argb ->
            val lch = argbToOklch(argb)
            val back = oklch(lch[0], lch[1], lch[2]).toArgbInt()
            assertEquals(argb and 0xFFFFFF, back and 0xFFFFFF, 0x030303)
        }
    }

    private fun Color.toArgbInt(): Int =
        (0xFF shl 24) or ((red * 255 + 0.5f).toInt() shl 16) or ((green * 255 + 0.5f).toInt() shl 8) or (blue * 255 + 0.5f).toInt()

    private fun assertEquals(expected: Int, actual: Int, tolerancePerChannel: Int) {
        val tol = tolerancePerChannel and 0xFF
        for (shift in listOf(16, 8, 0)) {
            val e = (expected shr shift) and 0xFF
            val a = (actual shr shift) and 0xFF
            assertTrue("kanaal $shift: $e tegen $a", kotlin.math.abs(e - a) <= tol)
        }
    }
}
