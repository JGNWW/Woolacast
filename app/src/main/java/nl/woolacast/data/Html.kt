package nl.woolacast.data

import androidx.core.text.HtmlCompat

/**
 * Omschrijvingen in RSS-feeds en in de Spotify-lijst zijn HTML: alinea's,
 * links, en entiteiten als &amp;amp;. Ongefilterd zie je die rommel terug in de
 * app, dus alles gaat hier doorheen voordat het het scherm haalt.
 */
object Html {

    /** Staat er echte opmaak in, of is het platte tekst (die ook een & of < kan bevatten)? */
    fun looksLikeHtml(raw: String): Boolean = HTML_TAG.containsMatchIn(raw)

    private val HTML_TAG = Regex("</?(p|br|a|div|span|ul|ol|li|b|i|strong|em|h[1-6]|blockquote)\\b[^>]*>", RegexOption.IGNORE_CASE)

    fun toPlainText(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        // Platte tekst met witregels zou HTML-weergave tot één lap aan elkaar plakken;
        // daar maken we de regeleinden eerst echte regeleinden van.
        val value = if (looksLikeHtml(trimmed)) trimmed else trimmed.replace("\r", "").replace("\n", "<br>")
        val text = HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(' ', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return text.takeIf { it.isNotEmpty() }
    }
}
