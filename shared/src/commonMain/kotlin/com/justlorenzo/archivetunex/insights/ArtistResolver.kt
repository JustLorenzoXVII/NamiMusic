package com.justlorenzo.archivetunex.insights

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.ArtistItem

/** L'artista su YouTube Music: la sua pagina e la sua foto. */
@Serializable
data class ResolvedArtist(
    val browseId: String? = null,
    val thumbnailUrl: String? = null,
    val checkedAt: Long = 0,
)

/**
 * Nella cronologia gli artisti sono spesso salvati senza foto e con un codice interno ("LA…"),
 * che YouTube non conosce. Qui si cerca l'artista vero su YouTube Music (una volta sola, poi resta salvato).
 */
object ArtistResolver {
    private const val RETRY_AFTER_MS = 7L * 24 * 60 * 60 * 1000

    private val store =
        JsonFile("artists.json", MapSerializer(String.serializer(), ResolvedArtist.serializer())) { emptyMap() }
    private val mutex = Mutex()

    val resolved = store.value

    /** Un id che si può aprire su YouTube Music (i canali iniziano con "UC"). */
    fun isYouTubeId(id: String?): Boolean = id != null && id.startsWith("UC")

    fun get(name: String): ResolvedArtist? = store.value.value[name.lowercase()]

    /** Cerca foto e pagina degli artisti che ancora non le hanno. */
    suspend fun resolve(artists: List<Pair<String, String?>>) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { resolveNow(artists) }

    private suspend fun resolveNow(artists: List<Pair<String, String?>>) =
        mutex.withLock {
            val now = nowMs()
            for ((name, id) in artists) {
                val known = get(name)
                if (known != null && (known.browseId != null || now - known.checkedAt < RETRY_AFTER_MS)) continue
                val found =
                    kotlinx.coroutines.withTimeoutOrNull(15_000) {
                    runCatching {
                        if (isYouTubeId(id)) {
                            val page = YouTube.artist(id!!).getOrNull()
                            ResolvedArtist(id, page?.artist?.thumbnail, now)
                        } else {
                            val match =
                                YouTube
                                    .search(name, YouTube.SearchFilter.FILTER_ARTIST)
                                    .getOrNull()
                                    ?.items
                                    ?.filterIsInstance<ArtistItem>()
                                    ?.let { list -> list.firstOrNull { it.title.equals(name, ignoreCase = true) } ?: list.firstOrNull() }
                            ResolvedArtist(match?.id, match?.thumbnail, now)
                        }
                    }.getOrNull()
                    } ?: ResolvedArtist(null, null, now)
                store.update { it + (name.lowercase() to found) }
            }
        }
}
