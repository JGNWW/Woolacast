package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/*
 * Een maker heeft geen eigen beeld; hij krijgt het gezicht van zijn podcasts.
 * Op zijn pagina een muur van hoezen, in lijsten een vierkantje met vier hoezen.
 * Beide schalen mee met hoeveel podcasts er zijn: met één of twee is een muur
 * alleen maar herhaling.
 */

/**
 * De kop van een makerpagina: vierkant, zo breed als het scherm.
 * - 3 of meer hoezen: rijen onder een hoek. Elke rij begint een hoes verder,
 *   zodat twee gelijke hoezen nooit naast of schuin onder elkaar staan.
 * - 2 hoezen: een duo, twee grote hoezen die elk een kant op kantelen.
 * - 1 hoes: die hoes zelf, zoals op de podcastpagina.
 */
@Composable
fun MakerWall(urls: List<String?>, modifier: Modifier = Modifier) {
    val covers = urls.filterNotNull().distinct().take(9)
    Box(modifier.clipToBounds()) {
        when (covers.size) {
            0 -> Unit
            1 -> AsyncImage(
                model = covers[0], contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            2 -> Duo(covers)
            else -> Wall(covers)
        }
    }
}

@Composable
private fun Wall(covers: List<String>) {
    val n = covers.size
    // Stap per rij: 1 bij weinig hoezen; 3 bij zes of meer, dan lopen er geen diagonalen van dezelfde hoes.
    val step = if (n >= 6) 3 else 1
    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart, unbounded = true)) {
        Column(
            modifier = Modifier
                .offset(x = (-30).dp, y = (-50).dp)
                .rotate(-10f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) { row ->
                Row(
                    modifier = Modifier.offset(x = (-40 - row * 52).dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(6) { col ->
                        WallCover(covers[(col + row * step) % n], 128.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Duo(covers: List<String>) {
    Box(Modifier.fillMaxSize()) {
        WallCover(
            covers[1], 230.dp,
            Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = 60.dp).rotate(8f)
        )
        WallCover(
            covers[0], 230.dp,
            Modifier.align(Alignment.TopStart).offset(x = (-18).dp, y = 36.dp).rotate(-7f)
        )
    }
}

@Composable
private fun WallCover(url: String, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.11f)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .requiredSize(size)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/**
 * Een maker in een lijst: vier hoezen in één vierkant, met de radius van een
 * hoes. Nooit één hele hoes naast een naam die geen podcast is, behalve als de
 * maker er maar één heeft; dan is dat eerlijk gezegd ook zijn gezicht.
 * 3 hoezen: de eerste groot links, twee klein rechts. 2: twee helften.
 */
@Composable
fun MakerTile(urls: List<String?>, size: Dp, modifier: Modifier = Modifier, corner: Dp = if (size < 48.dp) 9.dp else 11.dp) {
    val covers = urls.filterNotNull().distinct().take(4)
    val gap = 1.5.dp
    Box(
        modifier
            .size(size)
            .shadow(1.dp, RoundedCornerShape(corner), clip = false)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        when (covers.size) {
            0 -> Unit
            1 -> Part(covers[0], Modifier.fillMaxSize())
            2 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Part(covers[0], Modifier.weight(1f).fillMaxHeight())
                Part(covers[1], Modifier.weight(1f).fillMaxHeight())
            }
            3 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Part(covers[0], Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Part(covers[1], Modifier.weight(1f).fillMaxWidth())
                    Part(covers[2], Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                listOf(covers.subList(0, 2), covers.subList(2, 4)).forEach { pair ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        pair.forEach { Part(it, Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Part(url: String, modifier: Modifier) {
    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}
