@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package moe.rukamori.archivetune.compat

import kotlin.concurrent.atomics.AtomicBoolean as KAtomicBoolean
import kotlin.concurrent.atomics.AtomicInt as KAtomicInt
import kotlin.concurrent.atomics.AtomicLong as KAtomicLong
import kotlin.concurrent.atomics.AtomicReference as KAtomicReference
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.decrementAndFetch

/** java.util.concurrent.atomic.AtomicLong sui tipi atomici di Kotlin. */
class AtomicLong(initialValue: Long = 0L) {
    private val value = KAtomicLong(initialValue)

    fun get(): Long = value.load()

    fun set(newValue: Long) = value.store(newValue)

    fun incrementAndGet(): Long = value.incrementAndFetch()

    fun getAndIncrement(): Long = value.fetchAndIncrement()

    fun decrementAndGet(): Long = value.decrementAndFetch()

    fun addAndGet(delta: Long): Long = value.addAndFetch(delta)

    fun getAndSet(newValue: Long): Long = value.exchange(newValue)

    fun compareAndSet(expect: Long, update: Long): Boolean = value.compareAndSet(expect, update)

    fun updateAndGet(update: (Long) -> Long): Long {
        while (true) {
            val current = get()
            val next = update(current)
            if (compareAndSet(current, next)) return next
        }
    }

    override fun toString(): String = get().toString()
}

class AtomicInteger(initialValue: Int = 0) {
    private val value = KAtomicInt(initialValue)

    fun get(): Int = value.load()

    fun set(newValue: Int) = value.store(newValue)

    fun incrementAndGet(): Int = value.incrementAndFetch()

    fun getAndIncrement(): Int = value.fetchAndIncrement()

    fun decrementAndGet(): Int = value.decrementAndFetch()

    fun addAndGet(delta: Int): Int = value.addAndFetch(delta)

    fun getAndSet(newValue: Int): Int = value.exchange(newValue)

    fun compareAndSet(expect: Int, update: Int): Boolean = value.compareAndSet(expect, update)

    override fun toString(): String = get().toString()
}

class AtomicBoolean(initialValue: Boolean = false) {
    private val value = KAtomicBoolean(initialValue)

    fun get(): Boolean = value.load()

    fun set(newValue: Boolean) = value.store(newValue)

    fun getAndSet(newValue: Boolean): Boolean = value.exchange(newValue)

    fun compareAndSet(expect: Boolean, update: Boolean): Boolean = value.compareAndSet(expect, update)

    override fun toString(): String = get().toString()
}

class AtomicReference<T>(initialValue: T) {
    private val value = KAtomicReference(initialValue)

    fun get(): T = value.load()

    fun set(newValue: T) = value.store(newValue)

    fun getAndSet(newValue: T): T = value.exchange(newValue)

    fun compareAndSet(expect: T, update: T): Boolean = value.compareAndSet(expect, update)
}

/** java.util.concurrent.ConcurrentHashMap: qui una mappa normale (l'app la usa dal thread principale o con Mutex). */
class ConcurrentHashMap<K, V> private constructor(
    private val map: LinkedHashMap<K, V>,
) : MutableMap<K, V> by map {
    constructor() : this(LinkedHashMap())

    constructor(initialCapacity: Int) : this(LinkedHashMap(initialCapacity))

    fun putIfAbsent(key: K, value: V): V? = map[key] ?: run {
        map[key] = value
        null
    }

    fun remove(key: K, value: V): Boolean =
        if (map[key] == value) {
            map.remove(key)
            true
        } else {
            false
        }

    fun replace(key: K, oldValue: V, newValue: V): Boolean =
        if (map[key] == oldValue) {
            map[key] = newValue
            true
        } else {
            false
        }

    fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V = map.getOrPut(key) { mappingFunction(key) }

    companion object {
        fun <T> newKeySet(): MutableSet<T> = LinkedHashSet()
    }
}
