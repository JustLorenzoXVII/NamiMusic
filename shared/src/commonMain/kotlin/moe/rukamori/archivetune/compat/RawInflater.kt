package moe.rukamori.archivetune.compat

/**
 * Decoder DEFLATE (RFC 1951) senza intestazioni zlib, come java.util.zip.Inflater(nowrap = true).
 * Basato sull'algoritmo di "puff" di Mark Adler: blocchi non compressi, Huffman fissi e dinamici.
 */
internal class RawInflater(
    private val input: ByteArray,
    expectedSize: Int,
) {
    private var inPos = 0
    private var bitBuffer = 0
    private var bitCount = 0
    private var out = ByteArray(maxOf(expectedSize, 1024))
    private var outPos = 0

    fun inflate(): ByteArray {
        do {
            val last = bits(1)
            when (bits(2)) {
                0 -> stored()
                1 -> codes(FixedTables.lengthCode, FixedTables.distanceCode)
                2 -> dynamic()
                else -> throw kotlinx.io.IOException("Blocco DEFLATE non valido")
            }
        } while (last == 0)
        return out.copyOf(outPos)
    }

    private fun bits(need: Int): Int {
        var value = bitBuffer
        while (bitCount < need) {
            if (inPos >= input.size) throw kotlinx.io.IOException("Dati DEFLATE incompleti")
            value = value or ((input[inPos++].toInt() and 0xFF) shl bitCount)
            bitCount += 8
        }
        bitBuffer = value ushr need
        bitCount -= need
        return value and ((1 shl need) - 1)
    }

    private fun put(byte: Int) {
        if (outPos == out.size) out = out.copyOf(out.size * 2)
        out[outPos++] = byte.toByte()
    }

    private fun stored() {
        bitBuffer = 0
        bitCount = 0
        if (inPos + 4 > input.size) throw kotlinx.io.IOException("Blocco non compresso incompleto")
        val len = (input[inPos].toInt() and 0xFF) or ((input[inPos + 1].toInt() and 0xFF) shl 8)
        inPos += 4
        if (inPos + len > input.size) throw kotlinx.io.IOException("Blocco non compresso incompleto")
        repeat(len) { put(input[inPos++].toInt()) }
    }

    internal class Huffman(
        val count: IntArray,
        val symbol: IntArray,
    )

    private fun decode(h: Huffman): Int {
        var code = 0
        var first = 0
        var index = 0
        for (len in 1 until MAX_BITS + 1) {
            code = code or bits(1)
            val count = h.count[len]
            if (code - count < first) return h.symbol[index + (code - first)]
            index += count
            first += count
            first = first shl 1
            code = code shl 1
        }
        throw kotlinx.io.IOException("Codice Huffman non valido")
    }

    private fun codes(
        lencode: Huffman,
        distcode: Huffman,
    ) {
        while (true) {
            var symbol = decode(lencode)
            if (symbol < 256) {
                put(symbol)
            } else if (symbol == 256) {
                return
            } else {
                symbol -= 257
                if (symbol >= 29) throw kotlinx.io.IOException("Lunghezza non valida")
                val len = LENGTH_BASE[symbol] + bits(LENGTH_EXTRA[symbol])
                val distSymbol = decode(distcode)
                if (distSymbol >= 30) throw kotlinx.io.IOException("Distanza non valida")
                val dist = DIST_BASE[distSymbol] + bits(DIST_EXTRA[distSymbol])
                if (dist > outPos) throw kotlinx.io.IOException("Distanza oltre l'inizio")
                repeat(len) { put(out[outPos - dist].toInt()) }
            }
        }
    }

    private fun dynamic() {
        val nlen = bits(5) + 257
        val ndist = bits(5) + 1
        val ncode = bits(4) + 4
        if (nlen > 286 || ndist > 30) throw kotlinx.io.IOException("Tabelle dinamiche non valide")
        val lengths = IntArray(320)
        for (index in 0 until ncode) lengths[ORDER[index]] = bits(3)
        val lencode = construct(lengths, 0, 19)
        var index = 0
        while (index < nlen + ndist) {
            var symbol = decode(lencode)
            if (symbol < 16) {
                lengths[index++] = symbol
            } else {
                var len = 0
                when (symbol) {
                    16 -> {
                        if (index == 0) throw kotlinx.io.IOException("Ripetizione senza lunghezza")
                        len = lengths[index - 1]
                        symbol = 3 + bits(2)
                    }
                    17 -> symbol = 3 + bits(3)
                    else -> symbol = 11 + bits(7)
                }
                if (index + symbol > nlen + ndist) throw kotlinx.io.IOException("Troppe lunghezze")
                repeat(symbol) { lengths[index++] = len }
            }
        }
        if (lengths[256] == 0) throw kotlinx.io.IOException("Manca il codice di fine blocco")
        codes(construct(lengths, 0, nlen), construct(lengths, nlen, ndist))
    }

    companion object {
        private const val MAX_BITS = 15
        private val LENGTH_BASE = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
        private val LENGTH_EXTRA = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
        private val DIST_BASE = intArrayOf(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
        private val DIST_EXTRA = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)
        private val ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

        internal fun construct(
            lengths: IntArray,
            offset: Int,
            n: Int,
        ): Huffman {
            val count = IntArray(MAX_BITS + 1)
            for (symbol in 0 until n) count[lengths[offset + symbol]]++
            val offs = IntArray(MAX_BITS + 1)
            for (len in 1 until MAX_BITS) offs[len + 1] = offs[len] + count[len]
            val symbols = IntArray(n)
            for (symbol in 0 until n) {
                val len = lengths[offset + symbol]
                if (len != 0) symbols[offs[len]++] = symbol
            }
            count[0] = 0
            return Huffman(count, symbols)
        }
    }

    private object FixedTables {
        val lengthCode: Huffman
        val distanceCode: Huffman

        init {
            val lengths = IntArray(288)
            for (symbol in 0 until 144) lengths[symbol] = 8
            for (symbol in 144 until 256) lengths[symbol] = 9
            for (symbol in 256 until 280) lengths[symbol] = 7
            for (symbol in 280 until 288) lengths[symbol] = 8
            lengthCode = construct(lengths, 0, 288)
            val distances = IntArray(30) { 5 }
            distanceCode = construct(distances, 0, 30)
        }
    }
}
