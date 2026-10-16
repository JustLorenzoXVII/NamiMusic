/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.repository

import moe.rukamori.archivetune.innertube.network.createPlatformHttpClient

import kotlin.concurrent.Volatile

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import moe.rukamori.archivetune.models.NewsItem

class NewsRepository constructor() {
        private val client =
            createPlatformHttpClient(proxy = null, dnsOverHttpsUrl = null) {
            }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        @Volatile private var metadataCache: List<NewsItem>? = null

        suspend fun fetchNews(): List<NewsItem> {
            val response =
                client.get(METADATA_URL) {
                    headers {
                        append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                        append(HttpHeaders.Pragma, "no-cache")
                        append(HttpHeaders.Expires, "0")
                    }
                }
            val text = response.bodyAsText()
            val items = json.decodeFromString<List<NewsItem>>(text)
            metadataCache = items
            return items
        }

        suspend fun fetchNewsContent(id: String): String {
            val response =
                client.get("$CONTENT_BASE_URL$id") {
                    headers {
                        append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                        append(HttpHeaders.Pragma, "no-cache")
                        append(HttpHeaders.Expires, "0")
                    }
                }
            return response.bodyAsText()
        }

        fun getCachedItem(id: String): NewsItem? = metadataCache?.find { it.id == id }

        private companion object {
            const val METADATA_URL =
                "https://raw.githubusercontent.com/koiverse/ArchiveTuneNewsRepository/main/metadata.json"
            const val CONTENT_BASE_URL =
                "https://raw.githubusercontent.com/koiverse/ArchiveTuneNewsRepository/main/content/"
        }
    }
