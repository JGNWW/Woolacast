package nl.woolacast.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * De lijniconen uit de mockup (24 px, lijn 1,8), één op één overgenomen als
 * paden. Material's iconenset komt in de buurt, maar niet dicht genoeg om de
 * schermen hetzelfde te laten aanvoelen. Kleur komt van [androidx.compose.material3.Icon]'s tint.
 */
object WoolIcons {

    private fun stroke(
        name: String,
        width: Float = 1.8f,
        cap: StrokeCap = StrokeCap.Round,
        vararg paths: String
    ): ImageVector = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = cap,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

    private fun filled(name: String, viewport: Float = 24f, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = viewport, viewportHeight = viewport
        ).apply {
            paths.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
        }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

    /* ---- navigatie ---- */
    val Charts = stroke("charts", 2.2f, StrokeCap.Round, "M5 20v-7", "M12 20V4", "M19 20v-11")
    val Discover = stroke("discover", 1.9f, StrokeCap.Round, circle(12f, 12f, 8.5f), "M15 9l-2 4.2L9 15l2-4.2z")
    val Library = stroke("library", 1.9f, StrokeCap.Round, "M4 5v14", "M9.5 5v14", "M14.5 6.2l5 12.8")

    /* ---- app bar ---- */
    val Search = stroke("search", 1.8f, StrokeCap.Round, circle(11f, 11f, 7f), "M20.5 20.5L16.6 16.6")
    val Bell = stroke("bell", 1.8f, StrokeCap.Round, "M18 9a6 6 0 1 0 -12 0c0 5-2 6-2 6h16s-2-1-2-6", "M13.7 20a2 2 0 0 1 -3.4 0")
    val Filter = stroke("filter", 1.9f, StrokeCap.Round, "M4 7h16", "M7 12h10", "M10 17h4")
    val Back = stroke("back", 2f, StrokeCap.Round, "M15 5l-7 7 7 7")
    val ChevronDown = stroke("chevron-down", 2f, StrokeCap.Round, "M6 9l6 6 6-6")
    val ChevronRight = stroke("chevron-right", 2.2f, StrokeCap.Round, "M9 6l6 6-6 6")
    val Close = stroke("close", 2f, StrokeCap.Round, "M6 6l12 12", "M18 6L6 18")
    val Plus = stroke("plus", 2.2f, StrokeCap.Round, "M12 5v14", "M5 12h14")
    val Check = stroke("check", 2.2f, StrokeCap.Round, "M5 12.5l4.5 4.5L19 7.5")
    val More = filled("more", 24f, circle(12f, 5.5f, 1.8f), circle(12f, 12f, 1.8f), circle(12f, 18.5f, 1.8f))
    val Refresh = stroke("refresh", 1.9f, StrokeCap.Round, "M20 12a8 8 0 1 1 -2.4 -5.7", "M20 4v4.5h-4.5")

    /* ---- speler ---- */
    val Play = filled("play", 24f, "M8 5.2v13.6L19 12z")
    val Pause = filled("pause", 24f, "M7 5h3.6v14H7z", "M13.4 5h3.6v14h-3.6z")
    val Previous = filled("previous", 24f, "M7 6h2.4v12H7z", "M19 6v12l-9-6z")
    val Next = filled("next", 24f, "M14.6 6H17v12h-2.4z", "M5 6v12l9-6z")
    val Back15 = stroke("back-15", 1.7f, StrokeCap.Round, "M11.5 5.5L7 9l4.5 3.5", "M7 9h6.5a5.5 5.5 0 1 1 0 11H8")
    val Forward30 = stroke("forward-30", 1.7f, StrokeCap.Round, "M12.5 5.5L17 9l-4.5 3.5", "M17 9h-6.5a5.5 5.5 0 1 0 0 11H16")
    val SkipForward = stroke("skip-forward", 1.9f, StrokeCap.Round, "M5 12h14", "M14 7l5 5-5 5")
    val Speed = stroke("speed", 1.8f, StrokeCap.Round, "M4 17a8 8 0 1 1 16 0", "M12 13l4-3")
    val Timer = stroke("timer", 1.8f, StrokeCap.Round, "M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z")
    val Queue = stroke("queue", 1.8f, StrokeCap.Round, "M4 7h11", "M4 12h11", "M4 17h7", "M17 12v8", "M20 15l-3-3-3 3")
    val QueueAdded = stroke("queue-added", 1.8f, StrokeCap.Round, "M4 7h11", "M4 12h11", "M4 17h7", "M14.5 17l2.5 2.5 4.5-4.5")
    val Share = stroke("share", 1.8f, StrokeCap.Round, "M12 15V4", "M8 8l4-4 4 4", "M5 14v4.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V14")
    val Save = stroke("save", 1.8f, StrokeCap.Round, "M12 4v11", "M8 11l4 4 4-4", "M5 19h14")
    val Saved = stroke("saved", 1.8f, StrokeCap.Round, "M12 4v9", "M8 9l4 4 4-4", "M5 19h14", "M5 15h14")

    /* ---- lijsten ---- */
    val Clock = stroke("clock", 2f, StrokeCap.Round, circle(12f, 12f, 9f), "M12 7.5V12l3 2")
    val Trophy = stroke("trophy", 1.8f, StrokeCap.Round, "M8 4h8v5a4 4 0 0 1 -8 0z", "M8 5H5v1.5A3.5 3.5 0 0 0 8.5 10", "M16 5h3v1.5A3.5 3.5 0 0 1 15.5 10", "M10 20h4", "M12 13v7")
    val Info = stroke("info", 1.7f, StrokeCap.Round, circle(12f, 12f, 9f), "M12 8v5", "M12 16.2v0.1")
    val Bars = stroke("bars", 2.4f, StrokeCap.Round, "M5 20v-5", "M12 20V5", "M19 20v-8")
    val Up = filled("up", 12f, "M6 2l4.5 7h-9z")
    val Down = filled("down", 12f, "M6 10L1.5 3h9z")
    val Flat = filled("flat", 12f, "M1.5 5h9v2h-9z")
    val Remove = stroke("remove", 1.9f, StrokeCap.Round, "M5 12h14")
}
