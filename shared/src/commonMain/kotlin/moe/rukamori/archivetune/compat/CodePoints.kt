package moe.rukamori.archivetune.compat

/** String.codePointAt / codePointCount / offsetByCodePoints di Java, per testi con emoji e surrogati. */
fun String.codePointAt(index: Int): Int {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
        }
    }
    return high.code
}

fun String.codePointCount(
    beginIndex: Int,
    endIndex: Int,
): Int {
    var count = 0
    var i = beginIndex
    while (i < endIndex) {
        i += if (this[i].isHighSurrogate() && i + 1 < endIndex && this[i + 1].isLowSurrogate()) 2 else 1
        count++
    }
    return count
}

fun String.offsetByCodePoints(
    index: Int,
    codePointOffset: Int,
): Int {
    var i = index
    repeat(codePointOffset) {
        if (i >= length) return length
        i += if (this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1
    }
    return i
}

/** java.lang.Integer: solo le funzioni sui bit usate da ArchiveTune. */
object Integer {
    fun numberOfTrailingZeros(value: Int): Int = value.countTrailingZeroBits()

    fun numberOfLeadingZeros(value: Int): Int = value.countLeadingZeroBits()

    fun bitCount(value: Int): Int = value.countOneBits()

    const val MAX_VALUE: Int = Int.MAX_VALUE
    const val MIN_VALUE: Int = Int.MIN_VALUE
}
