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
}

/** Sostituto minimo di com.google.common.collect.ImmutableList (Guava). */
object ImmutableList {
    fun <T> of(vararg elements: T): List<T> = elements.toList()

    fun <T> copyOf(elements: Collection<T>): List<T> = elements.toList()

    fun <T> copyOf(elements: Iterable<T>): List<T> = elements.toList()
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
