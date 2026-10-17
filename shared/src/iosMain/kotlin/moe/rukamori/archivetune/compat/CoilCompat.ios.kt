package moe.rukamori.archivetune.compat

import coil3.request.ImageRequest

internal actual fun platformAllowHardware(
    builder: ImageRequest.Builder,
    enable: Boolean,
): ImageRequest.Builder = builder
