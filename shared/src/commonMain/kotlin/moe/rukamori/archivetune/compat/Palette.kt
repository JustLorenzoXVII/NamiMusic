package moe.rukamori.archivetune.compat

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Porting in Kotlin comune di androidx.palette (Apache 2.0), con la stessa API usata da
 * ArchiveTune: Palette.from(bitmap).maximumColorCount(n).generate(), vibrantSwatch, swatches…
 * Lavora su ImageBitmap di Compose, quindi funziona anche su iPhone.
 */
class Palette private constructor(
    val swatches: List<Swatch>,
    private val targets: List<Target>,
) {
    private val selectedSwatches = mutableMapOf<Target, Swatch?>()
    val dominantSwatch: Swatch? = swatches.maxByOrNull { it.population }

    val lightVibrantSwatch: Swatch? get() = getSwatchForTarget(Target.LIGHT_VIBRANT)
    val vibrantSwatch: Swatch? get() = getSwatchForTarget(Target.VIBRANT)
    val darkVibrantSwatch: Swatch? get() = getSwatchForTarget(Target.DARK_VIBRANT)
    val lightMutedSwatch: Swatch? get() = getSwatchForTarget(Target.LIGHT_MUTED)
    val mutedSwatch: Swatch? get() = getSwatchForTarget(Target.MUTED)
    val darkMutedSwatch: Swatch? get() = getSwatchForTarget(Target.DARK_MUTED)

    fun getSwatchForTarget(target: Target): Swatch? = selectedSwatches[target]

    fun getLightVibrantColor(defaultColor: Int): Int = lightVibrantSwatch?.rgb ?: defaultColor

    fun getVibrantColor(defaultColor: Int): Int = vibrantSwatch?.rgb ?: defaultColor

    fun getDarkVibrantColor(defaultColor: Int): Int = darkVibrantSwatch?.rgb ?: defaultColor

    fun getLightMutedColor(defaultColor: Int): Int = lightMutedSwatch?.rgb ?: defaultColor

    fun getMutedColor(defaultColor: Int): Int = mutedSwatch?.rgb ?: defaultColor

    fun getDarkMutedColor(defaultColor: Int): Int = darkMutedSwatch?.rgb ?: defaultColor

    fun getDominantColor(defaultColor: Int): Int = dominantSwatch?.rgb ?: defaultColor

    private fun generate() {
        val used = mutableSetOf<Int>()
        for (target in targets) {
            target.normalizeWeights()
            val swatch = generateScoredTarget(target, used)
            selectedSwatches[target] = swatch
            if (swatch != null && target.isExclusive) used += swatch.rgb
        }
    }

    private fun generateScoredTarget(
        target: Target,
        used: Set<Int>,
    ): Swatch? {
        val maxPopulation = dominantSwatch?.population ?: 1
        var best: Swatch? = null
        var bestScore = 0f
        for (swatch in swatches) {
            if (!shouldBeScoredForTarget(swatch, target, used)) continue
            val score = generateScore(swatch, target, maxPopulation)
            if (best == null || score > bestScore) {
                best = swatch
                bestScore = score
            }
        }
        return best
    }

    private fun shouldBeScoredForTarget(
        swatch: Swatch,
        target: Target,
        used: Set<Int>,
    ): Boolean {
        val hsl = swatch.hsl
        return hsl[1] >= target.minimumSaturation && hsl[1] <= target.maximumSaturation &&
            hsl[2] >= target.minimumLightness && hsl[2] <= target.maximumLightness &&
            swatch.rgb !in used
    }

    private fun generateScore(
        swatch: Swatch,
        target: Target,
        maxPopulation: Int,
    ): Float {
        val hsl = swatch.hsl
        var score = 0f
        if (target.saturationWeight > 0) {
            score += target.saturationWeight * (1f - abs(hsl[1] - target.targetSaturation))
        }
        if (target.lightnessWeight > 0) {
            score += target.lightnessWeight * (1f - abs(hsl[2] - target.targetLightness))
        }
        if (target.populationWeight > 0) {
            score += target.populationWeight * (swatch.population / maxPopulation.toFloat())
        }
        return score
    }

    class Swatch(
        val rgb: Int,
        val population: Int,
    ) {
        val hsl: FloatArray by lazy { FloatArray(3).also { colorToHsl(rgb, it) } }

        private val textColors: Pair<Int, Int> by lazy { computeTextColors(rgb) }

        val titleTextColor: Int get() = textColors.first
        val bodyTextColor: Int get() = textColors.second

        override fun equals(other: Any?): Boolean = other is Swatch && other.rgb == rgb && other.population == population

        override fun hashCode(): Int = 31 * rgb + population

        override fun toString(): String = "Swatch(rgb=#${(rgb.toLong() and 0xFFFFFFFFL).toString(16)}, population=$population)"
    }

    class Target private constructor(
        val minimumSaturation: Float,
        val targetSaturation: Float,
        val maximumSaturation: Float,
        val minimumLightness: Float,
        val targetLightness: Float,
        val maximumLightness: Float,
        val isExclusive: Boolean = true,
    ) {
        var saturationWeight = 0.24f
            private set
        var lightnessWeight = 0.52f
            private set
        var populationWeight = 0.24f
            private set

        internal fun normalizeWeights() {
            val sum = saturationWeight + lightnessWeight + populationWeight
            if (sum != 0f) {
                saturationWeight /= sum
                lightnessWeight /= sum
                populationWeight /= sum
            }
        }

        companion object {
            val LIGHT_VIBRANT = Target(0.35f, 1f, 1f, 0.55f, 0.74f, 1f)
            val VIBRANT = Target(0.35f, 1f, 1f, 0.3f, 0.5f, 0.7f)
            val DARK_VIBRANT = Target(0.35f, 1f, 1f, 0f, 0.26f, 0.45f)
            val LIGHT_MUTED = Target(0f, 0.3f, 0.4f, 0.55f, 0.74f, 1f)
            val MUTED = Target(0f, 0.3f, 0.4f, 0.3f, 0.5f, 0.7f)
            val DARK_MUTED = Target(0f, 0.3f, 0.4f, 0f, 0.26f, 0.45f)

            internal val DEFAULTS
                get() = listOf(LIGHT_VIBRANT, VIBRANT, DARK_VIBRANT, LIGHT_MUTED, MUTED, DARK_MUTED)
        }
    }

    class Builder internal constructor(
        private val pixels: () -> Pair<IntArray, Int>,
    ) {
        private var maxColors = DEFAULT_CALCULATE_NUMBER_COLORS
        private var resizeArea = DEFAULT_RESIZE_BITMAP_AREA
        private var useDefaultFilter = true

        fun maximumColorCount(colors: Int): Builder = apply { maxColors = colors }

        fun resizeBitmapArea(area: Int): Builder = apply { resizeArea = area }

        fun clearFilters(): Builder = apply { useDefaultFilter = false }

        fun generate(): Palette {
            val (allPixels, width) = pixels()
            val sampled = scaleDown(allPixels, width, resizeArea)
            val swatches = ColorCutQuantizer(sampled, maxColors, useDefaultFilter).quantizedColors
            return Palette(swatches, Target.DEFAULTS).also { it.generate() }
        }
    }

    companion object {
        private const val DEFAULT_RESIZE_BITMAP_AREA = 112 * 112
        private const val DEFAULT_CALCULATE_NUMBER_COLORS = 16

        fun from(bitmap: ImageBitmap): Builder =
            Builder {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.readPixels(pixels)
                pixels to bitmap.width
            }

        /** Per chi ha già i pixel ARGB (es. da un'immagine scaricata). */
        fun from(
            pixels: IntArray,
            width: Int,
        ): Builder = Builder { pixels to width }

        fun from(swatches: List<Swatch>): Palette = Palette(swatches, Target.DEFAULTS).also { it.generate() }

        private fun scaleDown(
            pixels: IntArray,
            width: Int,
            area: Int,
        ): IntArray {
            if (width <= 0 || pixels.isEmpty()) return pixels
            val height = pixels.size / width
            val bitmapArea = width * height
            if (area <= 0 || bitmapArea <= area) return pixels
            val scale = sqrt(area / bitmapArea.toDouble())
            val newWidth = max(1, (width * scale).toInt())
            val newHeight = max(1, (height * scale).toInt())
            val out = IntArray(newWidth * newHeight)
            for (y in 0 until newHeight) {
                val sourceY = min(height - 1, (y / scale).toInt())
                for (x in 0 until newWidth) {
                    val sourceX = min(width - 1, (x / scale).toInt())
                    out[y * newWidth + x] = pixels[sourceY * width + sourceX]
                }
            }
            return out
        }
    }
}

// ---- ColorCutQuantizer ----------------------------------------------------------------

private const val QUANTIZE_WORD_WIDTH = 5
private const val QUANTIZE_WORD_MASK = (1 shl QUANTIZE_WORD_WIDTH) - 1
private const val COMPONENT_RED = -3
private const val COMPONENT_GREEN = -2
private const val COMPONENT_BLUE = -1

private class ColorCutQuantizer(
    pixels: IntArray,
    maxColors: Int,
    private val useDefaultFilter: Boolean,
) {
    private val histogram = IntArray(1 shl (QUANTIZE_WORD_WIDTH * 3))
    private val colors: IntArray
    val quantizedColors: List<Palette.Swatch>

    init {
        for (pixel in pixels) {
            if (AndroidColor.alpha(pixel) < 128) continue
            histogram[quantizeFromRgb888(pixel)]++
        }
        var distinctColorCount = 0
        for (color in histogram.indices) {
            if (histogram[color] > 0 && shouldIgnoreColor(approximateToRgb888(color))) histogram[color] = 0
            if (histogram[color] > 0) distinctColorCount++
        }
        colors = IntArray(distinctColorCount)
        var index = 0
        for (color in histogram.indices) {
            if (histogram[color] > 0) colors[index++] = color
        }
        quantizedColors =
            if (distinctColorCount <= maxColors) {
                colors.map { Palette.Swatch(approximateToRgb888(it), histogram[it]) }
            } else {
                quantizePixels(maxColors)
            }
    }

    private fun quantizePixels(maxColors: Int): List<Palette.Swatch> {
        val queue = mutableListOf(Vbox(0, colors.size - 1))
        while (queue.size < maxColors) {
            val box = queue.maxByOrNull { it.volume } ?: break
            if (!box.canSplit()) break
            queue.remove(box)
            queue += box.splitBox()
            queue += box
        }
        return queue.mapNotNull { box -> box.averageColor().takeUnless { shouldIgnoreColor(it.rgb) } }
    }

    private inner class Vbox(
        private val lowerIndex: Int,
        var upperIndex: Int,
    ) {
        private var population = 0
        private var minRed = 0
        private var maxRed = 0
        private var minGreen = 0
        private var maxGreen = 0
        private var minBlue = 0
        private var maxBlue = 0

        init {
            fitBox()
        }

        val volume: Int
            get() = (maxRed - minRed + 1) * (maxGreen - minGreen + 1) * (maxBlue - minBlue + 1)

        fun canSplit(): Boolean = upperIndex + 1 - lowerIndex > 1

        fun fitBox() {
            minRed = Int.MAX_VALUE
            minGreen = Int.MAX_VALUE
            minBlue = Int.MAX_VALUE
            maxRed = Int.MIN_VALUE
            maxGreen = Int.MIN_VALUE
            maxBlue = Int.MIN_VALUE
            population = 0
            for (i in lowerIndex..upperIndex) {
                val color = colors[i]
                population += histogram[color]
                val r = quantizedRed(color)
                val g = quantizedGreen(color)
                val b = quantizedBlue(color)
                if (r > maxRed) maxRed = r
                if (r < minRed) minRed = r
                if (g > maxGreen) maxGreen = g
                if (g < minGreen) minGreen = g
                if (b > maxBlue) maxBlue = b
                if (b < minBlue) minBlue = b
            }
        }

        fun splitBox(): Vbox {
            val splitPoint = findSplitPoint()
            val newBox = Vbox(splitPoint + 1, upperIndex)
            upperIndex = splitPoint
            fitBox()
            return newBox
        }

        private fun longestColorDimension(): Int {
            val redLength = maxRed - minRed
            val greenLength = maxGreen - minGreen
            val blueLength = maxBlue - minBlue
            return when {
                redLength >= greenLength && redLength >= blueLength -> COMPONENT_RED
                greenLength >= redLength && greenLength >= blueLength -> COMPONENT_GREEN
                else -> COMPONENT_BLUE
            }
        }

        private fun findSplitPoint(): Int {
            val dimension = longestColorDimension()
            val slice = colors.copyOfRange(lowerIndex, upperIndex + 1)
            val keyed = slice.sortedBy { modifySignificantOctet(dimension, it) }
            for (i in keyed.indices) colors[lowerIndex + i] = keyed[i]
            val midPoint = population / 2
            var count = 0
            for (i in lowerIndex..upperIndex) {
                count += histogram[colors[i]]
                if (count >= midPoint) return min(upperIndex - 1, i)
            }
            return lowerIndex
        }

        fun averageColor(): Palette.Swatch {
            var redSum = 0L
            var greenSum = 0L
            var blueSum = 0L
            var totalPopulation = 0
            for (i in lowerIndex..upperIndex) {
                val color = colors[i]
                val colorPopulation = histogram[color]
                totalPopulation += colorPopulation
                redSum += colorPopulation.toLong() * quantizedRed(color)
                greenSum += colorPopulation.toLong() * quantizedGreen(color)
                blueSum += colorPopulation.toLong() * quantizedBlue(color)
            }
            val total = max(1, totalPopulation)
            val redMean = (redSum / total.toFloat()).let { kotlin.math.round(it).toInt() }
            val greenMean = (greenSum / total.toFloat()).let { kotlin.math.round(it).toInt() }
            val blueMean = (blueSum / total.toFloat()).let { kotlin.math.round(it).toInt() }
            return Palette.Swatch(approximateToRgb888(redMean, greenMean, blueMean), totalPopulation)
        }
    }

    private fun shouldIgnoreColor(rgb: Int): Boolean {
        if (!useDefaultFilter) return false
        val hsl = FloatArray(3)
        colorToHsl(rgb, hsl)
        val isBlack = hsl[2] <= 0.05f
        val isWhite = hsl[2] >= 0.95f
        val isNearRedILine = hsl[0] in 10f..37f && hsl[1] <= 0.82f
        return isBlack || isWhite || isNearRedILine
    }
}

private fun modifySignificantOctet(
    dimension: Int,
    color: Int,
): Int {
    val r = quantizedRed(color)
    val g = quantizedGreen(color)
    val b = quantizedBlue(color)
    return when (dimension) {
        COMPONENT_GREEN -> (g shl (QUANTIZE_WORD_WIDTH * 2)) or (r shl QUANTIZE_WORD_WIDTH) or b
        COMPONENT_BLUE -> (b shl (QUANTIZE_WORD_WIDTH * 2)) or (g shl QUANTIZE_WORD_WIDTH) or r
        else -> color
    }
}

private fun quantizeFromRgb888(color: Int): Int {
    val r = modifyWordWidth(AndroidColor.red(color), 8, QUANTIZE_WORD_WIDTH)
    val g = modifyWordWidth(AndroidColor.green(color), 8, QUANTIZE_WORD_WIDTH)
    val b = modifyWordWidth(AndroidColor.blue(color), 8, QUANTIZE_WORD_WIDTH)
    return (r shl (QUANTIZE_WORD_WIDTH * 2)) or (g shl QUANTIZE_WORD_WIDTH) or b
}

private fun approximateToRgb888(
    r: Int,
    g: Int,
    b: Int,
): Int =
    AndroidColor.rgb(
        modifyWordWidth(r, QUANTIZE_WORD_WIDTH, 8),
        modifyWordWidth(g, QUANTIZE_WORD_WIDTH, 8),
        modifyWordWidth(b, QUANTIZE_WORD_WIDTH, 8),
    )

private fun approximateToRgb888(color: Int): Int = approximateToRgb888(quantizedRed(color), quantizedGreen(color), quantizedBlue(color))

private fun quantizedRed(color: Int): Int = (color shr (QUANTIZE_WORD_WIDTH * 2)) and QUANTIZE_WORD_MASK

private fun quantizedGreen(color: Int): Int = (color shr QUANTIZE_WORD_WIDTH) and QUANTIZE_WORD_MASK

private fun quantizedBlue(color: Int): Int = color and QUANTIZE_WORD_MASK

private fun modifyWordWidth(
    value: Int,
    currentWidth: Int,
    targetWidth: Int,
): Int {
    val newValue =
        if (targetWidth > currentWidth) {
            value shl (targetWidth - currentWidth)
        } else {
            value shr (currentWidth - targetWidth)
        }
    return newValue and ((1 shl targetWidth) - 1)
}

// ---- Colori: HSL e testo leggibile (da androidx.core.graphics.ColorUtils) ----------------

internal fun colorToHsl(
    color: Int,
    outHsl: FloatArray,
) {
    val rf = AndroidColor.red(color) / 255f
    val gf = AndroidColor.green(color) / 255f
    val bf = AndroidColor.blue(color) / 255f
    val maxValue = maxOf(rf, gf, bf)
    val minValue = minOf(rf, gf, bf)
    val deltaMaxMin = maxValue - minValue
    var h: Float
    val s: Float
    val l = (maxValue + minValue) / 2f
    if (maxValue == minValue) {
        h = 0f
        s = 0f
    } else {
        h =
            when (maxValue) {
                rf -> ((gf - bf) / deltaMaxMin) % 6f
                gf -> ((bf - rf) / deltaMaxMin) + 2f
                else -> ((rf - gf) / deltaMaxMin) + 4f
            }
        s = deltaMaxMin / (1f - abs(2f * l - 1f))
    }
    h = (h * 60f) % 360f
    if (h < 0) h += 360f
    outHsl[0] = h.coerceIn(0f, 360f)
    outHsl[1] = s.coerceIn(0f, 1f)
    outHsl[2] = l.coerceIn(0f, 1f)
}

private fun luminanceOf(color: Int): Double {
    fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c < 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(AndroidColor.red(color)) + 0.7152 * channel(AndroidColor.green(color)) + 0.0722 * channel(AndroidColor.blue(color))
}

private fun computeTextColors(background: Int): Pair<Int, Int> {
    val luminance = luminanceOf(background)
    val light = luminance < 0.5
    val base = if (light) AndroidColor.WHITE else AndroidColor.BLACK
    val titleAlpha = if (light) 0xB3 else 0xDE
    val bodyAlpha = if (light) 0x8A else 0x8A
    fun withAlpha(alpha: Int) = (base and 0x00FFFFFF) or (alpha shl 24)
    return withAlpha(titleAlpha) to withAlpha(bodyAlpha)
}
