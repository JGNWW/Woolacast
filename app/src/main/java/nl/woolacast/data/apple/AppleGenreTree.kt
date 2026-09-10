package nl.woolacast.data.apple

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
data class GenreNode(
    val name: String = "",
    val subgenres: Map<String, GenreNode>? = null
)

/**
 * Apple hangt elke aflevering aan één genre, maar soms is dat een subgenre
 * ("Nieuwscommentaar") in plaats van een hoofdgenre ("Nieuws"). De genreboom
 * van de winkel vertelt welke bij welke hoort, in de taal van die winkel.
 *
 * Eén aanroep per land, daarna uit het geheugen.
 */
class AppleGenreTree(private val catalog: AppleCatalogApi) {

    private val mutex = Mutex()
    private val byStorefront = mutableMapOf<String, Map<String, Int>>()

    /** Genrenaam (hoofd- of subgenre) naar het id van het hoofdgenre. */
    suspend fun topLevelByName(storefront: String): Map<String, Int> {
        byStorefront[storefront]?.let { return it }

        val resolved = mutex.withLock {
            byStorefront[storefront] ?: runCatching {
                val root = catalog.genres(cc = storefront)[PODCASTS_GENRE_ID]
                buildMap {
                    root?.subgenres?.forEach { (id, top) ->
                        val topId = id.toIntOrNull() ?: return@forEach
                        put(top.name, topId)
                        top.subgenres?.values?.forEach { sub -> put(sub.name, topId) }
                    }
                }
            }.getOrElse { emptyMap() }.also { byStorefront[storefront] = it }
        }
        return resolved
    }

    private companion object {
        const val PODCASTS_GENRE_ID = "26"
    }
}
