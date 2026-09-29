package nl.woolacast.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/*
 * Kleur uit de hoes ("Beeldgloed", design/donker-herontwerp).
 *
 * Een hoes levert één tint: de kleur die er het meest in zit, gewogen naar hoe
 * uitgesproken hij is. Uit die tint maken we een paar vaste rollen, elk op een
 * vaste lichtheid in OKLCH en met een begrensde verzadiging, zodat een neon-,
 * zwarte of witte hoes nooit een onleesbare of schreeuwende kleur oplevert.
 * Warme tinten (oranje tot geel) krijgen op donkere vlakken een extra strenge
 * grens: donker oranje wordt anders bruin, en dat was precies de klacht over
 * het oude donkere thema.
 */

/** Wat we uit een hoes halen: de hoofdtint en hoe kleurrijk hij is. Null = grijs of zwart-wit. */
@Immutable
data class CoverSeed(val hue: Float, val chroma: Float)

/** De rollen die met de hoes meekleuren. Al het andere blijft neutraal. */
@Immutable
data class CoverColors(
    /** Grote afspeelknop, ringen, voortgang, "Meer", de spelende titel. */
    val accent: Color,
    /** Tekst en iconen óp het accent. */
    val onAccent: Color,
    /** De gloed die vanuit de hoes naar beneden wegloopt. */
    val glow: Color,
    /** Achtergrond van de mini-speler. */
    val mini: Color,
    /** Dekking, helderheid en verzadiging van de vervaagde kopie van de hoes. */
    val bleedAlpha: Float,
    val bleedBrightness: Float,
    val bleedSaturation: Float
)

private const val BRAND_HUE = 45f

/** Oranje tot geel: donker worden die bruin, dus daar knijpen we de verzadiging harder af. */
fun isWarmHue(hue: Float): Boolean = hue in 40f..110f

fun coverColors(seed: CoverSeed?, dark: Boolean): CoverColors {
    // Een grijze of zwart-witte hoes heeft geen kleur om over te nemen: dan het merk
    // als accent en een neutrale gloed.
    val hue = seed?.hue ?: BRAND_HUE
    val chroma = seed?.chroma ?: 0f
    val warm = seed != null && isWarmHue(hue)
    return if (dark) {
        CoverColors(
            // Rood verbleekt op L .80 tot zalm (de gamut knijpt de verzadiging af); iets donkerder houdt het rood.
            accent = if (seed == null) EmberLight else oklch(if (hue < 40f || hue >= 340f) 0.77f else 0.80f, minOf(maxOf(chroma, 0.09f), 0.14f), hue),
            onAccent = if (seed == null) Color(0xFF2A1006) else oklch(0.22f, minOf(chroma, 0.05f), hue),
            glow = when {
                seed == null -> Color(0xFF1E1F23)
                warm -> oklch(0.31f, 0.024f, hue)
                else -> oklch(0.33f, minOf(chroma * 0.6f, 0.07f), hue)
            },
            mini = when {
                seed == null -> NightSurface3
                warm -> oklch(0.26f, 0.010f, hue)
                else -> oklch(0.26f, minOf(chroma * 0.3f, 0.032f), hue)
            },
            bleedAlpha = if (warm) 0.5f else 0.65f,
            bleedBrightness = if (warm) 0.32f else 0.4f,
            bleedSaturation = if (warm) 0.7f else 0.9f
        )
    } else {
        CoverColors(
            accent = if (seed == null) Ember else oklch(0.50f, minOf(maxOf(chroma, 0.09f), 0.13f), hue),
            onAccent = Color.White,
            glow = if (seed == null) PaperSurface2 else oklch(0.93f, minOf(chroma * 0.4f, 0.035f), hue),
            mini = if (seed == null) Ink else oklch(0.27f, minOf(chroma * 0.3f, if (warm) 0.012f else 0.03f), hue),
            bleedAlpha = 0.35f,
            bleedBrightness = 1.15f,
            bleedSaturation = 0.8f
        )
    }
}

/**
 * De hoofdtint uit de pixels van een (verkleinde) hoes. Pixels die nauwelijks
 * kleur hebben tellen niet mee; als bijna niets kleur heeft, is er geen tint.
 */
fun coverSeedFromPixels(pixels: IntArray): CoverSeed? {
    if (pixels.isEmpty()) return null
    val bins = 36
    val weight = FloatArray(bins)
    val sumSin = FloatArray(bins)
    val sumCos = FloatArray(bins)
    val sumChroma = FloatArray(bins)
    var chromatic = 0
    for (argb in pixels) {
        if ((argb ushr 24) < 128) continue
        val lch = argbToOklch(argb)
        val l = lch[0]
        val c = lch[1]
        if (c < 0.04f || l < 0.12f || l > 0.97f) continue
        chromatic++
        // Uitgesproken kleur in het middenbereik weegt het zwaarst.
        val w = c * (1f - kotlin.math.abs(l - 0.6f))
        val h = lch[2]
        val bin = ((h / 360f) * bins).toInt().coerceIn(0, bins - 1)
        weight[bin] += w
        val rad = Math.toRadians(h.toDouble())
        sumSin[bin] += (sin(rad) * w).toFloat()
        sumCos[bin] += (cos(rad) * w).toFloat()
        sumChroma[bin] += c * w
    }
    if (chromatic < pixels.size * 0.06f) return null
    // Een tint is een buurt van drie vakjes: zo wint een hoes met één duidelijke
    // kleur, ook als die net over een vakgrens valt.
    var best = -1
    var bestWeight = 0f
    for (i in 0 until bins) {
        val w = weight[i] + 0.5f * (weight[(i + 1) % bins] + weight[(i + bins - 1) % bins])
        if (w > bestWeight) { bestWeight = w; best = i }
    }
    if (best < 0 || bestWeight <= 0f) return null
    var s = 0f; var co = 0f; var cw = 0f; var ww = 0f
    for (d in -1..1) {
        val i = (best + d + bins) % bins
        s += sumSin[i]; co += sumCos[i]; cw += sumChroma[i]; ww += weight[i]
    }
    val hue = ((Math.toDegrees(atan2(s.toDouble(), co.toDouble())) + 360.0) % 360.0).toFloat()
    return CoverSeed(hue = hue, chroma = if (ww > 0f) cw / ww else 0f)
}

/* ---- OKLab / OKLCH (Björn Ottosson) ---- */

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

private fun linearToSrgb(c: Float): Float =
    if (c <= 0.0031308f) c * 12.92f else (1.055f * c.toDouble().pow(1.0 / 2.4) - 0.055f).toFloat()

/** [L, C, h in graden] van een ARGB-pixel. */
fun argbToOklch(argb: Int): FloatArray {
    val r = srgbToLinear(((argb shr 16) and 0xFF) / 255f)
    val g = srgbToLinear(((argb shr 8) and 0xFF) / 255f)
    val b = srgbToLinear((argb and 0xFF) / 255f)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    val lab0 = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
    val lab1 = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
    val lab2 = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
    val c = hypot(lab1, lab2)
    val h = ((Math.toDegrees(atan2(lab2.toDouble(), lab1.toDouble())) + 360.0) % 360.0).toFloat()
    return floatArrayOf(lab0, c, h)
}

private fun oklchToLinear(l: Float, c: Float, h: Float): FloatArray {
    val rad = Math.toRadians(h.toDouble())
    val a = (c * cos(rad)).toFloat()
    val b = (c * sin(rad)).toFloat()
    val l_ = l + 0.3963377774f * a + 0.2158037573f * b
    val m_ = l - 0.1055613458f * a - 0.0638541728f * b
    val s_ = l - 0.0894841775f * a - 1.2914855480f * b
    val l3 = l_ * l_ * l_
    val m3 = m_ * m_ * m_
    val s3 = s_ * s_ * s_
    return floatArrayOf(
        4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3,
        -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3,
        -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3
    )
}

/**
 * Een kleur op lichtheid [l] en tint [h]. Past de gevraagde verzadiging niet in
 * sRGB, dan zakt die tot hij past; de lichtheid (en dus het contrast) blijft staan.
 */
fun oklch(l: Float, c: Float, h: Float): Color {
    var chroma = c
    var rgb = oklchToLinear(l, chroma, h)
    var guard = 0
    while (rgb.any { it < -0.0005f || it > 1.0005f } && guard < 40) {
        chroma *= 0.92f
        rgb = oklchToLinear(l, chroma, h)
        guard++
    }
    return Color(
        red = linearToSrgb(rgb[0].coerceIn(0f, 1f)).coerceIn(0f, 1f),
        green = linearToSrgb(rgb[1].coerceIn(0f, 1f)).coerceIn(0f, 1f),
        blue = linearToSrgb(rgb[2].coerceIn(0f, 1f)).coerceIn(0f, 1f)
    )
}
