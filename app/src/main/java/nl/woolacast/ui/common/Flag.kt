package nl.woolacast.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.domain.Catalog

/**
 * Landvlag. Eén stijl voor alle landen: het emoji van het systeem. Getekende
 * vlaggen zagen er naast de emoji's van de landen die niet te tekenen zijn
 * (VK, VS) rommelig uit.
 */
@Composable
fun Flag(countryCode: String, modifier: Modifier = Modifier, width: Dp = 20.dp, height: Dp = 14.dp, corner: Dp = 3.dp) {
    Box(modifier = modifier.size(width, height + 6.dp), contentAlignment = Alignment.Center) {
        Text(
            Catalog.country(countryCode).flag,
            fontSize = (height.value * 1.15f).sp,
            lineHeight = (height.value * 1.3f).sp,
            maxLines = 1
        )
    }
}
