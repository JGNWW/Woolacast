package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import nl.woolacast.ui.theme.CoverColors
import nl.woolacast.ui.theme.CoverSeed
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.Ink
import nl.woolacast.ui.theme.argbToOklch
import nl.woolacast.ui.theme.coverColors

/** Of het scherm nu donker is; het kader van de app beslist, niet het toestel. */
@Composable
fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** "ff6e00" → kleur; null bij iets dat geen kleur is. */
fun makerColor(hex: String?): Color? =
    hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/**
 * Kanaalkleur die op [background] zichtbaar blijft: in donker naar het lichte
 * inkt gemengd tot hij 3,2:1 haalt. Een zwart logo verdwijnt anders.
 */
fun liftedFor(color: Color, background: Color, toward: Color): Color {
    if (contrast(color, background) >= 3.2f) return color
    var t = 0f
    while (t <= 1f) {
        val mixed = lerp(color, toward, t)
        if (contrast(mixed, background) >= 3.2f) return mixed
        t += 0.02f
    }
    return toward
}

/** De kanaalkleur zoals hij op dit scherm getoond wordt. */
@Composable
fun screenMakerColor(hex: String?): Color? {
    val raw = makerColor(hex) ?: return null
    return if (isDarkSurface()) {
        liftedFor(raw, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onSurface)
    } else raw
}

/**
 * Het logo van een maker: altijd rond, zodat een maker nooit op een
 * podcasthoes lijkt. Met kanaal het logo van Apple op een schijf in de
 * kanaalkleur; zonder kanaal een neutraal monogram.
 */
@Composable
fun MakerLogo(
    name: String,
    logoUrl: String?,
    color: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    val background = MaterialTheme.colorScheme.background
    // Een logo dat bijna de kleur van de achtergrond heeft, krijgt een duidelijke rand.
    val faint = makerColor(color)?.let { contrast(it, background) < 3f } == true
    val ring = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark && faint) 0.40f else if (dark) 0.16f else 0.10f)
    val disc = screenMakerColor(color)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(disc ?: MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            AsyncImage(model = logoUrl, contentDescription = null, modifier = Modifier.size(size))
        } else {
            Text(
                monogram(name),
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.34f).sp,
                letterSpacing = 0.4.sp,
                // Op een kanaalkleur de inkt die het meeste contrast geeft, zoals onAccent.
                color = when {
                    disc == null -> MaterialTheme.colorScheme.onSurface
                    contrast(Color.White, disc) >= contrast(Ink, disc) -> Color.White
                    else -> Ink
                },
                maxLines = 1
            )
        }
    }
}

/** Twee beginletters: "Radio Oost" → "RO", "BNR Nieuwsradio" → "BN". */
fun monogram(name: String): String {
    val words = name.split(' ', '-', '.').filter { it.firstOrNull()?.isLetterOrDigit() == true }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        words.size == 1 -> words[0].take(2)
        else -> "?"
    }.uppercase()
}

/**
 * De Beeldgloed-kleuren van een kanaal: dezelfde rollen als bij een hoes, maar
 * uit de kanaalkleur in plaats van uit de pixels. Zwart, wit en grijs hebben
 * geen tint; dan geeft Beeldgloed zijn neutrale gloed, net als bij een grijze hoes.
 */
@Composable
fun makerCoverColors(hex: String?): CoverColors {
    val seed = makerColor(hex)?.let { color ->
        val lch = argbToOklch(color.toArgb())
        if (lch[1] < 0.03f) null else CoverSeed(hue = lch[2], chroma = lch[1])
    }
    return coverColors(seed, isDarkSurface())
}

/**
 * De gloed van een maker bovenaan zijn pagina: dezelfde laag als op
 * Hitlijsten, tot boven in de statusbalk. Zonder kanaal geen gloed.
 */
@Composable
fun MakerGlow(color: String?) {
    if (color == null) return
    CoverBackdrop(
        url = null,
        colors = makerCoverColors(color),
        showCover = false,
        glowTo = 560f, blurTop = 120f, blurTo = 500f,
        fade = listOf(260f to 0f, 560f to 1f)
    )
}
