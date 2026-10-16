package moe.rukamori.archivetune.compat

import kotlin.random.Random

/** Al posto di java.io.Serializable: su Android è proprio quella, su iPhone un'interfaccia vuota. */
expect interface Serializable

/** Le funzioni di org.apache.commons.lang3.RandomStringUtils usate da ArchiveTune. */
object RandomStringUtils {
    private const val ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val ALPHABETIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val NUMERIC = "0123456789"

    fun random(count: Int, letters: Boolean, numbers: Boolean): String {
        val source =
            when {
                letters && numbers -> ALPHANUMERIC
                letters -> ALPHABETIC
                numbers -> NUMERIC
                else -> ALPHANUMERIC
            }
        return buildString(count) { repeat(count) { append(source[Random.nextInt(source.length)]) } }
    }

    fun randomAlphanumeric(count: Int): String = random(count, letters = true, numbers = true)

    fun randomAlphabetic(count: Int): String = random(count, letters = true, numbers = false)

    fun randomNumeric(count: Int): String = random(count, letters = false, numbers = true)

    fun insecure(): RandomStringUtils = this

    fun secure(): RandomStringUtils = this

    fun nextAlphanumeric(count: Int): String = randomAlphanumeric(count)

    fun nextAlphabetic(count: Int): String = randomAlphabetic(count)

    fun nextNumeric(count: Int): String = randomNumeric(count)

    fun next(count: Int): String = random(count, letters = true, numbers = true)

    fun next(count: Int, letters: Boolean, numbers: Boolean): String = random(count, letters, numbers)
}

/** Sostituto minimo di com.google.common.collect.ImmutableList (Guava). */
/** Come com.google.common.collect.ImmutableList di Guava: una lista che non cambia. */
@androidx.compose.runtime.Immutable
class ImmutableList<out E> private constructor(
    private val list: List<E>,
) : List<E> by list {
    override fun equals(other: Any?): Boolean = list == other

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = list.toString()

    companion object {
        fun <T> of(vararg elements: T): ImmutableList<T> = ImmutableList(elements.toList())

        fun <T> of(): ImmutableList<T> = ImmutableList(emptyList())

        fun <T> copyOf(elements: Collection<T>): ImmutableList<T> = elements as? ImmutableList<T> ?: ImmutableList(elements.toList())

        fun <T> copyOf(elements: Iterable<T>): ImmutableList<T> = elements as? ImmutableList<T> ?: ImmutableList(elements.toList())

        fun <T> copyOf(elements: Array<out T>): ImmutableList<T> = ImmutableList(elements.toList())
    }
}

/** Come com.google.common.collect.ImmutableSet di Guava. */
@androidx.compose.runtime.Immutable
class ImmutableSet<out E> private constructor(
    private val set: Set<E>,
) : Set<E> by set {
    override fun equals(other: Any?): Boolean = set == other

    override fun hashCode(): Int = set.hashCode()

    override fun toString(): String = set.toString()

    companion object {
        fun <T> of(vararg elements: T): ImmutableSet<T> = ImmutableSet(elements.toCollection(LinkedHashSet()))

        fun <T> copyOf(elements: Collection<T>): ImmutableSet<T> = elements as? ImmutableSet<T> ?: ImmutableSet(LinkedHashSet(elements))

        fun <T> copyOf(elements: Iterable<T>): ImmutableSet<T> = elements as? ImmutableSet<T> ?: ImmutableSet(elements.toCollection(LinkedHashSet()))
    }
}

/** Come java.text.Normalizer (NFC, NFD, NFKC, NFKD). */
object Normalizer {
    enum class Form { NFC, NFD, NFKC, NFKD }

    fun normalize(
        src: CharSequence,
        form: Form,
    ): String = platformNormalize(src.toString(), form)
}

internal expect fun platformNormalize(
    value: String,
    form: Normalizer.Form,
): String
