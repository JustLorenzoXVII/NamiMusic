package moe.rukamori.archivetune.compat.media3

import kotlin.random.Random

/** Ordine di riproduzione casuale (androidx.media3.exoplayer.source.ShuffleOrder). */
interface ShuffleOrder {
    val length: Int

    fun getNextIndex(index: Int): Int

    fun getPreviousIndex(index: Int): Int

    fun getFirstIndex(): Int

    fun getLastIndex(): Int

    fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder

    fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder

    fun cloneAndClear(): ShuffleOrder

    /** Permutazione degli indici: [shuffled] dice in che ordine si suonano gli elementi. */
    class DefaultShuffleOrder(
        private val shuffled: IntArray,
        private val random: Random = Random.Default,
    ) : ShuffleOrder {
        constructor(shuffledIndices: IntArray, randomSeed: Long) : this(shuffledIndices, Random(randomSeed))

        private val indexInShuffled = IntArray(shuffled.size).also { inv -> shuffled.forEachIndexed { pos, value -> inv[value] = pos } }

        constructor(length: Int, randomSeed: Long = Random.nextLong()) : this(createShuffledList(length, Random(randomSeed)), Random(randomSeed))

        override val length: Int
            get() = shuffled.size

        override fun getNextIndex(index: Int): Int {
            val pos = indexInShuffled[index] + 1
            return if (pos < shuffled.size) shuffled[pos] else C.INDEX_UNSET
        }

        override fun getPreviousIndex(index: Int): Int {
            val pos = indexInShuffled[index] - 1
            return if (pos >= 0) shuffled[pos] else C.INDEX_UNSET
        }

        override fun getFirstIndex(): Int = if (shuffled.isEmpty()) C.INDEX_UNSET else shuffled[0]

        override fun getLastIndex(): Int = if (shuffled.isEmpty()) C.INDEX_UNSET else shuffled[shuffled.size - 1]

        override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
            val insertionPoints = IntArray(insertionCount) { random.nextInt(shuffled.size + 1) }
            val insertionValues = IntArray(insertionCount) { insertionIndex + it }
            insertionPoints.sort()
            val result = IntArray(shuffled.size + insertionCount)
            var indexInOld = 0
            var indexInInsertion = 0
            for (i in result.indices) {
                if (indexInInsertion < insertionCount && indexInOld == insertionPoints[indexInInsertion]) {
                    result[i] = insertionValues[indexInInsertion++]
                } else {
                    var value = shuffled[indexInOld++]
                    if (value >= insertionIndex) value += insertionCount
                    result[i] = value
                }
            }
            return DefaultShuffleOrder(result, Random(random.nextLong()))
        }

        override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
            val removed = indexToExclusive - indexFrom
            val result = IntArray(shuffled.size - removed)
            var found = 0
            for (i in shuffled.indices) {
                val value = shuffled[i]
                if (value in indexFrom until indexToExclusive) {
                    found++
                } else {
                    result[i - found] = if (value >= indexFrom) value - removed else value
                }
            }
            return DefaultShuffleOrder(result, Random(random.nextLong()))
        }

        override fun cloneAndClear(): ShuffleOrder = DefaultShuffleOrder(IntArray(0), Random(random.nextLong()))

        private companion object {
            fun createShuffledList(length: Int, random: Random): IntArray {
                val list = IntArray(length)
                for (i in 0 until length) {
                    val swap = random.nextInt(i + 1)
                    list[i] = list[swap]
                    list[swap] = i
                }
                return list
            }
        }
    }
}
