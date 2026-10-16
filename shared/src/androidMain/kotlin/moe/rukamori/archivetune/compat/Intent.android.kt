package moe.rukamori.archivetune.compat

import com.justlorenzo.archivetunex.player.AndroidAppContext

/** Converte l'Intent comune in un android.content.Intent vero. */
fun Intent.toAndroidIntent(): android.content.Intent {
    chooserTarget?.let { target ->
        return android.content.Intent.createChooser(target.toAndroidIntent(), chooserTitle)
    }
    val intent = android.content.Intent(action)
    val uri = data?.let { android.net.Uri.parse(it.toString()) }
    when {
        uri != null && type != null -> intent.setDataAndType(uri, type)
        uri != null -> intent.data = uri
        type != null -> intent.type = type
    }
    `package`?.let { intent.setPackage(it) }
    categories.forEach { intent.addCategory(it) }
    intent.addFlags(flags)
    extras.keySet().forEach { key ->
        when (val value = extras.get(key)) {
            is Boolean -> intent.putExtra(key, value)
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is String ->
                if (key == Intent.EXTRA_STREAM) {
                    intent.putExtra(key, android.net.Uri.parse(value))
                } else {
                    intent.putExtra(key, value)
                }
            null -> Unit
            else -> intent.putExtra(key, value.toString())
        }
    }
    return intent
}

internal actual fun platformStartActivity(intent: Intent) {
    val context = AndroidAppContext.context
    val androidIntent = intent.toAndroidIntent().addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(androidIntent) }
        .onFailure { Timber.e(it, "startActivity non riuscito") }
}

internal actual fun platformSdkInt(): Int = android.os.Build.VERSION.SDK_INT

internal actual fun platformOsVersion(): String = android.os.Build.VERSION.RELEASE

internal actual fun platformManufacturer(): String = android.os.Build.MANUFACTURER

internal actual fun platformModel(): String = android.os.Build.MODEL
