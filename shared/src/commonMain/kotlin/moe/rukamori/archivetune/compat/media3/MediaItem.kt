package moe.rukamori.archivetune.compat.media3

import moe.rukamori.archivetune.compat.Uri

/** Sostituto di androidx.media3.common.MediaItem: un elemento della coda. */
class MediaItem private constructor(
    val mediaId: String,
    val localConfiguration: LocalConfiguration?,
    val mediaMetadata: MediaMetadata,
) {
    class LocalConfiguration internal constructor(
        val uri: Uri,
        val mimeType: String?,
        val customCacheKey: String?,
        val tag: Any?,
    )

    fun buildUpon(): Builder =
        Builder().apply {
            setMediaId(mediaId)
            localConfiguration?.let {
                setUri(it.uri)
                setMimeType(it.mimeType)
                setCustomCacheKey(it.customCacheKey)
                setTag(it.tag)
            }
            setMediaMetadata(mediaMetadata)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaItem) return false
        return mediaId == other.mediaId && localConfiguration?.uri == other.localConfiguration?.uri
    }

    override fun hashCode(): Int = mediaId.hashCode() * 31 + (localConfiguration?.uri?.hashCode() ?: 0)

    override fun toString(): String = "MediaItem($mediaId)"

    class Builder {
        private var mediaId: String? = null
        private var uri: Uri? = null
        private var mimeType: String? = null
        private var customCacheKey: String? = null
        private var tag: Any? = null
        private var mediaMetadata: MediaMetadata = MediaMetadata.EMPTY

        fun setMediaId(value: String) = apply { mediaId = value }
        fun setUri(value: String?) = apply { uri = value?.let { Uri.parse(it) } }
        fun setUri(value: Uri?) = apply { uri = value }
        fun setMimeType(value: String?) = apply { mimeType = value }
        fun setCustomCacheKey(value: String?) = apply { customCacheKey = value }
        fun setTag(value: Any?) = apply { tag = value }
        fun setMediaMetadata(value: MediaMetadata) = apply { mediaMetadata = value }

        fun build(): MediaItem {
            val config = uri?.let { LocalConfiguration(it, mimeType, customCacheKey, tag) }
            return MediaItem(mediaId ?: DEFAULT_MEDIA_ID, config, mediaMetadata)
        }
    }

    companion object {
        const val DEFAULT_MEDIA_ID = ""
        val EMPTY: MediaItem = Builder().build()

        fun fromUri(uri: String): MediaItem = Builder().setUri(uri).build()

        fun fromUri(uri: Uri): MediaItem = Builder().setUri(uri).build()
    }
}
