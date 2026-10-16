/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import moe.rukamori.archivetune.compat.AndroidColor
import moe.rukamori.archivetune.compat.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.toHct
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.rukamori.archivetune.constants.AppFontPreference
import kotlin.io.encoding.Base64
import kotlin.math.abs
import kotlin.math.min

val DefaultThemeColor = Color(0xFFED5564)
val LocalArchiveTuneFontPreference = staticCompositionLocalOf { AppFontPreference.DEFAULT }
val LocalArchiveTuneFontFamily = staticCompositionLocalOf { FontFamily.Default }

@Composable
fun rememberArchiveTuneLyricsFontFamily(): FontFamily {
    val fontPreference = LocalArchiveTuneFontPreference.current
    val fontFamily = LocalArchiveTuneFontFamily.current
    val lyricsFontFamily = LyricsFontFamily
    return remember(fontPreference, fontFamily, lyricsFontFamily) {
        if (fontPreference == AppFontPreference.DEFAULT) lyricsFontFamily else fontFamily
    }
}

data class ThemeSeedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArchiveTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    seedPalette: ThemeSeedPalette? = null,
    disableAnimations: Boolean = false,
    fontPreference: AppFontPreference = AppFontPreference.DEFAULT,
    customFontUri: String = "",
    content: @Composable () -> Unit,
) {
    // ArchiveTuneX: i colori di sistema (Material You) esistono solo su Android 12+.
    val systemDynamicColorScheme = systemDynamicColorScheme(darkTheme)
    val useSystemDynamicColor =
        (seedPalette == null && themeColor == DefaultThemeColor && systemDynamicColorScheme != null)

    val customFontFamily =
        produceState<FontFamily?>(
            initialValue = null,
            fontPreference,
            customFontUri,
        ) {
            value =
                if (fontPreference == AppFontPreference.CUSTOM && customFontUri.isNotBlank()) {
                    CustomFontLoader.loadFontFamily(customFontUri)
                } else {
                    null
                }
        }.value
    val appFontFamily = AppFontFamily
    val appTypography = AppTypography
    val resolvedFontFamily =
        remember(fontPreference, customFontFamily, appFontFamily) {
            when (fontPreference) {
                AppFontPreference.DEFAULT -> appFontFamily
                AppFontPreference.SYSTEM -> FontFamily.Default
                AppFontPreference.CUSTOM -> customFontFamily ?: appFontFamily
            }
        }
    val typography =
        remember(resolvedFontFamily, appTypography) {
            when (resolvedFontFamily) {
                appFontFamily -> appTypography
                FontFamily.Default -> SystemTypography
                else -> typographyFor(resolvedFontFamily)
            }
        }
    val motionScheme =
        remember(disableAnimations) {
            if (disableAnimations) DisabledMotionScheme else MotionScheme.expressive()
        }
    val paletteStyle =
        remember(themeColor, seedPalette) {
            paletteStyleFor(seedPalette?.primary ?: themeColor)
        }

    val appColorScheme =
        remember(seedPalette, themeColor, darkTheme) {
            if (seedPalette != null) {
                exactPaletteColorScheme(
                    palette = seedPalette,
                    isDark = darkTheme,
                )
            } else {
                materialKolorDynamicColorScheme(
                    keyColor = themeColor,
                    isDark = darkTheme,
                    style = paletteStyle,
                )
            }
        }

    val baseColorScheme =
        if (useSystemDynamicColor) {
            requireNotNull(systemDynamicColorScheme)
        } else {
            appColorScheme
        }

    val colorScheme =
        remember(baseColorScheme, pureBlack, darkTheme) {
            if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
        }

    val animatedColorScheme =
        if (disableAnimations) {
            colorScheme
        } else {
            animateColorScheme(
                targetColorScheme = colorScheme,
                animationSpec = motionScheme.defaultEffectsSpec(),
            )
        }

    val expressiveShapes =
        remember {
            Shapes(
                extraSmall =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(8.dp),
                small =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(12.dp),
                medium =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(16.dp),
                large =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(24.dp),
                extraLarge =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(32.dp),
            )
        }

    CompositionLocalProvider(
        LocalArchiveTuneFontPreference provides fontPreference,
        LocalArchiveTuneFontFamily provides resolvedFontFamily,
    ) {
        MaterialExpressiveTheme(
            colorScheme = animatedColorScheme,
            motionScheme = motionScheme,
            typography = typography,
            shapes = expressiveShapes,
            content = content,
        )
    }
}

@Composable
private fun animateColorScheme(
    targetColorScheme: ColorScheme,
    animationSpec: FiniteAnimationSpec<Color>,
): ColorScheme =
    ColorScheme(
        primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer =
            animateColorAsState(
                targetColorScheme.onSecondaryContainer,
                animationSpec,
                label = "onSecondaryContainer",
            ).value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer =
            animateColorAsState(
                targetColorScheme.onTertiaryContainer,
                animationSpec,
                label = "onTertiaryContainer",
            ).value,
        background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value,
        surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value,
        surfaceContainerLow =
            animateColorAsState(
                targetColorScheme.surfaceContainerLow,
                animationSpec,
                label = "surfaceContainerLow",
            ).value,
        surfaceContainerLowest =
            animateColorAsState(
                targetColorScheme.surfaceContainerLowest,
                animationSpec,
                label = "surfaceContainerLowest",
            ).value,
        surfaceContainerHigh =
            animateColorAsState(
                targetColorScheme.surfaceContainerHigh,
                animationSpec,
                label = "surfaceContainerHigh",
            ).value,
        surfaceContainerHighest =
            animateColorAsState(
                targetColorScheme.surfaceContainerHighest,
                animationSpec,
                label = "surfaceContainerHighest",
            ).value,
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object DisabledMotionScheme : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}

private fun exactPaletteColorScheme(
    palette: ThemeSeedPalette,
    isDark: Boolean,
): ColorScheme =
    mergedSeedColorScheme(
        primarySeed = palette.primary,
        secondarySeed = palette.secondary,
        tertiarySeed = palette.tertiary,
        neutralSeed = palette.neutral,
        isDark = isDark,
    )

private fun materialKolorDynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
    style: PaletteStyle,
): ColorScheme =
    mergedSeedColorScheme(
        primarySeed = keyColor,
        secondarySeed = keyColor,
        tertiarySeed = keyColor,
        neutralSeed = keyColor,
        isDark = isDark,
        contrastLevel = contrastLevel,
        style = style,
    )

private fun mergedSeedColorScheme(
    primarySeed: Color,
    secondarySeed: Color,
    tertiarySeed: Color,
    neutralSeed: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
    style: PaletteStyle = paletteStyleFor(primarySeed),
): ColorScheme {
    val primaryScheme = materialKolorScheme(primarySeed, isDark, contrastLevel, style)
    val secondaryScheme = materialKolorScheme(secondarySeed, isDark, contrastLevel, paletteStyleFor(secondarySeed))
    val tertiaryScheme = materialKolorScheme(tertiarySeed, isDark, contrastLevel, paletteStyleFor(tertiarySeed))
    val neutralScheme = materialKolorScheme(neutralSeed, isDark, contrastLevel, paletteStyleFor(neutralSeed))

    return ColorScheme(
        primary = primaryScheme.primary,
        onPrimary = primaryScheme.onPrimary,
        primaryContainer = primaryScheme.primaryContainer,
        onPrimaryContainer = primaryScheme.onPrimaryContainer,
        inversePrimary = primaryScheme.inversePrimary,
        secondary = secondaryScheme.primary,
        onSecondary = secondaryScheme.onPrimary,
        secondaryContainer = secondaryScheme.primaryContainer,
        onSecondaryContainer = secondaryScheme.onPrimaryContainer,
        tertiary = tertiaryScheme.primary,
        onTertiary = tertiaryScheme.onPrimary,
        tertiaryContainer = tertiaryScheme.primaryContainer,
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer,
        background = neutralScheme.background,
        onBackground = neutralScheme.onBackground,
        surface = neutralScheme.surface,
        onSurface = neutralScheme.onSurface,
        surfaceVariant = neutralScheme.surfaceVariant,
        onSurfaceVariant = neutralScheme.onSurfaceVariant,
        inverseSurface = neutralScheme.inverseSurface,
        inverseOnSurface = neutralScheme.inverseOnSurface,
        surfaceBright = neutralScheme.surfaceBright,
        surfaceDim = neutralScheme.surfaceDim,
        surfaceContainer = neutralScheme.surfaceContainer,
        surfaceContainerLow = neutralScheme.surfaceContainerLow,
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest,
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh,
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest,
        outline = neutralScheme.outline,
        outlineVariant = neutralScheme.outlineVariant,
        error = primaryScheme.error,
        onError = primaryScheme.onError,
        errorContainer = primaryScheme.errorContainer,
        onErrorContainer = primaryScheme.onErrorContainer,
        scrim = neutralScheme.scrim,
        surfaceTint = primaryScheme.surfaceTint,
    )
}

private fun materialKolorScheme(
    seedColor: Color,
    isDark: Boolean,
    contrastLevel: Double,
    style: PaletteStyle,
): ColorScheme =
    dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        contrastLevel = contrastLevel,
        style = style,
    )

private fun paletteStyleFor(seedColor: Color): PaletteStyle {
    val chroma = seedColor.toHct().chroma
    return when {
        chroma < 4.0 -> PaletteStyle.Monochrome
        chroma < 12.0 -> PaletteStyle.Neutral
        else -> PaletteStyle.TonalSpot
    }
}

private fun Int.toComposeColor(): Color = Color(this.toLong() and 0xFFFFFFFFL)

fun ImageBitmap.extractThemeColor(): Color {
    val palette =
        Palette
            .from(this)
            .maximumColorCount(16)
            .generate()

    val swatch =
        palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightMutedSwatch
            ?: palette.darkMutedSwatch

    return swatch?.rgb?.toComposeColor() ?: DefaultThemeColor
}

fun ImageBitmap.extractGradientColors(): List<Color> {
    val palette =
        Palette
            .from(this)
            .maximumColorCount(48)
            .generate()

    val swatches =
        palette.swatches
            .filter { it.population > 0 }
            .sortedByDescending { it.population }

    if (swatches.isEmpty()) {
        return listOf(Color(0xFF595959), Color(0xFF0D0D0D))
    }

    val first = swatches.first()
    val firstHsv = FloatArray(3)
    AndroidColor.colorToHSV(first.rgb, firstHsv)

    val second =
        swatches
            .drop(1)
            .maxByOrNull { candidate ->
                val hsv = FloatArray(3)
                AndroidColor.colorToHSV(candidate.rgb, hsv)

                val hueDiffRaw = abs(hsv[0] - firstHsv[0])
                val hueDiff = min(hueDiffRaw, 360f - hueDiffRaw) / 180f
                val satDiff = abs(hsv[1] - firstHsv[1])
                val valueDiff = abs(hsv[2] - firstHsv[2])

                hueDiff * 0.65f + satDiff * 0.2f + valueDiff * 0.15f
            }
            ?: first

    return listOf(first.rgb.toComposeColor(), second.rgb.toComposeColor())
        .sortedByDescending { it.luminance() }
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) {
        copy(
            surface = Color.Black,
            background = Color.Black,
        )
    } else {
        this
    }

val ColorSaver =
    object : Saver<Color, Int> {
        override fun restore(value: Int): Color = value.toComposeColor()

        override fun SaverScope.save(value: Color): Int = value.toArgb()
    }

@Serializable
data class ThemeExportV1(
    val version: Int = 1,
    val name: String? = null,
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val neutral: String,
)

object ThemeSeedPaletteCodec {
    private const val PreferencePrefix = "seedPalette:"
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

    fun encodeForPreference(
        palette: ThemeSeedPalette,
        name: String? = null,
    ): String {
        val payload =
            json.encodeToString(
                ThemeExportV1(
                    name = name,
                    primary = palette.primary.toHexArgbString(),
                    secondary = palette.secondary.toHexArgbString(),
                    tertiary = palette.tertiary.toHexArgbString(),
                    neutral = palette.neutral.toHexArgbString(),
                ),
            )
        val b64 = Base64.UrlSafe.encode(payload.encodeToByteArray())
        return PreferencePrefix + b64
    }

    fun decodeFromPreference(value: String): ThemeSeedPalette? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded =
            runCatching {
                val bytes = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(b64)
                bytes.decodeToString()
            }.getOrNull()
                ?: return null
        return decodeFromJson(decoded)
    }

    fun encodeAsJson(
        palette: ThemeSeedPalette,
        name: String? = null,
    ): String =
        json.encodeToString(
            ThemeExportV1(
                name = name,
                primary = palette.primary.toHexArgbString(),
                secondary = palette.secondary.toHexArgbString(),
                tertiary = palette.tertiary.toHexArgbString(),
                neutral = palette.neutral.toHexArgbString(),
            ),
        )

    fun decodeFromJson(text: String): ThemeSeedPalette? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element.jsonObject

            val version = obj["version"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            if (version != 1) return@runCatching null

            fun getColor(key: String): Color? =
                obj[key]
                    ?.jsonPrimitive
                    ?.content
                    ?.toColorOrNull()

            val primary = getColor("primary") ?: return@runCatching null
            val secondary = getColor("secondary") ?: primary
            val tertiary = getColor("tertiary") ?: primary
            val neutral = getColor("neutral") ?: primary

            ThemeSeedPalette(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                neutral = neutral,
            )
        }.getOrNull()
            ?: decodeFromLegacyObject(trimmed)
    }

    fun extractNameFromJsonOrNull(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val element: JsonElement = json.parseToJsonElement(trimmed)
            element.jsonObject["name"]
                ?.jsonPrimitive
                ?.content
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun extractNameFromPreference(value: String): String? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded =
            runCatching {
                val bytes = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(b64)
                bytes.decodeToString()
            }.getOrNull()
                ?: return null
        return extractNameFromJsonOrNull(decoded)
    }

    private fun decodeFromLegacyObject(text: String): ThemeSeedPalette? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element.jsonObject

            fun getHex(key: String): String? =
                obj[key]
                    ?.jsonPrimitive
                    ?.content
                    ?.takeIf { it.isNotBlank() }

            val primary = getHex("primary")?.toColorOrNull() ?: return@runCatching null
            val secondary = getHex("secondary")?.toColorOrNull() ?: primary
            val tertiary = getHex("tertiary")?.toColorOrNull() ?: primary
            val neutral = getHex("neutral")?.toColorOrNull() ?: primary

            ThemeSeedPalette(primary, secondary, tertiary, neutral)
        }.getOrNull()
    }

    private fun Color.toHexArgbString(): String = "#" + (this.toArgb().toLong() and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')

    private fun String.toColorOrNull(): Color? {
        val normalized = trim()
        if (normalized.isEmpty()) return null
        return runCatching {
            val withHash = if (normalized.startsWith("#")) normalized else "#$normalized"
            Color(AndroidColor.parseColor(withHash))
        }.getOrNull()
    }
}

/** Material You (colori dallo sfondo): solo Android 12+. Su iPhone restituisce null. */
@Composable
expect fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme?
