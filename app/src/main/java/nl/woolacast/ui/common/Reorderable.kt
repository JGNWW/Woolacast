package nl.woolacast.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Een kolom waarvan de rijen te verslepen zijn aan een handvat. Bedoeld voor
 * korte lijsten zoals de wachtrij: alle rijen staan er tegelijk, dus er is geen
 * gedoe met rijen die tijdens het slepen uit beeld verdwijnen.
 *
 * Tijdens het slepen schuiven de andere rijen opzij; pas bij loslaten meldt
 * [onMove] de nieuwe plek. Wie niet kan slepen, verplaatst een rij met de
 * toegankelijkheidsacties "Omhoog" en "Omlaag" op het handvat.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (index: Int, item: T, handle: @Composable () -> Unit) -> Unit
) {
    var dragged by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val current by rememberUpdatedState(items)
    val move by rememberUpdatedState(onMove)

    // Hoeveel plekken de gesleepte rij is opgeschoven, afgeleid van hoe ver hij versleept is.
    fun shift(from: Int): Int {
        if (rowHeight == 0) return 0
        return (offset / rowHeight).roundToInt().coerceIn(-from, current.lastIndex - from)
    }

    Column(modifier) {
        items.forEachIndexed { index, item ->
            val id = key(item)
            key(id) {
                val from = items.indexOfFirst { key(it) == dragged }
                val target = if (from == -1) -1 else from + shift(from)
                // De gesleepte rij volgt de vinger; de rijen ertussen maken plaats.
                val translation = when {
                    from == -1 -> 0f
                    index == from -> offset
                    from < target && index in (from + 1)..target -> -rowHeight.toFloat()
                    target < from && index in target until from -> rowHeight.toFloat()
                    else -> 0f
                }
                Box(
                    Modifier
                        .zIndex(if (index == from) 1f else 0f)
                        .onGloballyPositioned { if (rowHeight == 0 || index == 0) rowHeight = it.size.height }
                        .graphicsLayer {
                            translationY = translation
                            if (index == from) shadowElevation = 12f
                        }
                ) {
                    row(index, item) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .semantics {
                                    contentDescription = "Verplaatsen"
                                    customActions = listOfNotNull(
                                        if (index > 0) CustomAccessibilityAction("Omhoog") { move(index, index - 1); true } else null,
                                        if (index < items.lastIndex) CustomAccessibilityAction("Omlaag") { move(index, index + 1); true } else null
                                    )
                                }
                                .pointerInput(id) {
                                    detectDragGestures(
                                        onDragStart = { dragged = id; offset = 0f },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            offset += amount.y
                                        },
                                        onDragEnd = {
                                            val start = current.indexOfFirst { key(it) == id }
                                            if (start != -1) {
                                                val end = start + shift(start)
                                                if (end != start) move(start, end)
                                            }
                                            dragged = null
                                            offset = 0f
                                        },
                                        onDragCancel = { dragged = null; offset = 0f }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(WoolIcons.Drag, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
