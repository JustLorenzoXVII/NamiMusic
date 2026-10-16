package moe.rukamori.archivetune.compat

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Le funzioni di android.graphics.Color usate da ArchiveTune, in Kotlin comune.
 * Permettono di riusare il codice originale sui colori anche su iPhone.
 */
object AndroidColor {
    const val BLACK: Int = 0xFF000000.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val TRANSPARENT: Int = 0

    fun alpha(color: Int): Int = color ushr 24

    fun red(color: Int): Int = (color shr 16) and 0xFF

    fun green(color: Int): Int = (color shr 8) and 0xFF

    fun blue(color: Int): Int = color and 0xFF

    fun rgb(
        red: Int,
        green: Int,
        blue: Int,
    ): Int = argb(255, red, green, blue)

    fun argb(
        alpha: Int,
        red: Int,
        green: Int,
        blue: Int,
    ): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    fun colorToHSV(
        color: Int,
        hsv: FloatArray,
    ) {
        RGBToHSV(red(color), green(color), blue(color), hsv)
    }

    @Suppress("FunctionName")
    fun RGBToHSV(
        red: Int,
        green: Int,
        blue: Int,
        hsv: FloatArray,
    ) {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue =
            when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6f)
                max == g -> 60f * (((b - r) / delta) + 2f)
                else -> 60f * (((r - g) / delta) + 4f)
            }
        hsv[0] = if (hue < 0f) hue + 360f else hue
        hsv[1] = if (max == 0f) 0f else delta / max
        hsv[2] = max
    }

    @Suppress("FunctionName")
    fun HSVToColor(hsv: FloatArray): Int = HSVToColor(255, hsv)

    @Suppress("FunctionName")
    fun HSVToColor(
        alpha: Int,
        hsv: FloatArray,
    ): Int {
        val h = ((hsv[0] % 360f) + 360f) % 360f
        val s = hsv[1].coerceIn(0f, 1f)
        val v = hsv[2].coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) =
            when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
        return argb(
            alpha.coerceIn(0, 255),
            ((r + m) * 255f).roundToInt().coerceIn(0, 255),
            ((g + m) * 255f).roundToInt().coerceIn(0, 255),
            ((b + m) * 255f).roundToInt().coerceIn(0, 255),
        )
    }

    /** Come Color.parseColor: "#RRGGBB" o "#AARRGGBB". */
    fun parseColor(colorString: String): Int {
        require(colorString.startsWith("#")) { "Unknown color" }
        val hex = colorString.substring(1)
        val value = hex.toLong(16)
        return when (hex.length) {
            6 -> (value or 0xFF000000L).toInt()
            8 -> value.toInt()
            else -> throw IllegalArgumentException("Unknown color")
        }
    }
}

/** Equivalente di androidx.core.graphics.toColorInt. */
fun String.toColorInt(): Int = AndroidColor.parseColor(this)

/** androidx.core.graphics.ColorUtils (luminanza e miscela di colori ARGB). */
object ColorUtils {
    fun calculateLuminance(color: Int): Double {
        fun channel(c: Int): Double {
            val v = c / 255.0
            return if (v < 0.03928) v / 12.92 else ((v + 0.055) / 1.055).let { kotlin.math.exp(2.4 * kotlin.math.ln(it)) }
        }
        val r = channel((color shr 16) and 0xFF)
        val g = channel((color shr 8) and 0xFF)
        val b = channel(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun blendARGB(color1: Int, color2: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        val a = ((color1 ushr 24) and 0xFF) * inverse + ((color2 ushr 24) and 0xFF) * ratio
        val r = ((color1 shr 16) and 0xFF) * inverse + ((color2 shr 16) and 0xFF) * ratio
        val g = ((color1 shr 8) and 0xFF) * inverse + ((color2 shr 8) and 0xFF) * ratio
        val b = (color1 and 0xFF) * inverse + (color2 and 0xFF) * ratio
        return (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    fun setAlphaComponent(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
}
