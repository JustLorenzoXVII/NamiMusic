package moe.rukamori.archivetune.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual fun platformDatabaseBuilder(name: String): RoomDatabase.Builder<InternalDatabase> {
    val documents =
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String
            ?: error("Cartella Documenti non trovata")
    return Room.databaseBuilder<InternalDatabase>(name = "$documents/$name")
}

internal actual fun platformDatabasePath(name: String): String =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String
        ?: error("Cartella Documenti non trovata")) + "/" + name
