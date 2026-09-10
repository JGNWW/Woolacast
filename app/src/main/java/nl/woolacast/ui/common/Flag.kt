package nl.woolacast.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.domain.Catalog

private val Paper = Color(0xFFFFFDFA)

private sealed interface FlagSpec {
    data class Horizontal(val colors: List<Color>) : FlagSpec
    data class Vertical(val colors: List<Color>) : FlagSpec
    data class Cross(val field: Color, val cross: Color) : FlagSpec
    data class Disc(val field: Color, val disc: Color) : FlagSpec
}

/* De vlaggen uit de mockup, getekend; de rest valt terug op het emoji. */
private val SPECS = mapOf(
    "nl" to FlagSpec.Horizontal(listOf(Color(0xFFAE1C28), Paper, Color(0xFF21468B))),
    "be" to FlagSpec.Vertical(listOf(Color(0xFF1A1A18), Color(0xFFF4D02B), Color(0xFFC8102E))),
    "de" to FlagSpec.Horizontal(listOf(Color(0xFF1A1A18), Color(0xFFC8102E), Color(0xFFF4C300))),
    "fr" to FlagSpec.Vertical(listOf(Color(0xFF22458F), Paper, Color(0xFFC8102E))),
    "it" to FlagSpec.Vertical(listOf(Color(0xFF009246), Paper, Color(0xFFCE2B37))),
    "ie" to FlagSpec.Vertical(listOf(Color(0xFF169B62), Paper, Color(0xFFFF883E))),
    "es" to FlagSpec.Horizontal(listOf(Color(0xFFC60B1E), Color(0xFFFFC400), Color(0xFFFFC400), Color(0xFFC60B1E))),
    "mx" to FlagSpec.Vertical(listOf(Color(0xFF006847), Paper, Color(0xFFCE1126))),
    "in" to FlagSpec.Horizontal(listOf(Color(0xFFFF9933), Paper, Color(0xFF138808))),
    "se" to FlagSpec.Cross(Color(0xFF006AA7), Color(0xFFFECC00)),
    "dk" to FlagSpec.Cross(Color(0xFFC8102E), Paper),
    "no" to FlagSpec.Cross(Color(0xFFBA0C2F), Paper),
    "jp" to FlagSpec.Disc(Paper, Color(0xFFBC002D))
)

/** Vlag van 20×14 (standaard) met een dunne inzetrand, zoals .flag in de mockup. */
@Composable
fun Flag(countryCode: String, modifier: Modifier = Modifier, width: Dp = 20.dp, height: Dp = 14.dp, corner: Dp = 3.dp) {
    val shape = RoundedCornerShape(corner)
    val spec = SPECS[countryCode.lowercase()]
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .border(1.dp, Color(0x1F211D17), shape),
        contentAlignment = Alignment.Center
    ) {
        if (spec == null) {
            Text(Catalog.country(countryCode).flag, fontSize = (height.value * 0.9f).sp)
            return@Box
        }
        Canvas(Modifier.size(width, height)) {
            when (spec) {
                is FlagSpec.Horizontal -> {
                    val h = size.height / spec.colors.size
                    spec.colors.forEachIndexed { i, c ->
                        drawRect(c, Offset(0f, i * h), Size(size.width, h + 0.5f))
                    }
                }
                is FlagSpec.Vertical -> {
                    val w = size.width / spec.colors.size
                    spec.colors.forEachIndexed { i, c ->
                        drawRect(c, Offset(i * w, 0f), Size(w + 0.5f, size.height))
                    }
                }
                is FlagSpec.Cross -> {
                    drawRect(spec.field)
                    val bar = size.height * 0.22f
                    drawRect(spec.cross, Offset(size.width * 0.28f, 0f), Size(bar, size.height))
                    drawRect(spec.cross, Offset(0f, (size.height - bar) / 2f), Size(size.width, bar))
                }
                is FlagSpec.Disc -> {
                    drawRect(spec.field)
                    drawCircle(spec.disc, radius = size.height * 0.3f)
                }
            }
        }
    }
}
