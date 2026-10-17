package moe.rukamori.archivetune.compat

import moe.rukamori.archivetune.BuildConfig

/** android.Manifest.permission (solo i nomi). */
object Manifest {
    object permission {
        const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
        const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
        const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
        const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    }
}

class PackageInfo internal constructor(
    val packageName: String,
    val versionName: String,
    val longVersionCode: Long,
) {
    @Suppress("unused")
    val versionCode: Int
        get() = longVersionCode.toInt()
}

/** android.content.pm.PackageManager (informazioni sull'app e permessi). */
class PackageManager internal constructor() {
    fun getPackageInfo(packageName: String, flags: Int): PackageInfo = PackageInfo(packageName, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())

    fun getPackageInfo(packageName: String, flags: PackageInfoFlags): PackageInfo = getPackageInfo(packageName, 0)

    fun canRequestPackageInstalls(): Boolean = false

    fun getLaunchIntentForPackage(packageName: String): Intent? = null

    fun hasSystemFeature(name: String): Boolean = false

    fun queryIntentActivities(
        @Suppress("UNUSED_PARAMETER") intent: Intent,
        @Suppress("UNUSED_PARAMETER") flags: Int,
    ): List<ResolveInfo> = emptyList()

    class PackageInfoFlags private constructor(val value: Long) {
        companion object {
            fun of(value: Long): PackageInfoFlags = PackageInfoFlags(value)
        }
    }

    companion object {
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
        const val MATCH_DEFAULT_ONLY = 0x00010000
        const val MATCH_ALL = 0x00020000
    }
}

private val sharedPackageManager = PackageManager()

val Context.packageManager: PackageManager
    get() = sharedPackageManager

/** androidx.core.content.ContextCompat (permessi). */
object ContextCompat {
    fun checkSelfPermission(context: Context, permission: String): Int =
        if (platformHasPermission(permission)) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
}

/** Su Android controlla il permesso vero; su iPhone i permessi Android non esistono (concessi). */
internal expect fun platformHasPermission(permission: String): Boolean

/** android.provider.Settings (azioni per aprire le impostazioni di sistema). */
object Settings {
    const val ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ACTION_APP_OPEN_BY_DEFAULT_SETTINGS = "android.settings.APP_OPEN_BY_DEFAULT_SETTINGS"
    const val ACTION_MANAGE_UNKNOWN_APP_SOURCES = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
    const val ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
    const val EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE"

    object System {
        val CONTENT_URI: Uri = Uri.parse("content://settings/system")
    }
}

class ActivityNotFoundException(
    message: String? = null,
) : RuntimeException(message)


/** android.content.pm.ResolveInfo: le app che possono aprire un Intent (non disponibile su iPhone). */
class ResolveInfo(
    val activityInfo: ActivityInfo?,
) {
    class ActivityInfo(
        val packageName: String,
        val name: String,
        val applicationInfo: ApplicationInfo? = null,
    )

    class ApplicationInfo(
        private val label: String,
    ) {
        fun loadLabel(
            @Suppress("UNUSED_PARAMETER") packageManager: PackageManager,
        ): CharSequence = label
    }
}
