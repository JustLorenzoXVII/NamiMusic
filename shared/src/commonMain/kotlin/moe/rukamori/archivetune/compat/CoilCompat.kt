package moe.rukamori.archivetune.compat

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest

/**
 * Le estensioni di Coil che esistono solo su Android (Context.imageLoader, allowHardware),
 * in versione comune: su iPhone usano l'ImageLoader condiviso e ignorano le bitmap "hardware".
 */
val PlatformContext.imageLoader: ImageLoader
    get() = SingletonImageLoader.get(this)

fun ImageRequest.Builder.allowHardware(enable: Boolean): ImageRequest.Builder = platformAllowHardware(this, enable)

internal expect fun platformAllowHardware(
    builder: ImageRequest.Builder,
    enable: Boolean,
): ImageRequest.Builder
