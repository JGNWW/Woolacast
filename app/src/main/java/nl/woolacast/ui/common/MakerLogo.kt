package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import nl.woolacast.ui.theme.DisplayFamily

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
    val ring = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark && faint) 0.35f else if (dark) 0.16f else 0.10f)
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
                color = if (disc != null) Color.White else MaterialTheme.colorScheme.onSurface,
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
 * De gloed van een maker bovenaan zijn pagina: 8% van de kanaalkleur (16% in
 * donker), tot boven in de statusbalk en naadloos naar de achtergrond. Zonder
 * kanaal is er geen gloed.
 */
@Composable
fun BoxScope.MakerGlow(color: String?, height: Dp = 330.dp) {
    // Zwart, wit en grijs zijn geen merkkleur maar de achtergrond van een logo: geen gloed.
    val raw = makerColor(color) ?: return
    if (maxOf(raw.red, raw.green, raw.blue) - minOf(raw.red, raw.green, raw.blue) < 0.12f) return
    val tint = screenMakerColor(color) ?: return
    val base = MaterialTheme.colorScheme.background
    val top = tint.copy(alpha = if (isDarkSurface()) 0.16f else 0.08f).compositeOver(base)
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(0f to top, 0.45f to top, 1f to base))
    )
}
