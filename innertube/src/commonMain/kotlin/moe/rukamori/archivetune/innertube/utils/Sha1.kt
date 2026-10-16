package moe.rukamori.archivetune.innertube.utils

/**
 * SHA-1 in puro Kotlin (al posto di java.security.MessageDigest, che su iOS non esiste).
 * Serve per l'intestazione SAPISIDHASH delle richieste con l'account.
 */
internal object Sha1 {
    fun digest(input: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()

        val bitLength = input.size.toLong() * 8
        val paddedSize = ((input.size + 9 + 63) / 64) * 64
        val message = ByteArray(paddedSize)
        input.copyInto(message)
        message[input.size] = 0x80.toByte()
        for (i in 0 until 8) {
            message[paddedSize - 1 - i] = (bitLength ushr (8 * i)).toByte()
        }

        val w = IntArray(80)
        for (chunk in 0 until paddedSize / 64) {
            val offset = chunk * 64
            for (i in 0 until 16) {
                val j = offset + i * 4
                w[i] = ((message[j].toInt() and 0xFF) shl 24) or
                    ((message[j + 1].toInt() and 0xFF) shl 16) or
                    ((message[j + 2].toInt() and 0xFF) shl 8) or
                    (message[j + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    i < 20 -> { f = (b and c) or (b.inv() and d); k = 0x5A827999 }
                    i < 40 -> { f = b xor c xor d; k = 0x6ED9EBA1 }
                    i < 60 -> { f = (b and c) or (b and d) or (c and d); k = 0x8F1BBCDC.toInt() }
                    else -> { f = b xor c xor d; k = 0xCA62C1D6.toInt() }
                }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }
            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
        }

        val result = ByteArray(20)
        intArrayOf(h0, h1, h2, h3, h4).forEachIndexed { index, value ->
            for (i in 0 until 4) {
                result[index * 4 + i] = (value ushr (24 - 8 * i)).toByte()
            }
        }
        return result
    }
}
