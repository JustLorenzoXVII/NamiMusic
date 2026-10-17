/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: database Room in versione multipiattaforma (Room KMP + SQLite incluso).
 * È un'app nuova, quindi lo schema parte dalla versione 1 senza le migrazioni storiche di ArchiveTune.
 */

package moe.rukamori.archivetune.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import moe.rukamori.archivetune.db.entities.AlbumArtistMap
import moe.rukamori.archivetune.db.entities.AlbumEntity
import moe.rukamori.archivetune.db.entities.ArtistEntity
import moe.rukamori.archivetune.db.entities.Event
import moe.rukamori.archivetune.db.entities.FormatEntity
import moe.rukamori.archivetune.db.entities.LibraryTopMixEntity
import moe.rukamori.archivetune.db.entities.LibraryTopMixSongMap
import moe.rukamori.archivetune.db.entities.LyricsEntity
import moe.rukamori.archivetune.db.entities.LocalMusicAlias
import moe.rukamori.archivetune.db.entities.LibrarySongArtistMap
import moe.rukamori.archivetune.db.entities.PlayCountEntity
import moe.rukamori.archivetune.db.entities.PlaylistEntity
import moe.rukamori.archivetune.db.entities.PlaylistSongMap
import moe.rukamori.archivetune.db.entities.PlaylistSongMapPreview
import moe.rukamori.archivetune.db.entities.PlaylistTagMap
import moe.rukamori.archivetune.db.entities.PodcastEntity
import moe.rukamori.archivetune.db.entities.RelatedSongMap
import moe.rukamori.archivetune.db.entities.SearchHistory
import moe.rukamori.archivetune.db.entities.SetVideoIdEntity
import moe.rukamori.archivetune.db.entities.SongAlbumMap
import moe.rukamori.archivetune.db.entities.SongArtistMap
import moe.rukamori.archivetune.db.entities.SongEntity
import moe.rukamori.archivetune.db.entities.SortedSongAlbumMap
import moe.rukamori.archivetune.db.entities.SortedSongArtistMap
import moe.rukamori.archivetune.db.entities.TagEntity

class MusicDatabase(
    private val delegate: InternalDatabase,
) : DatabaseDao by delegate.dao {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Esegue [block] in background (nell'originale su un Executor). */
    fun query(block: suspend MusicDatabase.() -> Unit) {
        scope.launch { block(this@MusicDatabase) }
    }

    /** Come [query], ma tutto dentro una transazione. */
    fun transaction(block: suspend MusicDatabase.() -> Unit) {
        scope.launch { withTransaction { block() } }
    }

    suspend fun <R> withTransaction(block: suspend MusicDatabase.() -> R): R =
        delegate.useWriterConnection { transactor ->
            transactor.immediateTransaction { block(this@MusicDatabase) }
        }

    suspend fun awaitIdle(timeoutMs: Long = 5_000L) {
        withTimeout(timeoutMs) {
            scope.coroutineContext[Job]?.children?.toList()?.joinAll()
        }
    }

    fun close() = delegate.close()

    companion object {
        const val DB_NAME = "archivetunex.db"

        fun newInstance(): MusicDatabase =
            MusicDatabase(
                delegate =
                    platformDatabaseBuilder(DB_NAME)
                        .setDriver(BundledSQLiteDriver())
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build(),
            )
    }
}

@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        PodcastEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        FormatEntity::class,
        LyricsEntity::class,
        LocalMusicAlias::class,
        Event::class,
        RelatedSongMap::class,
        SetVideoIdEntity::class,
        PlayCountEntity::class,
        TagEntity::class,
        PlaylistTagMap::class,
        LibraryTopMixEntity::class,
        LibraryTopMixSongMap::class,
    ],
    views = [
        LibrarySongArtistMap::class,
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
@ConstructedBy(InternalDatabaseConstructor::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao
}

// Room genera le implementazioni per Android e iOS.
@Suppress("KotlinNoActualForExpect")
expect object InternalDatabaseConstructor : RoomDatabaseConstructor<InternalDatabase> {
    override fun initialize(): InternalDatabase
}

/** Percorso del file del database sulla piattaforma. */
internal expect fun platformDatabaseBuilder(name: String): RoomDatabase.Builder<InternalDatabase>
