package moe.rukamori.archivetune.compat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * android.content.SharedPreferences: piccolo archivio chiave/valore salvato come JSON
 * nella cartella dell'app (shared_prefs/<nome>.json). Usato da poche schermate di ArchiveTune;
 * le impostazioni vere sono nel DataStore.
 */
class SharedPreferences internal constructor(
    private val file: File,
) {
    private var values: MutableMap<String, JsonElement> = load()

    private fun load(): MutableMap<String, JsonElement> =
        runCatching {
            if (!file.exists()) return@runCatching mutableMapOf()
            (Json.parseToJsonElement(file.readText()) as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        }.getOrDefault(mutableMapOf())

    private fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(JsonObject(values).toString())
        }
    }

    private fun primitive(key: String): JsonPrimitive? = (values[key] as? JsonPrimitive)

    fun contains(key: String): Boolean = values.containsKey(key)

    fun getString(
        key: String,
        defValue: String?,
    ): String? = primitive(key)?.contentOrNull ?: defValue

    fun getLong(
        key: String,
        defValue: Long,
    ): Long = primitive(key)?.longOrNull ?: defValue

    fun getInt(
        key: String,
        defValue: Int,
    ): Int = primitive(key)?.longOrNull?.toInt() ?: defValue

    fun getFloat(
        key: String,
        defValue: Float,
    ): Float = primitive(key)?.doubleOrNull?.toFloat() ?: defValue

    fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean = primitive(key)?.booleanOrNull ?: defValue

    val all: Map<String, Any?>
        get() = values.mapValues { (_, value) -> value.jsonPrimitive.contentOrNull }

    fun edit(): Editor = Editor()

    inner class Editor internal constructor() {
        private val changes = mutableMapOf<String, JsonElement?>()
        private var clearAll = false

        fun putString(
            key: String,
            value: String?,
        ): Editor = apply { changes[key] = value?.let(::JsonPrimitive) }

        fun putLong(
            key: String,
            value: Long,
        ): Editor = apply { changes[key] = JsonPrimitive(value) }

        fun putInt(
            key: String,
            value: Int,
        ): Editor = apply { changes[key] = JsonPrimitive(value) }

        fun putFloat(
            key: String,
            value: Float,
        ): Editor = apply { changes[key] = JsonPrimitive(value) }

        fun putBoolean(
            key: String,
            value: Boolean,
        ): Editor = apply { changes[key] = JsonPrimitive(value) }

        fun remove(key: String): Editor = apply { changes[key] = null }

        fun clear(): Editor = apply { clearAll = true }

        fun apply() {
            commit()
        }

        fun commit(): Boolean {
            if (clearAll) values.clear()
            changes.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
            save()
            return true
        }
    }
}

private val sharedPreferencesCache = mutableMapOf<String, SharedPreferences>()

fun Context.getSharedPreferences(
    name: String,
    @Suppress("UNUSED_PARAMETER") mode: Int,
): SharedPreferences =
    sharedPreferencesCache.getOrPut(name) {
        SharedPreferences(File(File(filesDir, "shared_prefs"), "$name.json"))
    }

/** Context.getExternalFilesDirs: su iPhone e in ArchiveTuneX c'è solo la cartella dell'app. */
fun Context.getExternalFilesDirs(
    @Suppress("UNUSED_PARAMETER") type: String?,
): Array<File?> = arrayOf(filesDir)
