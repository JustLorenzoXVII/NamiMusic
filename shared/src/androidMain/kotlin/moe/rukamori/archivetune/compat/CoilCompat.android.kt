package moe.rukamori.archivetune.compat

import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun platformAllowHardware(
    builder: ImageRequest.Builder,
    enable: Boolean,
): ImageRequest.Builder = builder.allowHardware(enable)
