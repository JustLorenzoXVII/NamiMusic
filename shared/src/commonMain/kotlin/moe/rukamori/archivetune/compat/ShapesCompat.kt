package moe.rukamori.archivetune.compat

import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * Morph.toPath / RoundedPolygon.toPath di androidx.graphics.shapes restituiscono un android.graphics.Path
 * e quindi esistono solo su Android. Qui si costruisce direttamente un Path di Compose dalle curve.
 */
fun Morph.toComposePath(progress: Float): Path = asCubics(progress).toComposePath()

fun RoundedPolygon.toComposePath(): Path = cubics.toComposePath()

private fun List<Cubic>.toComposePath(): Path {
    val path = Path()
    val first = firstOrNull() ?: return path
    path.moveTo(first.anchor0X, first.anchor0Y)
    forEach { cubic ->
        path.cubicTo(
            cubic.control0X,
            cubic.control0Y,
            cubic.control1X,
            cubic.control1Y,
            cubic.anchor1X,
            cubic.anchor1Y,
        )
    }
    path.close()
    return path
}
