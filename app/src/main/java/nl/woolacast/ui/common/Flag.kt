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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.domain.Catalog

private val Paper = Color(0xFFFFFDFA)

/**
 * Getekende vlaggen in de stijl van de mockup: een afgerond vlakje met
 * banen, in één stijl voor alle landen. De ingewikkelde (VK, VS, Canada,
 * Australië, Brazilië) zijn vereenvoudigd tot wat op 20 px nog herkenbaar is.
 */
private val FLAGS: Map<String, DrawScope.() -> Unit> = mapOf(
    "nl" to { horizontal(listOf(Color(0xFFAE1C28), Paper, Color(0xFF21468B))) },
    "de" to { horizontal(listOf(Color(0xFF1A1A18), Color(0xFFC8102E), Color(0xFFF4C300))) },
    "es" to { horizontal(listOf(Color(0xFFC60B1E), Color(0xFFFFC400), Color(0xFFFFC400), Color(0xFFC60B1E))) },
    "in" to { horizontal(listOf(Color(0xFFFF9933), Paper, Color(0xFF138808))) },
    "be" to { vertical(listOf(Color(0xFF1A1A18), Color(0xFFF4D02B), Color(0xFFC8102E))) },
    "fr" to { vertical(listOf(Color(0xFF22458F), Paper, Color(0xFFC8102E))) },
    "it" to { vertical(listOf(Color(0xFF009246), Paper, Color(0xFFCE2B37))) },
    "ie" to { vertical(listOf(Color(0xFF169B62), Paper, Color(0xFFFF883E))) },
    "mx" to { vertical(listOf(Color(0xFF006847), Paper, Color(0xFFCE1126))) },
    "se" to { cross(Color(0xFF006AA7), Color(0xFFFECC00)) },
    "dk" to { cross(Color(0xFFC8102E), Paper) },
    "no" to { cross(Color(0xFFBA0C2F), Paper); crossInner(Color(0xFF00205B)) },
    "jp" to { drawRect(Paper); drawCircle(Color(0xFFBC002D), radius = size.height * 0.3f) },
    "gb" to { unionJack(Rect(Offset.Zero, size)) },
    "us" to { unitedStates() },
    "ca" to { canada() },
    "au" to { australia() },
    "br" to { brazil() }
)

/** Vlag van 20×14 (standaard) met een dunne inzetrand, zoals .flag in de mockup. */
@Composable
fun Flag(countryCode: String, modifier: Modifier = Modifier, width: Dp = 20.dp, height: Dp = 14.dp, corner: Dp = 3.dp) {
    val shape = RoundedCornerShape(corner)
    val draw = FLAGS[countryCode.lowercase()]
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .border(1.dp, Color(0x1F211D17), shape),
        contentAlignment = Alignment.Center
    ) {
        if (draw == null) {
            Text(Catalog.country(countryCode).flag, fontSize = (height.value * 0.9f).sp)
        } else {
            Canvas(Modifier.size(width, height)) { draw() }
        }
    }
}

/* ---- tekenhulpjes; +0.5 px overlap tegen naden tussen banen ---- */

private fun DrawScope.horizontal(colors: List<Color>) {
    val h = size.height / colors.size
    colors.forEachIndexed { i, c -> drawRect(c, Offset(0f, i * h), Size(size.width, h + 0.5f)) }
}

private fun DrawScope.vertical(colors: List<Color>) {
    val w = size.width / colors.size
    colors.forEachIndexed { i, c -> drawRect(c, Offset(i * w, 0f), Size(w + 0.5f, size.height)) }
}

private fun DrawScope.cross(field: Color, cross: Color, thickness: Float = 0.22f) {
    drawRect(field)
    val bar = size.height * thickness
    drawRect(cross, Offset(size.width * 0.28f, 0f), Size(bar, size.height))
    drawRect(cross, Offset(0f, (size.height - bar) / 2f), Size(size.width, bar))
}

/** Het smallere binnenkruis van Noorwegen. */
private fun DrawScope.crossInner(color: Color) {
    val outer = size.height * 0.22f
    val bar = outer * 0.5f
    drawRect(color, Offset(size.width * 0.28f + (outer - bar) / 2f, 0f), Size(bar, size.height))
    drawRect(color, Offset(0f, (size.height - bar) / 2f), Size(size.width, bar))
}

private fun DrawScope.unionJack(rect: Rect) {
    val blue = Color(0xFF012169)
    val red = Color(0xFFC8102E)
    drawRect(blue, rect.topLeft, rect.size)
    val h = rect.height
    // Diagonalen: wit breed, rood smal erbovenop.
    listOf(rect.topLeft to rect.bottomRight, rect.topRight to rect.bottomLeft).forEach { (a, b) ->
        drawLine(Paper, a, b, strokeWidth = h * 0.2f)
        drawLine(red, a, b, strokeWidth = h * 0.07f)
    }
    // Kruis: wit breed, rood smal.
    drawRect(Paper, Offset(rect.center.x - h * 0.16f, rect.top), Size(h * 0.32f, h))
    drawRect(Paper, Offset(rect.left, rect.center.y - h * 0.16f), Size(rect.width, h * 0.32f))
    drawRect(red, Offset(rect.center.x - h * 0.09f, rect.top), Size(h * 0.18f, h))
    drawRect(red, Offset(rect.left, rect.center.y - h * 0.09f), Size(rect.width, h * 0.18f))
}

private fun DrawScope.unitedStates() {
    val red = Color(0xFFB31942)
    val stripe = size.height / 13f
    drawRect(Paper)
    for (i in 0 until 13 step 2) drawRect(red, Offset(0f, i * stripe), Size(size.width, stripe + 0.3f))
    val canton = Size(size.width * 0.4f, stripe * 7f)
    drawRect(Color(0xFF0A3161), Offset.Zero, canton)
    // Een raster van stipjes suggereert de sterren; losse sterren zijn hier onleesbaar.
    val r = size.height * 0.03f
    for (row in 0 until 4) for (col in 0 until 5) {
        drawCircle(Paper, r, Offset(canton.width * (col + 0.5f) / 5f, canton.height * (row + 0.5f) / 4f))
    }
}

private fun DrawScope.canada() {
    val red = Color(0xFFD80621)
    drawRect(Paper)
    drawRect(red, Offset.Zero, Size(size.width * 0.25f, size.height))
    drawRect(red, Offset(size.width * 0.75f, 0f), Size(size.width * 0.25f, size.height))
    // Esdoornblad, sterk vereenvoudigd.
    val c = center
    val u = size.height
    val leaf = listOf(
        0f to -0.40f, 0.09f to -0.22f, 0.21f to -0.28f, 0.16f to -0.06f, 0.34f to -0.10f,
        0.29f to 0.04f, 0.37f to 0.14f, 0.13f to 0.20f, 0.03f to 0.20f, 0.03f to 0.42f,
        -0.03f to 0.42f, -0.03f to 0.20f, -0.13f to 0.20f, -0.37f to 0.14f, -0.29f to 0.04f,
        -0.34f to -0.10f, -0.16f to -0.06f, -0.21f to -0.28f, -0.09f to -0.22f
    )
    val path = Path()
    leaf.forEachIndexed { i, (x, y) ->
        val p = Offset(c.x + x * u, c.y + y * u)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, red)
}

private fun DrawScope.australia() {
    drawRect(Color(0xFF00008B))
    unionJack(Rect(Offset.Zero, Size(size.width * 0.5f, size.height * 0.5f)))
    val w = size.width
    val h = size.height
    // Commonwealth-ster linksonder, Zuiderkruis rechts — als stippen.
    drawCircle(Paper, h * 0.11f, Offset(w * 0.25f, h * 0.75f))
    listOf(0.75f to 0.18f, 0.62f to 0.42f, 0.85f to 0.48f, 0.75f to 0.82f, 0.70f to 0.55f).forEach { (x, y) ->
        drawCircle(Paper, h * 0.055f, Offset(w * x, h * y))
    }
}

private fun DrawScope.brazil() {
    drawRect(Color(0xFF009C3B))
    val w = size.width
    val h = size.height
    val rhombus = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.9f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.9f)
        lineTo(w * 0.1f, h * 0.5f)
        close()
    }
    drawPath(rhombus, Color(0xFFFFDF00))
    drawCircle(Color(0xFF002776), radius = h * 0.26f)
}
