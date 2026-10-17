package moe.rukamori.archivetune.compat

/**
 * java.lang.Character: solo la direzionalità, usata per capire se un testo è da destra a sinistra.
 * Calcolata sui blocchi Unicode principali (ebraico, arabo, siriaco, thaana, n'ko...).
 */
object Character {
    const val DIRECTIONALITY_UNDEFINED: Byte = -1
    const val DIRECTIONALITY_LEFT_TO_RIGHT: Byte = 0
    const val DIRECTIONALITY_RIGHT_TO_LEFT: Byte = 1
    const val DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC: Byte = 2
    const val DIRECTIONALITY_EUROPEAN_NUMBER: Byte = 3
    const val DIRECTIONALITY_OTHER_NEUTRALS: Byte = 13
    const val DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING: Byte = 14
    const val DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE: Byte = 15
    const val DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING: Byte = 16
    const val DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE: Byte = 17

    fun getDirectionality(ch: Char): Byte {
        val code = ch.code
        return when {
            code == 0x202A -> DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
            code == 0x202D -> DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE
            code == 0x202B -> DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
            code == 0x202E -> DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
            code in 0x0590..0x05FF || code in 0x07C0..0x07FF || code in 0x0800..0x085F ||
                code in 0xFB1D..0xFB4F || code == 0x200F -> DIRECTIONALITY_RIGHT_TO_LEFT
            code in 0x0600..0x06FF || code in 0x0700..0x074F || code in 0x0750..0x077F ||
                code in 0x0780..0x07BF || code in 0x08A0..0x08FF || code in 0xFB50..0xFDFF ||
                code in 0xFE70..0xFEFF -> if (ch.isDigit()) DIRECTIONALITY_EUROPEAN_NUMBER else DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
            ch.isDigit() -> DIRECTIONALITY_EUROPEAN_NUMBER
            ch.isLetter() || code == 0x200E -> DIRECTIONALITY_LEFT_TO_RIGHT
            ch.isWhitespace() || !ch.isLetterOrDigit() -> DIRECTIONALITY_OTHER_NEUTRALS
            else -> DIRECTIONALITY_UNDEFINED
        }
    }
}
