package moe.rukamori.archivetune.compat

/** Sostituto multipiattaforma di android.os.Bundle (mappa chiave/valore). */
class Bundle() {
    private val values = LinkedHashMap<String, Any?>()

    constructor(other: Bundle) : this() {
        values.putAll(other.values)
    }

    fun keySet(): Set<String> = values.keys

    fun containsKey(key: String): Boolean = key in values

    fun isEmpty(): Boolean = values.isEmpty()

    fun size(): Int = values.size

    fun remove(key: String) {
        values.remove(key)
    }

    fun clear() = values.clear()

    fun get(key: String): Any? = values[key]

    fun putAll(other: Bundle) = values.putAll(other.values)

    fun putBoolean(key: String, value: Boolean) { values[key] = value }
    fun putInt(key: String, value: Int) { values[key] = value }
    fun putLong(key: String, value: Long) { values[key] = value }
    fun putFloat(key: String, value: Float) { values[key] = value }
    fun putDouble(key: String, value: Double) { values[key] = value }
    fun putString(key: String, value: String?) { values[key] = value }
    fun putStringArrayList(key: String, value: ArrayList<String>?) { values[key] = value }
    fun putBundle(key: String, value: Bundle?) { values[key] = value }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = values[key] as? Boolean ?: defaultValue
    fun getInt(key: String, defaultValue: Int = 0): Int = values[key] as? Int ?: defaultValue
    fun getLong(key: String, defaultValue: Long = 0L): Long = values[key] as? Long ?: defaultValue
    fun getFloat(key: String, defaultValue: Float = 0f): Float = values[key] as? Float ?: defaultValue
    fun getDouble(key: String, defaultValue: Double = 0.0): Double = values[key] as? Double ?: defaultValue
    fun getString(key: String): String? = values[key] as? String
    fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue

    @Suppress("UNCHECKED_CAST")
    fun getStringArrayList(key: String): ArrayList<String>? = values[key] as? ArrayList<String>
    fun getBundle(key: String): Bundle? = values[key] as? Bundle

    override fun toString(): String = "Bundle$values"

    companion object {
        val EMPTY = Bundle()
    }
}

fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle =
    Bundle().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Double -> putDouble(key, value)
                is Bundle -> putBundle(key, value)
                else -> putString(key, value?.toString())
            }
        }
    }
