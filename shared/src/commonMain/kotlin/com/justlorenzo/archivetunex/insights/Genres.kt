package com.justlorenzo.archivetunex.insights

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient

/** Un genere ampio, quello che si mostra nelle statistiche. */
enum class Genre(
    val label: String,
    private val keywords: List<String>,
) {
    Pop("Pop", listOf("pop", "dance-pop", "synthpop", "electropop")),
    Rock("Rock", listOf("rock", "grunge", "punk", "post-punk", "emo")),
    Metal("Metal", listOf("metal", "metalcore", "nu metal", "hardcore")),
    HipHop("Hip hop / Rap", listOf("hip hop", "hip-hop", "rap", "trap", "drill", "grime")),
    Electronic("Elettronica", listOf("electronic", "house", "techno", "edm", "dubstep", "trance", "drum and bass", "electro", "dance")),
    RnB("R&B / Soul", listOf("r&b", "rnb", "soul", "funk", "neo soul")),
    Indie("Indie / Alternative", listOf("indie", "alternative", "shoegaze", "dream pop")),
    Italian("Musica italiana", listOf("italian", "cantautor", "canzone")),
    Latin("Latin / Reggaeton", listOf("latin", "reggaeton", "salsa", "bachata", "urbano")),
    Jazz("Jazz / Blues", listOf("jazz", "blues", "swing", "bossa")),
    Classical("Classica", listOf("classical", "orchestral", "opera", "baroque", "soundtrack", "score")),
    Country("Country / Folk", listOf("country", "folk", "americana", "bluegrass", "acoustic")),
    Reggae("Reggae", listOf("reggae", "dancehall", "ska", "dub")),
    Kpop("K-pop / J-pop", listOf("k-pop", "kpop", "j-pop", "jpop", "anime", "city pop"));

    companion object {
        /** Da un genere preciso (es. "alternative metal") a quelli ampi a cui appartiene. */
        fun fromTag(tag: String): List<Genre> {
            val t = tag.lowercase()
            // "pop" dentro "k-pop" o "synthpop" non deve contare come Pop generico se c'è un genere più preciso
            val matches = entries.filter { genre -> genre.keywords.any { t.contains(it) } }
            return if (matches.size > 1 && Pop in matches) matches - Pop else matches
        }
    }
}

@Serializable
data class ArtistGenres(
    val tags: List<String> = emptyList(),
    val checkedAt: Long = 0,
)

/**
 * Generi degli artisti da MusicBrainz (gratis, senza chiave). Ogni artista si chiede una volta
 * sola e il risultato resta salvato. MusicBrainz chiede al massimo una richiesta al secondo.
 */
object GenreService {
    private val cache =
        JsonFile("genres.json", MapSerializer(String.serializer(), ArtistGenres.serializer())) { emptyMap() }
    private val client by lazy { createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {} }
    private val mutex = Mutex()

    private const val RETRY_AFTER_MS = 30L * 24 * 60 * 60 * 1000

    val genres = cache.value

    /** Generi già noti di un artista (senza rete). */
    fun cached(artist: String): List<String>? = cache.value.value[artist.lowercase()]?.tags

    /** Cerca i generi degli artisti che non si conoscono ancora (al massimo [limit] alla volta). */
    suspend fun fetchMissing(
        artists: List<String>,
        limit: Int = 20,
    ) = mutex.withLock {
        val now = nowMs()
        val missing =
            artists
                .distinctBy { it.lowercase() }
                .filter { name ->
                    val known = cache.value.value[name.lowercase()]
                    known == null || (known.tags.isEmpty() && now - known.checkedAt > RETRY_AFTER_MS)
                }.take(limit)
        for (name in missing) {
            val tags = runCatching { lookup(name) }.getOrNull() ?: continue
            cache.update { it + (name.lowercase() to ArtistGenres(tags, nowMs())) }
        }
    }

    private suspend fun lookup(name: String): List<String> {
        // 1) cerca l'artista
        delay(1_100)
        val search =
            client.get("https://musicbrainz.org/ws/2/artist/") {
                parameter("query", "artist:\"${name.replace("\"", "")}\"")
                parameter("fmt", "json")
                parameter("limit", 3)
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "application/json")
            }
        if (!search.status.isSuccess()) error("MusicBrainz ${search.status}")
        val artists = insightsJson.parseToJsonElement(search.bodyAsText()).jsonObject["artists"]?.jsonArray ?: return emptyList()
        val best =
            artists
                .map { it.jsonObject }
                .firstOrNull { (it["score"]?.jsonPrimitive?.intOrNull ?: 0) >= 90 }
                ?: return emptyList()
        val id = best["id"]?.jsonPrimitive?.content ?: return emptyList()

        // 2) i suoi generi (più puliti dei tag liberi)
        delay(1_100)
        val detail =
            client.get("https://musicbrainz.org/ws/2/artist/$id") {
                parameter("inc", "genres+tags")
                parameter("fmt", "json")
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "application/json")
            }
        if (!detail.status.isSuccess()) error("MusicBrainz ${detail.status}")
        val body = insightsJson.parseToJsonElement(detail.bodyAsText()).jsonObject
        val genres = body.counted("genres")
        val tags = if (genres.isNotEmpty()) genres else body.counted("tags")
        return tags.take(6)
    }

    private fun JsonObject.counted(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.map { it.jsonObject }
            ?.sortedByDescending { it["count"]?.jsonPrimitive?.intOrNull ?: 0 }
            ?.mapNotNull { it["name"]?.jsonPrimitive?.content }
            .orEmpty()

    private const val USER_AGENT = "Nami/1.0 (personal music player; https://github.com/JustLorenzoXVII)"
}
