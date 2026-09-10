package nl.woolacast.data

import androidx.core.text.HtmlCompat

/**
 * Omschrijvingen in RSS-feeds en in de Spotify-lijst zijn HTML: alinea's,
 * links, en entiteiten als &amp;amp;. Ongefilterd zie je die rommel terug in de
 * app, dus alles gaat hier doorheen voordat het het scherm haalt.
 */
object Html {

    fun toPlainText(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val text = HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(' ', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return text.takeIf { it.isNotEmpty() }
    }
}
