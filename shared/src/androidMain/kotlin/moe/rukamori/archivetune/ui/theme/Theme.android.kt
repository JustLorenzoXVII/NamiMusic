/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

fun Bitmap.extractThemeColor(): Color = asImageBitmap().extractThemeColor()

fun Bitmap.extractGradientColors(): List<Color> = asImageBitmap().extractGradientColors()

fun extractWallpaperThemeColor(context: Context): Color? {
    return try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val drawable = wallpaperManager.drawable ?: return null
        val bitmap =
            if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp =
                    Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888,
                    )
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
        bitmap.extractThemeColor()
    } catch (e: Exception) {
        null
    }
}
