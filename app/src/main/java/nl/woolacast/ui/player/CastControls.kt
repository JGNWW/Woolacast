package nl.woolacast.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.cast.MediaRouteButton
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import nl.woolacast.ui.common.GlassColor
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.outlineOnGlow
import nl.woolacast.ui.theme.NightInk

/** Wat er te casten valt: of er een speaker of tv is, en waar er nu op gespeeld wordt. */
data class CastRoutes(
    val available: Boolean,
    val playingOn: String?,
    /** Voor voorvertoningen en schermafdrukken: teken het cast-teken zonder in het netwerk te zoeken. */
    val preview: Boolean = false
)

/** Dezelfde ontvanger als Media3's standaardinstellingen in het manifest. */
private val castSelector: MediaRouteSelector by lazy {
    MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
        .build()
}

/**
 * Volgt het netwerk zolang de speler open is: welke speakers en tv's er zijn,
 * en of er een gekozen is. Zonder Google Play-diensten vindt Android geen
 * Cast-apparaten, en dan is er niets.
 */
@Composable
fun rememberCastRoutes(): CastRoutes {
    val context = LocalContext.current
    if (LocalInspectionMode.current) return CastRoutes(false, null)
    val router = remember(context) { runCatching { MediaRouter.getInstance(context.applicationContext) }.getOrNull() }
        ?: return CastRoutes(false, null)
    fun read(): CastRoutes = runCatching {
        val selected = router.selectedRoute
        val remote = selected.takeIf { !it.isDefault && !it.isBluetooth && it.matchesSelector(castSelector) }
        CastRoutes(
            available = router.isRouteAvailable(castSelector, MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE),
            playingOn = remote?.name
        )
    }.getOrDefault(CastRoutes(false, null))
    var routes by remember(router) { mutableStateOf(read()) }
    DisposableEffect(router) {
        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) { routes = read() }
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) { routes = read() }
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) { routes = read() }
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) { routes = read() }
            override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) { routes = read() }
        }
        // Zoeken alleen zolang dit scherm er is; dat spaart de batterij.
        router.addCallback(castSelector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        routes = read()
        onDispose { router.removeCallback(callback) }
    }
    return routes
}

/**
 * De cast-knop in de kop van de speler, alleen als er een speaker of tv in het
 * netwerk is of er al naartoe gespeeld wordt. Tikken opent Media3's kiezer; op
 * Android 14 en later is dat de uitvoerkiezer van het systeem.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun CastButton(routes: CastRoutes, modifier: Modifier = Modifier) {
    if (!routes.available && routes.playingOn == null) return
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(GlassColor)
            .semantics {
                contentDescription = routes.playingOn?.let { "Speelt op $it. Tik om te wisselen of te stoppen." }
                    ?: "Afspelen op een speaker of tv"
            },
        contentAlignment = Alignment.Center
    ) {
        if (routes.preview || LocalInspectionMode.current) {
            // Voorvertoningen en schermafdrukken hebben geen netwerk om in te zoeken.
            Icon(WoolIcons.Cast, null, tint = NightInk, modifier = Modifier.size(21.dp))
        } else {
            CompositionLocalProvider(LocalContentColor provides NightInk) {
                MediaRouteButton(Modifier.size(44.dp))
            }
        }
    }
}

/** "Speelt op Woonkamer", onder de titel, zolang een speaker of tv speelt. Zelfde vorm als het hitlijstlabel. */
@Composable
fun CastingLabel(routes: CastRoutes, modifier: Modifier = Modifier) {
    val name = routes.playingOn ?: return
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, outlineOnGlow(), RoundedCornerShape(20.dp))
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(WoolIcons.Speaker, null, modifier = Modifier.size(16.dp))
        Text(
            "Speelt op $name",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
