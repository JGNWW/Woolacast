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
    // Open cirkel (r 8.5 om 12,13) met een chevron op het uiteinde; het getal komt er als tekst in.
    val Back15 = stroke("back-15", 1.8f, StrokeCap.Round, "M7.75 5.64A8.5 8.5 0 1 0 16.25 5.64", "M10.6 2.7L7.6 5.7l3 3")
    val Forward30 = stroke("forward-30", 1.8f, StrokeCap.Round, "M16.25 5.64A8.5 8.5 0 1 1 7.75 5.64", "M13.4 2.7l3 3-3 3")
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
    val Trend = stroke("trend", 2.2f, StrokeCap.Round, "M4 17l6-6 4 4 6-7", "M15 8h5v5")
    val News = stroke("news", 1.9f, StrokeCap.Round, "M4 5h13v14H6a2 2 0 0 1 -2-2z", "M17 8h3v9a2 2 0 0 1 -2 2", "M7 9h6", "M7 13h7")
    val Spark = filled("spark", 24f, "M12 3l2.2 5.8L20 11l-5.8 2.2L12 19l-2.2-5.8L4 11l5.8-2.2z")

    /* ---- categorieen ----
       Een tekening per categorie, in dezelfde lijn als de rest: 24 px, lijn 1,8,
       ronde uiteinden. De sleutel is het genre-id van Apple, zodat het niet aan
       de vertaling van een naam hangt. */
    private val COMEDY = stroke("genre-comedy", 1.8f, StrokeCap.Round,
        circle(12f, 12f, 8.5f), "M8 13.8a4.6 4.6 0 0 0 8 0", "M9.2 10v0.01", "M14.8 10v0.01")
    private val TRUE_CRIME = stroke("genre-true-crime", 1.8f, StrokeCap.Round,
        circle(10.5f, 10.5f, 6.5f), "M20.5 20.5l-5.3-5.3", "M8.3 11.6a2.3 2.3 0 0 1 4.6 0")
    private val SOCIETY = stroke("genre-society", 1.8f, StrokeCap.Round,
        circle(9.2f, 8.4f, 3.2f), "M3.6 19.6c0-3.2 2.5-5.2 5.6-5.2s5.6 2 5.6 5.2",
        circle(17f, 10f, 2.3f), "M16 14.8c2.8-0.4 4.6 1.6 4.6 4.8")
    private val SPORT = stroke("genre-sport", 1.7f, StrokeCap.Round,
        circle(12f, 12f, 8.5f), "M12 8.4l3.2 2.3-1.2 3.8h-4l-1.2-3.8z",
        "M12 8.4V3.5", "M15.2 10.7l4.6-1.5", "M14 14.5l2.9 3.9", "M10 14.5l-2.9 3.9",
        "M8.8 10.7L4.2 9.2")
    private val BUSINESS = stroke("genre-business", 1.8f, StrokeCap.Round,
        "M3.5 8.5h17v9.5a2 2 0 0 1 -2 2h-13a2 2 0 0 1 -2-2z",
        "M9 8.5V6.8A1.8 1.8 0 0 1 10.8 5h2.4A1.8 1.8 0 0 1 15 6.8v1.7", "M3.5 13.2h17")
    private val SCIENCE = stroke("genre-science", 1.8f, StrokeCap.Round,
        "M10 3.5h4",
        "M10.6 3.5v5.8l-4.9 8.5a2 2 0 0 0 1.7 3h9.2a2 2 0 0 0 1.7-3l-4.9-8.5V3.5",
        "M8.2 14.6h7.6")
    private val HISTORY = stroke("genre-history", 1.8f, StrokeCap.Round,
        "M7 3.5h10", "M7 20.5h10", "M8.2 3.5v3.3l3.8 3.7 3.8-3.7V3.5",
        "M8.2 20.5v-3.3l3.8-3.7 3.8 3.7v3.3")
    private val HEALTH = stroke("genre-health", 2f, StrokeCap.Round,
        "M3.5 12.5h3.6l2-4.6 3.2 9.2 2.3-5.4 1.4 2.3h4.5")
    private val ARTS = stroke("genre-arts", 1.8f, StrokeCap.Round,
        "M12 3.2a8.8 8.8 0 0 0 0 17.6 1.9 1.9 0 0 0 1.5-3 1.9 1.9 0 0 1 1.5-3h1.8a4 4 0 0 0 4-4c0-4-3.9-7.6-8.8-7.6z",
        "M8.2 9.6v0.01", "M12 7.6v0.01", "M7.6 13.6v0.01")
    private val EDUCATION = stroke("genre-education", 1.8f, StrokeCap.Round,
        "M2.5 9.3L12 5l9.5 4.3L12 13.6z",
        "M6.8 11.4v4.4c0 1.5 2.3 2.7 5.2 2.7s5.2-1.2 5.2-2.7v-4.4", "M20.6 10.1v4.6")
    private val TECH = stroke("genre-tech", 1.7f, StrokeCap.Round,
        "M6.8 6.8h10.4v10.4h-10.4z", "M10.4 10.4h3.2v3.2h-3.2z",
        "M9.8 6.8V4.2", "M14.2 6.8V4.2", "M9.8 19.8v-2.6", "M14.2 19.8v-2.6",
        "M6.8 9.8H4.2", "M6.8 14.2H4.2", "M19.8 9.8h-2.6", "M19.8 14.2h-2.6")
    private val MUSIC = stroke("genre-music", 1.8f, StrokeCap.Round,
        "M9.5 17.4V6l9-2v11.4", circle(7f, 17.4f, 2.5f), circle(16f, 15.4f, 2.5f))
    private val FICTION = stroke("genre-fiction", 1.8f, StrokeCap.Round,
        "M12 7S9.8 5.2 6.4 5.2H3.4v12.4h3.2c3.2 0 5.4 1.9 5.4 1.9s2.2-1.9 5.4-1.9h3.2V5.2h-3c-3.4 0-5.6 1.8-5.6 1.8z",
        "M12 7v12.5")
    private val LEISURE = stroke("genre-leisure", 1.8f, StrokeCap.Round,
        "M4.8 19.2s-1.8-7.6 4.4-12c3.4-2.6 7.4-2.6 10.6-2.8 0.2 3-0.2 7.2-2.6 10.4-4.4 5-12.4 4.4-12.4 4.4z",
        "M4.8 19.2c2.8-3.8 5.8-6 9.6-7.8")
    private val KIDS = stroke("genre-kids", 1.8f, StrokeCap.Round,
        "M12 3.2a4.9 4.9 0 0 1 4.9 4.9c0 3.3-3 6.4-4.9 6.4s-4.9-3.1-4.9-6.4A4.9 4.9 0 0 1 12 3.2z",
        "M12 14.5v2.3", "M10.4 20.6c0-1.6 3.2-1.6 3.2-3.4")
    private val RELIGION = stroke("genre-religion", 1.8f, StrokeCap.Round,
        "M12 3.2s5.2 4.6 5.2 9.2a5.2 5.2 0 1 1 -10.4 0c0-4.6 5.2-9.2 5.2-9.2z")
    private val TV_FILM = stroke("genre-tv-film", 1.8f, StrokeCap.Round,
        "M4 6.2h16v11.2h-16z", "M8.6 3.2L12 6.2l3.4-3", "M9 20.8h6")
    private val GOVERNMENT = stroke("genre-government", 1.8f, StrokeCap.Round,
        "M3.4 9.6L12 4.6l8.6 5", "M6 9.6v8.8", "M10 9.6v8.8", "M14 9.6v8.8", "M18 9.6v8.8",
        "M3.4 20.4h17.2")

    /** Het tekeningetje bij een categorie; null als we het genre niet kennen. */
    fun genre(genreId: Int?): ImageVector? = when (genreId) {
        1303 -> COMEDY
        1489 -> News
        1488 -> TRUE_CRIME
        1324 -> SOCIETY
        1545 -> SPORT
        1321 -> BUSINESS
        1533 -> SCIENCE
        1487 -> HISTORY
        1512 -> HEALTH
        1301 -> ARTS
        1304 -> EDUCATION
        1318 -> TECH
        1310 -> MUSIC
        1483 -> FICTION
        1502 -> LEISURE
        1305 -> KIDS
        1314 -> RELIGION
        1309 -> TV_FILM
        1511 -> GOVERNMENT
        else -> null
    }
}
