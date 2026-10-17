package moe.rukamori.archivetune.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.util.Locale

actual fun shareLocalAudio(
    context: moe.rukamori.archivetune.compat.Context,
    mediaId: String,
    mimeType: String?,
): Boolean {
    @Suppress("NAME_SHADOWING")
    val context: Context = com.justlorenzo.archivetunex.player.AndroidAppContext.context
    val uri = mediaId.toUri()
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "content" && scheme != "android.resource") return false

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType?.takeIf(String::isNotBlank) ?: "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(shareIntent, null))
    return true
}
