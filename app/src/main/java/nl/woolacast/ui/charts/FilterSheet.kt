package nl.woolacast.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.data.ChartRepository
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Category
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.Country
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.ButtonKind
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.Flag
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.LinkText
import nl.woolacast.ui.common.WoolButton
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.theme.LocalChartColors

private const val SHORTLIST = 5

/**
 * "Lijst instellen": bron, land en categorie in één sheet. Per bron staat erbij
 * wat hij kan, zodat je nooit op een lege lijst uitkomt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    query: ChartQuery,
    repository: ChartRepository,
    onDismiss: () -> Unit,
    onApply: (SourceId, Country, Category) -> Unit
) {
    var source by remember { mutableStateOf(query.source) }
    var country by remember { mutableStateOf(query.country) }
    var category by remember { mutableStateOf(query.category) }
    var allCountries by remember { mutableStateOf(Catalog.countries.indexOf(query.country) >= SHORTLIST) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val capabilities = repository.source(source).capabilities
    val categoriesAllowed = capabilities.supportsCategories(query.level)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lijst instellen", style = MaterialTheme.typography.titleLarge)
                IconAction(WoolIcons.Close, "Sluiten", onDismiss)
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Label("Bron", topPadding = 2.dp)
                repository.allSources().forEach { chartSource ->
                    SourceOption(
                        id = chartSource.id,
                        summary = chartSource.capabilities.summary,
                        selected = chartSource.id == source,
                        onSelect = { source = chartSource.id }
                    )
                }

                Label("Land") {
                    LinkText(if (allCountries) "Minder" else "Alle ${Catalog.countries.size}") {
                        allCountries = !allCountries
                    }
                }
                if (allCountries) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Catalog.countries.forEach { item ->
                            CountryTile(item, item.code == country.code) { country = item }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(Catalog.countries.take(SHORTLIST), key = { it.code }) { item ->
                            CountryTile(item, item.code == country.code) { country = item }
                        }
                        item {
                            MoreTile(Catalog.countries.size - SHORTLIST) { allCountries = true }
                        }
                    }
                }

                Label(
                    if (categoriesAllowed) "Categorie"
                    else "Categorie · niet op ${query.level.label.lowercase()}niveau bij ${source.label}"
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Catalog.categories.forEach { item ->
                        FilterChipBox(
                            label = item.label,
                            selected = item.appleGenreId == category.appleGenreId,
                            enabled = categoriesAllowed || item.isAll,
                            onClick = { category = item }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WoolButton(
                    "Herstel",
                    onClick = {
                        source = SourceId.APPLE
                        country = Catalog.defaultCountry
                        category = Catalog.defaultCategory
                    },
                    kind = ButtonKind.OUTLINE
                )
                WoolButton(
                    "Toon ${expectedCount(source, query.level, category)} ${query.level.label.lowercase()}",
                    onClick = { onApply(source, country, category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Hoe diep een lijst gaat; gemeten aan de bronnen zelf. */
private fun expectedCount(source: SourceId, level: ChartLevel, category: Category): Int = when {
    source == SourceId.APPLE && level == ChartLevel.EPISODES -> if (category.isAll) 100 else 50
    !category.isAll -> 50
    else -> 200
}

@Composable
private fun Label(text: String, topPadding: androidx.compose.ui.unit.Dp = 14.dp, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 9.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
        )
        trailing?.invoke()
    }
}

@Composable
private fun SourceOption(
    id: SourceId,
    summary: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val colors = LocalChartColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLowest
            )
            .border(
                1.5.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Radioknop zoals getekend: 22 dp ring, 11 dp stip.
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else colors.muted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Column(Modifier.weight(1f)) {
            Text(id.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = colors.muted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Vlag met landcode eronder; de gekozen krijgt een accentring (.ctry). */
@Composable
private fun CountryTile(country: Country, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .width(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp, 33.dp)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Flag(country.code, width = 36.dp, height = 25.dp, corner = 6.dp)
        }
        Text(
            country.code.uppercase(),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoreTile(count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .width(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .size(36.dp, 25.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("+$count", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Meer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
