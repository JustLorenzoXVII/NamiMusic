package moe.rukamori.archivetune.compat

import android.util.Log

actual typealias Serializable = java.io.Serializable

internal actual fun platformLog(
    level: String,
    tag: String,
    message: String,
) {
    when (level) {
        "V" -> Log.v(tag, message)
        "D" -> Log.d(tag, message)
        "I" -> Log.i(tag, message)
        "W" -> Log.w(tag, message)
        else -> Log.e(tag, message)
    }
}

// Come App.onCreate() dell'originale: log su logcat e nei Debug Logs dell'app.
internal actual fun plantPlatformLogTrees() {
    Timber.plant(Timber.DebugTree())
    try {
        Timber.plant(moe.rukamori.archivetune.utils.GlobalLogTree())
    } catch (_: Exception) {
    }
}

private fun Locale.toJava(): java.util.Locale = java.util.Locale.Builder().setLanguage(language).setRegion(country).build()

private fun java.util.Locale.toCompat(): Locale = Locale(language, country)

internal actual fun platformDefaultLocale(): Locale = java.util.Locale.getDefault().toCompat()

internal actual fun platformAvailableLocales(): Array<Locale> =
    java.util.Locale.getAvailableLocales().map { it.toCompat() }.toTypedArray()

internal actual fun displayLanguageOf(
    locale: Locale,
    inLocale: Locale?,
): String = runCatching { locale.toJava().getDisplayLanguage(inLocale?.toJava() ?: java.util.Locale.getDefault()) }.getOrDefault(locale.language)

internal actual fun displayNameOf(
    locale: Locale,
    inLocale: Locale?,
): String = runCatching { locale.toJava().getDisplayName(inLocale?.toJava() ?: java.util.Locale.getDefault()) }.getOrDefault(locale.toLanguageTag())

internal actual fun displayCountryOf(locale: Locale): String = runCatching { locale.toJava().displayCountry }.getOrDefault(locale.country)

private val primaryCollator: java.text.Collator by lazy {
    java.text.Collator.getInstance(java.util.Locale.getDefault()).apply { strength = java.text.Collator.PRIMARY }
}
private val tertiaryCollator: java.text.Collator by lazy { java.text.Collator.getInstance(java.util.Locale.getDefault()) }

internal actual fun platformCompare(
    a: String,
    b: String,
    ignoreCaseAndAccents: Boolean,
): Int = (if (ignoreCaseAndAccents) primaryCollator else tertiaryCollator).compare(a, b)

internal actual fun platformNormalize(
    value: String,
    form: Normalizer.Form,
): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.valueOf(form.name))
