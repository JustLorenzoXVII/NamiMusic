package moe.rukamori.archivetune.compat.media3

/**
 * Cache dei brani (androidx.media3.datasource.cache.Cache): in ArchiveTuneX la cache dei
 * download sono i file scaricati, mentre la riproduzione in streaming non salva nulla su disco.
 */
interface Cache {
    val keys: Set<String>

    val cacheSpace: Long

    fun isCached(key: String, position: Long, length: Long): Boolean

    fun getCachedBytes(key: String, position: Long, length: Long): Long

    fun removeResource(key: String)

    fun release() = Unit

    /** Come Cache.getCachedSpans di Media3: qui un unico blocco con i byte salvati. */
    fun getCachedSpans(key: String): List<CacheSpan> {
        val bytes = getCachedBytes(key, 0L, Long.MAX_VALUE)
        return if (bytes > 0L) listOf(CacheSpan(key, 0L, bytes)) else emptyList()
    }
}

/** androidx.media3.datasource.cache.CacheSpan. */
data class CacheSpan(
    val key: String,
    val position: Long,
    val length: Long,
)

/** Cache vuota (lettore in streaming). */
object EmptyCache : Cache {
    override val keys: Set<String> = emptySet()
    override val cacheSpace: Long = 0L

    override fun isCached(key: String, position: Long, length: Long): Boolean = false

    override fun getCachedBytes(key: String, position: Long, length: Long): Long = 0L

    override fun removeResource(key: String) = Unit
}
