package moe.rukamori.archivetune.compat

import platform.Foundation.*

actual interface Serializable

internal actual fun platformLog(
    level: String,
    tag: String,
    message: String,
) {
    // Niente NSLog: e' una funzione variadica e Kotlin/Native non converte la String in NSString
    // per gli argomenti "...", quindi l'app crashava al primo messaggio di log.
    println("$level/$tag: $message")
}

private fun Locale.toNs(): platform.Foundation.NSLocale = platform.Foundation.NSLocale(localeIdentifier = if (country.isBlank()) language else "${language}_$country")

internal actual fun platformDefaultLocale(): Locale {
    val preferred = platform.Foundation.NSLocale.preferredLanguages.firstOrNull() as? String
    val region = (platform.Foundation.NSLocale.currentLocale.objectForKey(platform.Foundation.NSLocaleCountryCode) as? String).orEmpty()
    val language = preferred?.substringBefore('-')?.lowercase() ?: "en"
    return Locale(language, region)
}

internal actual fun platformAvailableLocales(): Array<Locale> =
    platform.Foundation.NSLocale.availableLocaleIdentifiers
        .mapNotNull { it as? String }
        .map { Locale.forLanguageTag(it) }
        .toTypedArray()

internal actual fun displayLanguageOf(
    locale: Locale,
    inLocale: Locale?,
): String =
    (inLocale ?: platformDefaultLocale()).toNs().displayNameForKey(platform.Foundation.NSLocaleLanguageCode, locale.language)
        ?: locale.language

internal actual fun displayNameOf(
    locale: Locale,
    inLocale: Locale?,
): String =
    (inLocale ?: platformDefaultLocale()).toNs().displayNameForKey(platform.Foundation.NSLocaleIdentifier, locale.toString())
        ?: locale.toLanguageTag()

internal actual fun displayCountryOf(locale: Locale): String =
    platformDefaultLocale().toNs().displayNameForKey(platform.Foundation.NSLocaleCountryCode, locale.country) ?: locale.country

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun platformCompare(
    a: String,
    b: String,
    ignoreCaseAndAccents: Boolean,
): Int {
    val ns = a as platform.Foundation.NSString
    val result = if (ignoreCaseAndAccents) ns.localizedCaseInsensitiveCompare(b) else ns.localizedCompare(b)
    return result.toInt()
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun platformNormalize(
    value: String,
    form: Normalizer.Form,
): String {
    val ns = value as platform.Foundation.NSString
    return when (form) {
        Normalizer.Form.NFC -> ns.precomposedStringWithCanonicalMapping
        Normalizer.Form.NFD -> ns.decomposedStringWithCanonicalMapping
        Normalizer.Form.NFKC -> ns.precomposedStringWithCompatibilityMapping
        Normalizer.Form.NFKD -> ns.decomposedStringWithCompatibilityMapping
    }
}
