package moe.rukamori.archivetune.compat

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.toBitmap

internal actual fun platformCoilImageToImageBitmap(image: coil3.Image): ImageBitmap = image.toBitmap().asComposeImageBitmap()
