package com.justlorenzo.archivetunex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import archivetunex.shared.generated.resources.Res
import archivetunex.shared.generated.resources.google_sans
import archivetunex.shared.generated.resources.google_sans_italic
import org.jetbrains.compose.resources.Font

/**
 * Google Sans (font variabile): un solo file per tutti i pesi, dal normale al nero.
 * Ogni peso usato nell'app punta allo stesso file con il suo valore di "wght".
 */
val GoogleSans: FontFamily
    @Composable get() {
        val weights =
            listOf(
                FontWeight.Light,
                FontWeight.Normal,
                FontWeight.Medium,
                FontWeight.SemiBold,
                FontWeight.Bold,
                FontWeight.ExtraBold,
                FontWeight.Black,
            )
        val fonts =
            weights.map { Font(Res.font.google_sans, weight = it, style = FontStyle.Normal) } +
                weights.map { Font(Res.font.google_sans_italic, weight = it, style = FontStyle.Italic) }
        return FontFamily(fonts)
    }

/** Tipografia di Nami: quella di Material 3 con Google Sans e il corpo del testo a 16sp (come Fuji). */
@Composable
fun atxTypography(systemFont: Boolean = false): Typography {
    // Tema iOS: font di sistema (SF Pro su iPhone)
    val family = if (systemFont) FontFamily.Default else GoogleSans
    return remember(family) {
        val base = Typography()
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = family),
            displayMedium = base.displayMedium.copy(fontFamily = family),
            displaySmall = base.displaySmall.copy(fontFamily = family),
            headlineLarge = base.headlineLarge.copy(fontFamily = family),
            headlineMedium = base.headlineMedium.copy(fontFamily = family),
            headlineSmall = base.headlineSmall.copy(fontFamily = family),
            titleLarge = base.titleLarge.copy(fontFamily = family),
            titleMedium = base.titleMedium.copy(fontFamily = family),
            titleSmall = base.titleSmall.copy(fontFamily = family),
            bodyLarge =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp,
                ),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            bodySmall = base.bodySmall.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family),
            labelMedium = base.labelMedium.copy(fontFamily = family),
            labelSmall = base.labelSmall.copy(fontFamily = family),
        )
    }
}

/**
 * Tipografia del tema iOS: font di sistema (SF Pro) con la scala dei testi di Apple
 * (Large Title 34, Title 28/22/20, Headline 17, Body 17, Subheadline 15, Footnote 13, Caption 12/11).
 */
fun iosTypography(): Typography {
    val f = FontFamily.Default
    fun style(
        size: Int,
        line: Int,
        weight: FontWeight,
        tracking: Double,
    ) = TextStyle(fontFamily = f, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)
    return Typography(
        displayLarge = style(40, 48, FontWeight.Bold, 0.37),
        displayMedium = style(36, 43, FontWeight.Bold, 0.37),
        displaySmall = style(34, 41, FontWeight.Bold, 0.37),
        headlineLarge = style(28, 34, FontWeight.Bold, 0.36),
        headlineMedium = style(28, 34, FontWeight.Bold, 0.36),
        headlineSmall = style(22, 28, FontWeight.Bold, -0.26),
        titleLarge = style(20, 25, FontWeight.SemiBold, -0.45),
        titleMedium = style(17, 22, FontWeight.SemiBold, -0.43),
        titleSmall = style(15, 20, FontWeight.SemiBold, -0.23),
        bodyLarge = style(17, 22, FontWeight.Normal, -0.43),
        bodyMedium = style(15, 20, FontWeight.Normal, -0.23),
        bodySmall = style(13, 18, FontWeight.Normal, -0.08),
        labelLarge = style(15, 20, FontWeight.SemiBold, -0.23),
        labelMedium = style(12, 16, FontWeight.Medium, 0.0),
        labelSmall = style(11, 13, FontWeight.Medium, 0.06),
    )
}
