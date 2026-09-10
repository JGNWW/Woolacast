package nl.woolacast.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.local.LocalStore
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.NoticePanel

@Composable
fun LibraryScreen(
    store: LocalStore,
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val follows by store.follows.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "WOOLACAST",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp)
        )
        Text(
            "Bibliotheek",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 14.dp)
        )

        if (follows.isEmpty()) {
            NoticePanel(
                title = "Nog niets gevolgd",
                message = "Open een podcast uit een hitlijst en tik op Volgen. " +
                    "Wat je volgt komt hier te staan."
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(follows, key = { it.id }) { show ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPodcast(show.id) },
                    horizontalAlignment = Alignment.Start
                ) {
                    Artwork(show.artworkUrl, 102.dp, corner = 13.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        show.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
