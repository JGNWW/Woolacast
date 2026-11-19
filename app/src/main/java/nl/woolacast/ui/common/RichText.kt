package nl.woolacast.ui.common

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

/*
 * Omschrijvingen zoals de maker ze opmaakte: alinea's, vet en cursief, en links
 * die je kunt aantikken (ze openen in de browser of de app die erbij hoort).
 * Kale webadressen in de tekst worden ook links, want veel feeds zetten ze er
 * gewoon tussen haakjes in.
 */

/** De omschrijving van een aflevering, opgemaakt: uit [html] als die er is, anders uit [plain]. */
@Composable
fun rememberRichDescription(html: String?, plain: String?, linkColor: Color): AnnotatedString? =
    remember(html, plain, linkColor) { richDescription(html, plain, linkColor) }

fun richDescription(html: String?, plain: String?, linkColor: Color): AnnotatedString? {
    val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    val fromHtml = html?.let { runCatching { fromHtml(it, styles) }.getOrNull() }
    val text = fromHtml ?: plain?.trim()?.takeIf { it.isNotEmpty() }?.let { AnnotatedString(it) } ?: return null
    return if (text.text.isBlank()) null else withBareLinks(text, styles)
}

private fun fromHtml(html: String, styles: TextLinkStyles): AnnotatedString {
    val cleaned = html
        .replace(Regex("(?is)<(script|style)\\b.*?</\\1>"), "")
        .replace("\r", "")
    val spanned = SpannableStringBuilder(HtmlCompat.fromHtml(cleaned, HtmlCompat.FROM_HTML_MODE_LEGACY))
    tidy(spanned)
    return buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            when (span) {
                is URLSpan -> span.url?.takeIf { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("mailto:") }
                    ?.let { addLink(LinkAnnotation.Url(it, styles), start, end) }
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic), start, end)
                }
                is UnderlineSpan -> Unit // onderstrepen is voor links; losse onderstreping laten we weg
            }
        }
    }
}

/**
 * Witruimte zoals in een gewone tekst: geen spaties aan het begin van een regel,
 * hooguit één witregel tussen alinea's, en niets aan begin en eind. Gaat via
 * de SpannableStringBuilder, zodat links op hun plek blijven.
 */
private fun tidy(text: SpannableStringBuilder) {
    fun replaceAll(regex: Regex, with: String) {
        while (true) {
            val match = regex.find(text) ?: return
            text.replace(match.range.first, match.range.last + 1, with)
        }
    }
    text.toString().indices.reversed().forEach { i -> if (text[i] == ' ') text.replace(i, i + 1, " ") }
    replaceAll(Regex("[ \\t]+\\n"), "\n")
    replaceAll(Regex("\\n[ \\t]+"), "\n")
    replaceAll(Regex("[ \\t]{2,}"), " ")
    replaceAll(Regex("\\n{3,}"), "\n\n")
    while (text.isNotEmpty() && text[0].isWhitespace()) text.delete(0, 1)
    while (text.isNotEmpty() && text[text.length - 1].isWhitespace()) text.delete(text.length - 1, text.length)
}

/** Een webadres in de tekst; afsluitende leestekens en haakjes horen er niet bij. */
private val BARE_URL = Regex("https?://[^\\s<>\"]+")

private fun withBareLinks(text: AnnotatedString, styles: TextLinkStyles): AnnotatedString {
    val linked = text.getLinkAnnotations(0, text.length)
    val extra = BARE_URL.findAll(text.text).mapNotNull { match ->
        var end = match.range.last + 1
        while (end > match.range.first && text.text[end - 1] in ".,;:!?)]'") {
            // Een sluithaakje hoort bij het adres als er in het adres ook een openingshaakje staat.
            if (text.text[end - 1] == ')' && text.text.substring(match.range.first, end - 1).contains('(')) break
            end--
        }
        val start = match.range.first
        if (end <= start || linked.any { it.start < end && start < it.end }) null else start to end
    }.toList()
    if (extra.isEmpty()) return text
    return buildAnnotatedString {
        append(text)
        extra.forEach { (start, end) -> addLink(LinkAnnotation.Url(text.text.substring(start, end), styles), start, end) }
    }
}
