package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

/* ---- bouwstenen uit de mockup: maatvoering en kleur één op één ---- */

@Composable
fun Artwork(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 11.dp,
    elevation: Dp = 1.dp
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(size))
        }
    }
}

/** 44 px raakvlak met een 24 px lijnicoon, zoals .ibtn in de mockup. */
@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Het merkje links in de balk. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        "WOOLACAST",
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.7.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/** App-balk van 52 dp: merk links, acties rechts. */
@Composable
fun MarkBar(actions: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Wordmark()
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** App-balk met terugpijl en een titel, voor onderliggende schermen. */
@Composable
fun TitleBar(title: String?, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconAction(WoolIcons.Back, "Terug", onBack)
        if (title != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.5.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp)
    )
}

/** Kleine kop in kapitalen boven een blok (.lbl2). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 9.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing?.invoke()
    }
}

/** Sectiekop met een kleine actie rechts (.sect). */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/** Tekstlinkje in accentkleur, zoals "Alles" en "Deze week". */
@Composable
fun LinkText(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

/**
 * Tabbladen zoals in de mockup: losse woorden met 24 dp ertussen en een 3 dp
 * accentstreep onder het actieve, in plaats van Material's volle breedte.
 */
@Composable
fun UnderlineTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp).height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (on) MaterialTheme.colorScheme.onSurface else LocalChartColors.current.muted
                    )
                    if (on) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun RankNumber(rank: Int, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val colors = LocalChartColors.current
    Text(
        text = rank.toString(),
        modifier = modifier.width(size),
        textAlign = TextAlign.End,
        fontFamily = DisplayFamily,
        fontSize = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.6).sp,
        color = if (rank <= 3) colors.rankAccent else colors.muted
    )
}

/** Beweging in een lijstrij: pijl plus getal, rechts uitgelijnd (.mv). */
@Composable
fun MovementBadge(movement: Movement, modifier: Modifier = Modifier) {
    val colors = LocalChartColors.current
    Row(
        modifier = modifier.width(40.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (movement) {
            is Movement.Up -> {
                Icon(WoolIcons.Up, null, tint = colors.rise, modifier = Modifier.size(11.dp))
                movement.places?.let {
                    Text("$it", color = colors.rise, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            is Movement.Down -> {
                Icon(WoolIcons.Down, null, tint = colors.fall, modifier = Modifier.size(11.dp))
                movement.places?.let {
                    Text("$it", color = colors.fall, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Movement.New -> Text(
                "NIEUW",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Movement.Flat -> Text("–", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            // Nog geen eerdere meting: pas na een tweede dag valt er iets te zeggen.
            Movement.Unknown -> Unit
        }
    }
}

/** Beweging als gekleurd pilletje, voor afleveringrijen (.pill). */
@Composable
fun MovementPill(movement: Movement, modifier: Modifier = Modifier) {
    val colors = LocalChartColors.current
    val (bg, fg, icon, text) = when (movement) {
        is Movement.Up -> Quad(colors.riseContainer, colors.onRiseContainer, WoolIcons.Up, movement.places?.toString().orEmpty())
        is Movement.Down -> Quad(colors.fallContainer, colors.onFallContainer, WoolIcons.Down, movement.places?.toString().orEmpty())
        Movement.New -> Quad(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, null, "NIEUW")
        Movement.Flat -> Quad(MaterialTheme.colorScheme.surfaceContainer, colors.muted, null, "–")
        Movement.Unknown -> return
    }
    Row(
        modifier = modifier
            .height(19.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(9.dp))
        Text(
            text, color = fg, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
            letterSpacing = if (movement == Movement.New) 0.6.sp else 0.sp
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/** Klein tekstpilletje in een vaste kleur, zoals "#3 NL" of "TOP". */
@Composable
fun TextPill(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(19.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = foreground, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Het vierkantje met de beginletter van een bron. */
@Composable
fun SourceDot(source: SourceId, modifier: Modifier = Modifier, size: Dp = 18.dp, active: Boolean = true) {
    val colors = LocalChartColors.current
    val bg = when {
        !active -> MaterialTheme.colorScheme.surfaceContainerHigh
        source == SourceId.APPLE -> colors.seriesApple
        else -> colors.seriesSpotify
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            source.initial,
            fontSize = (size.value * 0.53f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (active) Color(0xFFFBF6EE) else colors.muted
        )
    }
}

@Composable
fun SourceChip(
    source: SourceId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val colors = LocalChartColors.current
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) colors.panel else MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape))
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else colors.muted),
            contentAlignment = Alignment.Center
        ) {
            Text(
                source.initial,
                color = Color(0xFFFBF6EE),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            source.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPanel else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Ronde afspeelknop met rand, zoals naast elke afleveringrij (.pbtn). */
@Composable
fun PlayCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    playing: Boolean = false,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            playing -> Icon(WoolIcons.Pause, "Pauzeren", modifier = Modifier.size(20.dp))
            else -> Icon(WoolIcons.Play, "Afspelen", modifier = Modifier.size(20.dp))
        }
    }
}

enum class ButtonKind { PRIMARY, OUTLINE, TONAL }

/** De drie knopstijlen uit de mockup: 46 dp hoog, 13 dp hoeken. */
@Composable
fun WoolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(13.dp)
    val bg = when (kind) {
        ButtonKind.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonKind.OUTLINE -> Color.Transparent
        ButtonKind.TONAL -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when (kind) {
        ButtonKind.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .clip(shape)
            .background(bg)
            .then(if (kind == ButtonKind.OUTLINE) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.5.sp), color = fg, maxLines = 1)
    }
}

/** Vierkante 46 dp knop met rand voor één icoon (naast de knoppenrij op de podcastpagina). */
@Composable
fun SquareIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(20.dp))
    }
}

/** Filterchip (.chip): 36 dp, accent-container als hij aanstaat. */
@Composable
fun FilterChipBox(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leading?.invoke()
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = when {
                !enabled -> MaterialTheme.colorScheme.outline
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        trailing?.invoke()
    }
}

/** Uitleg in plaats van een lege lijst — een combinatie die niet bestaat is geen fout. */
@Composable
fun NoticePanel(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(WoolIcons.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            WoolButton(actionLabel, onAction, kind = ButtonKind.TONAL, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
