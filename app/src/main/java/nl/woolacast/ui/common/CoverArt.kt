package nl.woolacast.ui.common

import android.os.Build
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.woolacast.ui.theme.CoverColors
import nl.woolacast.ui.theme.CoverEdge
import nl.woolacast.ui.theme.CoverSeed
import nl.woolacast.ui.theme.NightInk
import nl.woolacast.ui.theme.NightOutline
import nl.woolacast.ui.theme.NightInkSoft
import nl.woolacast.ui.theme.coverColors
import nl.woolacast.ui.theme.coverEdgeFromPixels
import nl.woolacast.ui.theme.coverSeedFromPixels

/*
 * "Beeldgloed": de hoes staat over de volle breedte, lost onderaan op en zijn
 * kleur lekt naar beneden weg. Maten komen uit het ontwerp op 390 dp breed en
 * schalen mee met de schermbreedte, zodat de verhouding tot de hoes klopt.
 */

/** Wat we uit een hoes halen: de hoofdtint (accent) en de kleur van de onderrand (vlakken). */
@androidx.compose.runtime.Immutable
data class CoverTones(val seed: CoverSeed?, val edge: CoverEdge?)

private val tonesCache = LruCache<String, CoverTones>(64)

/** De hoofdtint van een hoes, uit een verkleinde kopie. Null zolang hij laadt en bij een grijze hoes. */
@Composable
fun rememberCoverSeed(url: String?): CoverSeed? = rememberCoverTones(url)?.seed

/** Hoofdtint en onderrand van een hoes. Null zolang hij laadt. */
@Composable
fun rememberCoverTones(url: String?): CoverTones? {
    val context = LocalContext.current
    val cached = url?.let { tonesCache.get(it) }
    val tones by produceState(cached, url) {
        // Bij een andere hoes (ook een die al in de cache staat) altijd opnieuw
        // zetten: produceState houdt anders de waarde van de vorige vast.
        if (url == null) { value = null; return@produceState }
        if (cached != null) { value = cached; return@produceState }
        // Nog niet geladen: de vorige kleur blijft staan tot de nieuwe er is, zodat
        // er bij het wisselen niet eerst het merkaccent tussendoor flitst.
        val request = ImageRequest.Builder(context)
            // Voor de kleur is een kleine hoes genoeg, en die is er eerder.
            .data(sizedArtwork(url, 100))
            .size(48)
            .allowHardware(false)
            .build()
        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        val drawable = (result as? SuccessResult)?.drawable ?: return@produceState
        val found = withContext(Dispatchers.Default) {
            val bitmap = drawable.toBitmap(48, 48)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            CoverTones(coverSeedFromPixels(pixels), coverEdgeFromPixels(pixels, bitmap.width))
        }
        tonesCache.put(url, found)
        value = found
    }
    return tones
}

/** De meekleurende rollen voor deze hoes, met een zachte overgang als de hoes wisselt. */
@Composable
fun rememberCoverColors(url: String?): CoverColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tones = rememberCoverTones(url)
    val target = coverColors(tones?.seed, dark, tones?.edge)
    val spec = tween<Color>(durationMillis = 450)
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val onAccent by animateColorAsState(target.onAccent, spec, label = "onAccent")
    val glow by animateColorAsState(target.glow, spec, label = "glow")
    val mini by animateColorAsState(target.mini, spec, label = "mini")
    return target.copy(accent = accent, onAccent = onAccent, glow = glow, mini = mini)
}

/**
 * Hoe ver de lijst gescrold is, in pixels vanaf de bovenkant, zolang de kop nog
 * in de buurt is. Een LazyColumn kent alleen de positie van zijn eerste
 * zichtbare item; de hoogtes van de items erboven onthouden we zelf.
 */
@Composable
fun rememberListScrollPx(state: LazyListState, tracked: Int = 12): () -> Float {
    val sizes = remember(state) { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.map { it.index to it.size } }
            .collect { items -> items.forEach { (index, size) -> if (index < tracked) sizes[index] = size } }
    }
    return remember(state) { { scrollOf(state, sizes, tracked) } }
}

private fun scrollOf(state: LazyListState, sizes: Map<Int, Int>, tracked: Int): Float {
    val first = state.firstVisibleItemIndex
    if (first >= tracked) return Float.MAX_VALUE
    var above = 0f
    for (i in 0 until first) above += sizes[i] ?: return Float.MAX_VALUE
    return above + state.firstVisibleItemScrollOffset
}

private fun dimmed(brightness: Float, saturation: Float): ColorFilter {
    val matrix = ColorMatrix().apply { setToSaturation(saturation) }
    matrix.timesAssign(
        ColorMatrix(
            floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

/** Vervagen kan pas vanaf Android 12; daarvoor blijft het bij de gloedkleur. */
private val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * De achtergrond van een Beeldgloed-scherm: gloedkleur, een vervaagde kopie
 * van de hoes, de scherpe hoes met een oplossende onderrand, en een verloop
 * naar de basis. [scrollPx] schuift alles mee met de inhoud.
 *
 * [fade] is het verloop terug naar de basis: punten (ontwerp-dp vanaf de
 * bovenkant, dekking). Na het laatste punt blijft die dekking staan.
 *
 * [extendTo] rekt het uitlopende deel op tot dat punt (vanaf de bovenkant van
 * de pagina), zodat de gloed ook bij een lange kop tot in de lijst doorloopt.
 */
@Composable
fun CoverBackdrop(
    url: String?,
    colors: CoverColors,
    modifier: Modifier = Modifier,
    scrollPx: () -> Float = { 0f },
    glowTo: Float = 1080f,
    blurTo: Float = 950f,
    fade: List<Pair<Float, Float>> = listOf(440f to 0f, 700f to 0.35f, 920f to 0.75f, 1080f to 1f),
    blurTop: Float = 170f,
    showCover: Boolean = true,
    extendTo: Dp? = null,
    /** In plaats van de hoes zelf, bijvoorbeeld de muur van een maker; zelfde plek en zelfde oplossende rand. */
    art: (@Composable (Modifier) -> Unit)? = null
) {
    val base = MaterialTheme.colorScheme.background
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = maxWidth
        val k = width / 390.dp
        fun d(v: Float): Dp = (v * k).dp
        // Alles onder de hoes (vanaf 440) schaalt mee naar het gevraagde eindpunt.
        val lastY = fade.last().first
        val endY = extendTo?.let { maxOf(lastY, it.value / k) } ?: lastY
        val s = if (lastY > 440f) (endY - 440f) / (lastY - 440f) else 1f
        fun stretch(y: Float) = if (y <= 440f) y else 440f + (y - 440f) * s
        @Suppress("NAME_SHADOWING") val fade = fade.map { stretch(it.first) to it.second }
        @Suppress("NAME_SHADOWING") val glowTo = stretch(glowTo)
        @Suppress("NAME_SHADOWING") val blurTo = stretch(blurTo)
        Box(
            Modifier
                .fillMaxWidth()
                // Hoger dan het scherm, maar aan de bovenkant verankerd (requiredHeight
                // zou de laag centreren en alles omhoog schuiven).
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(d(maxOf(glowTo, fade.last().first) + 400f))
                .graphicsLayer {
                    val y = scrollPx()
                    translationY = -y.coerceAtMost(size.height + 1f)
                    alpha = if (y == Float.MAX_VALUE) 0f else 1f
                }
        ) {
            // 1. De gloedkleur zelf, zodat er ook zonder vervagen kleur uitlekt.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(d(glowTo))
                    .background(Brush.verticalGradient(0f to colors.glow, 0.3f to colors.glow, 1f to colors.glow.copy(alpha = 0f)))
            )
            // 2. De vervaagde hoes, gedimd en minder verzadigd bij warme tinten.
            if (canBlur && url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = dimmed(colors.bleedBrightness, colors.bleedSaturation),
                    modifier = Modifier
                        .offset(y = d(blurTop))
                        .requiredWidth(width + d(180f))
                        .requiredHeight(d(blurTo - blurTop))
                        // Dekking en vervaging in één laag die niet knipt: Modifier.alpha
                        // knipt wel, en dan kreeg de vervaagde kopie harde randen.
                        .graphicsLayer {
                            alpha = colors.bleedAlpha
                            val radius = 64.dp.toPx()
                            renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                            clip = false
                        }
                        // Boven- en onderrand lopen al vóór het vervagen weg, zodat er
                        // nergens een rand van de kopie te zien is.
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.2f to Color.Black,
                                    0.65f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            }
            // 3. Terug naar de basis: geleidelijk, zodat de gloed doorloopt in de lijst.
            val top = fade.first().first
            val span = fade.last().first - top + 400f
            Box(
                Modifier
                    .offset(y = d(top))
                    .fillMaxWidth()
                    .height(d(span))
                    .background(
                        Brush.verticalGradient(
                            *(fade.map { (y, a) -> ((y - top) / span) to base.copy(alpha = a) } +
                                (1f to base.copy(alpha = fade.last().second))).toTypedArray()
                        )
                    )
            )
            // 4. De hoes over de volle breedte; de onderrand lost op in de gloed.
            if (showCover && (url != null || art != null)) {
                val heroModifier = Modifier
                    .size(width)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black,
                                0.55f to Color.Black,
                                0.8f to Color.Black.copy(alpha = 0.4f),
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                if (art != null) {
                    art(heroModifier)
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = heroModifier
                    )
                }
                // 5. Zachte scrim onder de titel, zodat die op elke hoes leesbaar is.
                Box(
                    Modifier
                        .offset(y = d(190f))
                        .fillMaxWidth()
                        .height(d(440f))
                        .background(
                            Brush.verticalGradient(
                                0f to base.copy(alpha = 0f),
                                0.38f to base.copy(alpha = 0.45f),
                                0.62f to base.copy(alpha = 0.3f),
                                1f to base.copy(alpha = 0f)
                            )
                        )
                )
            }
        }
        // Statusbalk: een vleugje donker, zodat klok en batterij op een lichte hoes leesbaar blijven.
        if (showCover) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 1f to Color.Transparent))
            )
        }
    }
}

/** Donker glas voor knoppen en labels boven op de hoes (.72: haalt AA ook op een witte hoes). */
val GlassColor = Color(0xB80E0F11)

@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(GlassColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = NightInk, modifier = Modifier.size(21.dp))
    }
}

/** Omlijnde ronde knop op de gloed: 1,5 dp rand in 50% ink. */
@Composable
fun OutlineCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    filled: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    outline: Color = outlineOnGlow()
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(MaterialTheme.colorScheme.onSurface)
                else Modifier.border(1.5.dp, outline, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = if (filled) MaterialTheme.colorScheme.background else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** De omlijning die op elke gloed 3:1 haalt; in het lichte thema de gewone rand. */
@Composable
fun outlineOnGlow(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) NightOutline
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

/** De grote afspeelknop in de hoeskleur. */
@Composable
fun AccentPlayButton(
    playing: Boolean,
    colors: CoverColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    playLabel: String = "Afspelen"
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) WoolIcons.Pause else WoolIcons.Play,
            if (playing) "Pauzeren" else playLabel,
            tint = colors.onAccent,
            modifier = Modifier.size(size * 0.44f)
        )
    }
}

/**
 * Drie staafjes: "speelt nu", overal dezelfde maat (12 dp). Met [animated] dansen
 * ze, zolang er echt geluid is; gepauzeerd staan ze stil.
 */
@Composable
fun Equalizer(color: Color, modifier: Modifier = Modifier, animated: Boolean = false) {
    // Vaste standen als hij stilstaat; bewegend elk een eigen tempo, zodat het niet als één blok op en neer gaat.
    val bars: List<() -> Float> = if (animated) {
        val transition = rememberInfiniteTransition(label = "equalizer")
        listOf(
            Triple(0.3f, 1f, 430),
            Triple(1f, 0.35f, 330),
            Triple(0.55f, 0.95f, 520)
        ).map { (from, to, ms) ->
            val state = transition.animateFloat(
                initialValue = from,
                targetValue = to,
                animationSpec = infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar"
            )
            ({ state.value })
        }
    } else {
        listOf({ 7f / 12f }, { 1f }, { 9f / 12f })
    }
    Box(
        modifier = modifier
            .size(width = 13.dp, height = 12.dp)
            .semantics { contentDescription = if (animated) "Speelt nu" else "Gepauzeerd" }
            // Alleen tekenen, niet opnieuw opmaken: de staafjes bewegen elk beeld.
            .drawBehind {
                val bar = 3.dp.toPx()
                val gap = 2.dp.toPx()
                val radius = CornerRadius(1.dp.toPx())
                bars.forEachIndexed { i, fraction ->
                    val h = size.height * fraction().coerceIn(0.15f, 1f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * (bar + gap), size.height - h),
                        size = Size(bar, h),
                        cornerRadius = radius
                    )
                }
            }
    )
}

/**
 * Ronde afspeelknop met een ring die laat zien hoe ver je in de aflevering bent.
 * Niet begonnen: een volle ring. Begonnen: een vage baan met daarop de voortgang
 * vanaf boven, met de klok mee. [progress] wordt bij het tekenen gelezen, zodat
 * een lopende aflevering de knop niet elke tik opnieuw opbouwt.
 */
@Composable
fun ProgressRingButton(
    playing: Boolean,
    color: Color,
    progress: () -> Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    playLabel: String = "Afspelen"
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                val stroke = 2.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val fraction = progress()
                if (fraction == null) {
                    drawCircle(color, radius = this.size.minDimension / 2f - inset, style = Stroke(stroke))
                } else {
                    drawCircle(color.copy(alpha = 0.28f), radius = this.size.minDimension / 2f - inset, style = Stroke(stroke))
                    val sweep = 360f * fraction.coerceIn(0f, 1f)
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) WoolIcons.Pause else WoolIcons.Play,
            if (playing) "Pauzeren" else playLabel,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Secundaire tekst op de gloed: 78% ink in het donker, de gewone variant in het licht. */
@Composable
fun softInk(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) NightInkSoft
    else MaterialTheme.colorScheme.onSurfaceVariant

/** Omlijnde pilknop (Volgen): 44 dp, rand 1,5 dp; gekozen = wit vlak, zoals een actieve chip. */
@Composable
fun OutlinePillButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(22.dp)
    val fg = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.onSurface)
                else Modifier.border(1.5.dp, outlineOnGlow(), shape)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .padding(start = 16.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
