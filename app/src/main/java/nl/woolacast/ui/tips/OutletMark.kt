package nl.woolacast.ui.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.woolacast.ui.theme.DisplayFamily

/* De merkkleuren van de media zijn niet van ons; dit is een neutrale reeks uit
   het eigen palet, vast per naam zodat een medium er altijd hetzelfde uitziet. */
private val MARK_COLORS = listOf(
    Color(0xFF1A1A18), Color(0xFFB5482A), Color(0xFF14504E), Color(0xFF332F63),
    Color(0xFF7E3149), Color(0xFF2B4C7E), Color(0xFF3C5A2B), Color(0xFF7E5AA0)
)

/** De beginletters van een medium in een gekleurd blokje. */
@Composable
fun OutletMark(outlet: String, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val initials = outlet.split(' ', '.')
        .filter { it.isNotBlank() && it.first().isUpperCase() }
        .take(3)
        .joinToString("") { it.first().toString() }
        .ifEmpty { outlet.take(2).uppercase() }
    val colour = MARK_COLORS[(outlet.hashCode().mod(MARK_COLORS.size))]
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size / 3.5f)).background(colour),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            fontFamily = DisplayFamily,
            fontSize = (size.value * (if (initials.length > 2) 0.30f else 0.38f)).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFBF6EE)
        )
    }
}
