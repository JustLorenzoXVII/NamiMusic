package moe.rukamori.archivetune.compat

/**
 * Mappa LRU: come LinkedHashMap(capacity, 0.75f, accessOrder = true) con removeEldestEntry,
 * che su iPhone non si può usare (LinkedHashMap lì è final e non ha l'ordine di accesso).
 * Ogni lettura sposta la voce in fondo; oltre [maxSize] voci si toglie la più vecchia.
 */
class LruMap<K, V>(
    private val maxSize: Int = Int.MAX_VALUE,
) : MutableMap<K, V> {
    private val delegate = LinkedHashMap<K, V>()

    override val size: Int
        get() = delegate.size

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun containsKey(key: K): Boolean = delegate.containsKey(key)

    override fun containsValue(value: V): Boolean = delegate.containsValue(value)

    override fun get(key: K): V? {
        if (!delegate.containsKey(key)) return null
        @Suppress("UNCHECKED_CAST")
        val value = delegate.remove(key) as V
        delegate[key] = value
        return value
    }

    override val keys: MutableSet<K>
        get() = delegate.keys

    override val values: MutableCollection<V>
        get() = delegate.values

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = delegate.entries

    override fun put(
        key: K,
        value: V,
    ): V? {
        val previous = delegate.remove(key)
        delegate[key] = value
        trim()
        return previous
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) -> put(key, value) }
    }

    override fun remove(key: K): V? = delegate.remove(key)

    override fun clear() = delegate.clear()

    private fun trim() {
        while (delegate.size > maxSize) {
            val eldest = delegate.keys.firstOrNull() ?: return
            delegate.remove(eldest)
        }
    }
}
