package nl.woolacast.data.download

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Haalt een audiobestand binnen naar een .part-bestand, en gaat verder waar
 * een eerdere poging bleef.
 *
 * Verdergaan mag alleen als het bestand op de server nog hetzelfde is. Hosts
 * als Acast en Megaphone voegen per verzoek andere reclame in; twee helften van
 * verschillende versies aan elkaar plakken geeft stil kapotte audio. Daarom
 * onthoudt een klein .meta-bestand de ETag of Last-Modified en de totale
 * lengte, en vraagt het vervolg met If-Range: klopt de versie niet meer, dan
 * stuurt de server gewoon alles opnieuw.
 */
class AudioFetcher(private val client: OkHttpClient) {

    /**
     * Geeft het aantal bytes terug als het bestand compleet is, of null als
     * [isStopped] halverwege waar werd. [onProgress] komt hooguit eens per seconde.
     */
    fun fetch(
        url: String,
        part: File,
        isStopped: () -> Boolean = { false },
        now: () -> Long = System::currentTimeMillis,
        onProgress: (done: Long, total: Long) -> Unit = { _, _ -> }
    ): Long? {
        part.parentFile?.mkdirs()
        val meta = File(part.path + META)
        val saved = Meta.read(meta)
        val already = if (part.exists()) part.length() else 0L
        // Zonder bewaarde versie weten we niet of het halve bestand nog klopt.
        val resume = already > 0L && saved?.validator != null
        if (!resume) { part.delete(); meta.delete() }

        val request = Request.Builder().url(url).apply {
            if (resume) {
                header("Range", "bytes=$already-")
                header("If-Range", saved!!.validator!!)
            }
        }.build()

        client.newCall(request).execute().use { response ->
            if (response.code == 416 && resume) {
                // Het deel was al compleet; de app stopte voor het hernoemen.
                if (saved!!.total > 0L && already == saved.total) { meta.delete(); return already }
                part.delete(); meta.delete()
                throw IOException("HTTP 416")
            }
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Leeg antwoord.")

            // Een 206 die niet aansluit (de server negeerde If-Range maar deed wel
            // de Range): dat stuk is geen heel bestand. Opnieuw, en dan schoon.
            if (response.code == 206 && !(resume && continues(response, already, saved!!.total))) {
                part.delete(); meta.delete()
                throw IOException("Het vervolg sloot niet aan.")
            }
            val append = response.code == 206
            val start = if (append) already else 0L
            val total = body.contentLength().takeIf { it > 0L }?.plus(start) ?: 0L
            if (!append) Meta(validator(response), total).write(meta)

            var done = start
            var lastReport = 0L
            FileOutputStream(part, append).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isStopped()) return null
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        done += read
                        val at = now()
                        if (at - lastReport >= REPORT_EVERY_MS) {
                            lastReport = at
                            onProgress(done, total)
                        }
                    }
                }
            }
            if (total > 0L && done < total) throw IOException("De verbinding viel weg.")
            if (done < MIN_BYTES) throw IOException("Het bestand is te klein om audio te zijn.")
            meta.delete()
            return done
        }
    }

    /** Sluit dit 206-antwoord echt aan op wat we hebben? Content-Range: bytes 100-999/1000. */
    private fun continues(response: Response, already: Long, total: Long): Boolean {
        val range = response.header("Content-Range") ?: return false
        val match = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""").find(range) ?: return false
        val from = match.groupValues[1].toLongOrNull() ?: return false
        val size = match.groupValues[3].toLongOrNull()
        return from == already && (total <= 0L || size == null || size == total)
    }

    /** If-Range kent alleen sterke ETags; een zwakke (W/…) telt niet. */
    private fun validator(response: Response): String? =
        response.header("ETag")?.takeUnless { it.startsWith("W/") }
            ?: response.header("Last-Modified")

    private data class Meta(val validator: String?, val total: Long) {
        fun write(file: File) = runCatching { file.writeText("${validator.orEmpty()}\n$total") }

        companion object {
            fun read(file: File): Meta? = runCatching {
                val lines = file.readLines()
                Meta(lines.getOrNull(0)?.takeIf { it.isNotBlank() }, lines.getOrNull(1)?.toLongOrNull() ?: 0L)
            }.getOrNull()
        }
    }

    companion object {
        const val META = ".meta"
        private const val REPORT_EVERY_MS = 1_000L
        /** Minder dan dit is een foutpagina, geen aflevering. */
        const val MIN_BYTES = 16 * 1024L
    }
}
