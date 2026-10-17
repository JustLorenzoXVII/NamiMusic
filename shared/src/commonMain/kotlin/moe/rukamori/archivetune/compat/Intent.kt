package moe.rukamori.archivetune.compat

/**
 * Sostituto multipiattaforma di android.content.Intent per i casi usati dall'interfaccia:
 * aprire un link (ACTION_VIEW) e condividere testo o file (ACTION_SEND).
 * Su Android diventa un Intent vero; su iPhone apre Safari o il foglio di condivisione.
 */
class Intent() {
    /** Intent(context, Classe::class): in ArchiveTuneX indica solo la schermata/servizio di destinazione. */
    constructor(
        @Suppress("UNUSED_PARAMETER") context: Context,
        target: kotlin.reflect.KClass<*>,
    ) : this() {
        targetClassName = target.simpleName
    }

    var targetClassName: String? = null

    var action: String? = null
    var data: Uri? = null
    var type: String? = null
    var flags: Int = 0
    var `package`: String? = null
    val extras: Bundle = Bundle()
    val categories = mutableSetOf<String>()

    /** Se l'Intent è un selettore (createChooser), l'Intent da condividere. */
    var chooserTarget: Intent? = null
        internal set
    var chooserTitle: CharSequence? = null
        internal set

    constructor(action: String?) : this() {
        this.action = action
    }

    constructor(action: String?, uri: Uri?) : this() {
        this.action = action
        this.data = uri
    }

    fun setAction(action: String?): Intent = apply { this.action = action }

    fun setData(uri: Uri?): Intent = apply { this.data = uri }

    fun setType(type: String?): Intent = apply { this.type = type }

    fun setDataAndType(uri: Uri?, type: String?): Intent = apply {
        this.data = uri
        this.type = type
    }

    fun setPackage(packageName: String?): Intent = apply { this.`package` = packageName }

    fun addFlags(flags: Int): Intent = apply { this.flags = this.flags or flags }

    fun setFlags(flags: Int): Intent = apply { this.flags = flags }

    fun addCategory(category: String): Intent = apply { categories += category }

    /** Su iPhone non si può sapere in anticipo quali app aprono un link: si considera sempre apribile. */
    fun resolveActivity(
        @Suppress("UNUSED_PARAMETER") packageManager: PackageManager,
    ): ComponentName? = ComponentName(Context.PACKAGE_NAME, "external")

    fun putExtra(name: String, value: String?): Intent = apply { extras.putString(name, value) }

    fun putExtra(name: String, value: CharSequence?): Intent = apply { extras.putString(name, value?.toString()) }

    fun putExtra(name: String, value: Boolean): Intent = apply { extras.putBoolean(name, value) }

    fun putExtra(name: String, value: Int): Intent = apply { extras.putInt(name, value) }

    fun putExtra(name: String, value: Long): Intent = apply { extras.putLong(name, value) }

    fun putExtra(name: String, value: Uri?): Intent = apply { extras.putString(name, value?.toString()) }

    fun getStringExtra(name: String): String? = extras.getString(name)

    fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean = extras.getBoolean(name, defaultValue)

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE"
        const val ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH"
        const val ACTION_CHOOSER = "android.intent.action.CHOOSER"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_TITLE = "android.intent.extra.TITLE"
        const val CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE"
        const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
        const val CATEGORY_DEFAULT = "android.intent.category.DEFAULT"
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001
        const val FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002
        const val URI_INTENT_SCHEME = 1

        fun createChooser(target: Intent, title: CharSequence?): Intent =
            Intent(ACTION_CHOOSER).apply {
                chooserTarget = target
                chooserTitle = title
            }
    }
}

/** Come Context.startActivity: apre il link o la condivisione sulla piattaforma. */
fun Context.startActivity(intent: Intent) {
    platformStartActivity(intent)
}

internal expect fun platformStartActivity(intent: Intent)

/** Come android.os.Build: su iPhone SDK_INT vale 0, così i rami solo-Android non vengono eseguiti. */
object Build {
    object VERSION {
        val SDK_INT: Int
            get() = platformSdkInt()
        val RELEASE: String
            get() = platformOsVersion()
    }

    object VERSION_CODES {
        const val M = 23
        const val N = 24
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val S_V2 = 32
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
        const val VANILLA_ICE_CREAM = 35
        const val BAKLAVA = 36
    }

    val MANUFACTURER: String
        get() = platformManufacturer()
    val MODEL: String
        get() = platformModel()
    val BRAND: String
        get() = platformManufacturer()
    val DEVICE: String
        get() = platformModel()
}

internal expect fun platformSdkInt(): Int

internal expect fun platformOsVersion(): String

internal expect fun platformManufacturer(): String

internal expect fun platformModel(): String

/** Come android.os.SystemClock. */
object SystemClock {
    private val start = kotlin.time.TimeSource.Monotonic.markNow()

    fun elapsedRealtime(): Long = start.elapsedNow().inWholeMilliseconds

    fun uptimeMillis(): Long = start.elapsedNow().inWholeMilliseconds

    fun elapsedRealtimeNanos(): Long = start.elapsedNow().inWholeNanoseconds
}


/** android.content.ComponentName. */
data class ComponentName(
    val packageName: String,
    val className: String,
)
