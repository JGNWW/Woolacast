package nl.woolacast.ui.common

/** De maten die we bij Apple vragen; een paar vaste, zodat de beeldcache ze deelt. */
private val ARTWORK_STEPS = intArrayOf(100, 160, 240, 400, 600)

/** Het laatste stuk van een Apple-hoesadres, zoals `/600x600bb.jpg`. */
private val APPLE_SIZE = Regex("""/\d+x\d+bb\.(png|jpg|webp)$""")

/**
 * Een hoes op de maat waarop hij getoond wordt. Apple zet de maat letterlijk
 * in het adres, en de lijsten vragen overal 600×600: zo'n 100 kB per hoes, waar
 * een rij van 44 dp met 11 kB toekan. Andere adressen (feeds, Spotify) blijven
 * zoals ze zijn.
 */
fun sizedArtwork(url: String?, px: Int): String? {
    if (url == null || "mzstatic.com/" !in url) return url
    val step = ARTWORK_STEPS.firstOrNull { it >= px } ?: return url
    return APPLE_SIZE.replace(url) { "/${step}x${step}bb.${it.groupValues[1]}" }
}
