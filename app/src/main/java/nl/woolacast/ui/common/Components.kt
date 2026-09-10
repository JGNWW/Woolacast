package nl.woolacast.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.theme.LocalChartColors

@Composable
fun Artwork(url: String?, size: Dp, modifier: Modifier = Modifier, corner: Dp = 11.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun RankNumber(rank: Int, modifier: Modifier = Modifier) {
    val colors = LocalChartColors.current
    Text(
        text = rank.toString(),
        modifier = modifier.width(26.dp),
        textAlign = TextAlign.End,
        fontSize = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.6).sp,
        color = if (rank <= 3) colors.rankAccent else colors.muted
    )
}

@Composable
fun MovementBadge(movement: Movement, modifier: Modifier = Modifier) {
    val colors = LocalChartColors.current
    Row(
        modifier = modifier.width(40.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (movement) {
            is Movement.Up -> {
                Icon(Icons.Filled.ArrowDropUp, null, tint = colors.rise, modifier = Modifier.size(16.dp))
                Text("${movement.places}", color = colors.rise, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            is Movement.Down -> {
                Icon(Icons.Filled.ArrowDropDown, null, tint = colors.fall, modifier = Modifier.size(16.dp))
                Text("${movement.places}", color = colors.fall, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Movement.New -> Text(
                "NIEUW",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )

            Movement.Flat -> Text("–", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            // Nog geen eerdere meting: pas na een tweede dag valt er iets te zeggen.
            Movement.Unknown -> Unit
        }
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
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
            )
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else LocalChartColors.current.muted
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                source.initial,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            source.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
