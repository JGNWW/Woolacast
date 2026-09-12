package nl.woolacast.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.data.reco.Suggestion
import nl.woolacast.ui.theme.LocalChartColors

/**
 * Een rij voorgestelde podcasts, met onder elke tegel de reden waarom hij er
 * staat. Die reden is geen sier: een voorstel zonder uitleg is een gok, en de
 * lezer hoort te kunnen zien waar hij vandaan komt.
 */
@Composable
fun SuggestionRow(
    title: String,
    suggestions: List<Suggestion>,
    onOpen: (showId: String, feedUrl: String?, title: String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    if (suggestions.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = if (subtitle == null) 10.dp else 2.dp)
        )
        if (subtitle != null) {
            Text(
                subtitle,
                fontSize = 11.5.sp,
                color = LocalChartColors.current.muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Een sleutel moet uniek zijn, anders valt Compose om. Een voorstel
            // zonder id zou er twee lege sleutels van maken.
            items(suggestions.filter { it.entry.id.isNotBlank() }.distinctBy { it.entry.id },
                  key = { it.entry.id }) { suggestion ->
                val entry = suggestion.entry
                Column(
                    modifier = Modifier
                        .width(116.dp)
                        .clickable { onOpen(entry.id, entry.feedUrl, entry.title) }
                ) {
                    Artwork(entry.artworkUrl, 116.dp, corner = 14.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 16.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        suggestion.reason,
                        fontSize = 11.sp,
                        color = LocalChartColors.current.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
