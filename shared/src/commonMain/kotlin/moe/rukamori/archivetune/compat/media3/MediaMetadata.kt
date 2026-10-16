package moe.rukamori.archivetune.compat.media3

import moe.rukamori.archivetune.compat.Bundle
import moe.rukamori.archivetune.compat.Uri

/** Sostituto di androidx.media3.common.MediaMetadata (i dati per notifica e schermata di blocco). */
class MediaMetadata private constructor(builder: Builder) {
    val title: CharSequence? = builder.title
    val subtitle: CharSequence? = builder.subtitle
    val artist: CharSequence? = builder.artist
    val albumTitle: CharSequence? = builder.albumTitle
    val albumArtist: CharSequence? = builder.albumArtist
    val displayTitle: CharSequence? = builder.displayTitle
    val artworkUri: Uri? = builder.artworkUri
    val artworkData: ByteArray? = builder.artworkData
    val trackNumber: Int? = builder.trackNumber
    val discNumber: Int? = builder.discNumber
    val releaseYear: Int? = builder.releaseYear
    val isPlayable: Boolean? = builder.isPlayable
    val isBrowsable: Boolean? = builder.isBrowsable
    val mediaType: Int? = builder.mediaType
    val durationMs: Long? = builder.durationMs
    val extras: Bundle? = builder.extras

    fun buildUpon(): Builder =
        Builder().also {
            it.title = title
            it.subtitle = subtitle
            it.artist = artist
            it.albumTitle = albumTitle
            it.albumArtist = albumArtist
            it.displayTitle = displayTitle
            it.artworkUri = artworkUri
            it.artworkData = artworkData
            it.trackNumber = trackNumber
            it.discNumber = discNumber
            it.releaseYear = releaseYear
            it.isPlayable = isPlayable
            it.isBrowsable = isBrowsable
            it.mediaType = mediaType
            it.durationMs = durationMs
            it.extras = extras
        }

    class Builder {
        internal var title: CharSequence? = null
        internal var subtitle: CharSequence? = null
        internal var artist: CharSequence? = null
        internal var albumTitle: CharSequence? = null
        internal var albumArtist: CharSequence? = null
        internal var displayTitle: CharSequence? = null
        internal var artworkUri: Uri? = null
        internal var artworkData: ByteArray? = null
        internal var trackNumber: Int? = null
        internal var discNumber: Int? = null
        internal var releaseYear: Int? = null
        internal var isPlayable: Boolean? = null
        internal var isBrowsable: Boolean? = null
        internal var mediaType: Int? = null
        internal var durationMs: Long? = null
        internal var extras: Bundle? = null

        fun setTitle(value: CharSequence?) = apply { title = value }
        fun setSubtitle(value: CharSequence?) = apply { subtitle = value }
        fun setArtist(value: CharSequence?) = apply { artist = value }
        fun setAlbumTitle(value: CharSequence?) = apply { albumTitle = value }
        fun setAlbumArtist(value: CharSequence?) = apply { albumArtist = value }
        fun setDisplayTitle(value: CharSequence?) = apply { displayTitle = value }
        fun setArtworkUri(value: Uri?) = apply { artworkUri = value }
        fun setArtworkData(value: ByteArray?, pictureType: Int? = null) = apply { artworkData = value }
        fun setTrackNumber(value: Int?) = apply { trackNumber = value }
        fun setDiscNumber(value: Int?) = apply { discNumber = value }
        fun setReleaseYear(value: Int?) = apply { releaseYear = value }
        fun setIsPlayable(value: Boolean?) = apply { isPlayable = value }
        fun setIsBrowsable(value: Boolean?) = apply { isBrowsable = value }
        fun setMediaType(value: Int?) = apply { mediaType = value }
        fun setDurationMs(value: Long?) = apply { durationMs = value }
        fun setExtras(value: Bundle?) = apply { extras = value }

        fun build(): MediaMetadata = MediaMetadata(this)
    }

    companion object {
        val EMPTY: MediaMetadata = Builder().build()

        const val MEDIA_TYPE_MIXED = 0
        const val MEDIA_TYPE_MUSIC = 1
        const val MEDIA_TYPE_AUDIO_BOOK_CHAPTER = 2
        const val MEDIA_TYPE_PODCAST_EPISODE = 3
        const val MEDIA_TYPE_FOLDER_MIXED = 20
        const val MEDIA_TYPE_FOLDER_ALBUMS = 22
        const val MEDIA_TYPE_FOLDER_ARTISTS = 23
        const val MEDIA_TYPE_FOLDER_PLAYLISTS = 25
        const val MEDIA_TYPE_PLAYLIST = 26
        const val MEDIA_TYPE_ALBUM = 10
        const val MEDIA_TYPE_ARTIST = 11
        const val PICTURE_TYPE_FRONT_COVER = 3
    }
}
