package moe.rukamori.archivetune.db

import androidx.room.Room
import androidx.room.RoomDatabase
import com.justlorenzo.archivetunex.player.AndroidAppContext

internal actual fun platformDatabaseBuilder(name: String): RoomDatabase.Builder<InternalDatabase> {
    val context = AndroidAppContext.context
    return Room.databaseBuilder<InternalDatabase>(
        context = context,
        name = context.getDatabasePath(name).absolutePath,
    )
}
