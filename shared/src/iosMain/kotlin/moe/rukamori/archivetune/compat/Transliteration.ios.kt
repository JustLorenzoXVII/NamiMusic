package moe.rukamori.archivetune.compat

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFStringTokenizerAdvanceToNextToken
import platform.CoreFoundation.CFStringTokenizerCopyCurrentTokenAttribute
import platform.CoreFoundation.CFStringTokenizerCreate
import platform.CoreFoundation.CFStringTokenizerGetCurrentTokenRange
import platform.CoreFoundation.CFLocaleCreate
import platform.CoreFoundation.kCFStringTokenizerAttributeLatinTranscription
import platform.CoreFoundation.kCFStringTokenizerTokenNone
import platform.CoreFoundation.kCFStringTokenizerUnitWord
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSString
import platform.Foundation.stringByApplyingTransform
import kotlinx.cinterop.useContents

@Suppress("CAST_NEVER_SUCCEEDS")
actual fun platformTransliterate(
    transform: String,
    text: String,
): String = (text as NSString).stringByApplyingTransform(transform, reverse = false) ?: text

@OptIn(ExperimentalForeignApi::class)
@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
actual fun platformTokenizeJapanese(text: String): List<JapaneseToken> {
    if (text.isEmpty()) return emptyList()
    val cfText = CFBridgingRetain(text as NSString) as CFStringRef?
    val localeId = CFBridgingRetain("ja" as NSString) as CFStringRef?
    val locale = CFLocaleCreate(null, localeId)
    val tokenizer = CFStringTokenizerCreate(null, cfText, CFRangeMake(0, text.length.toLong()), kCFStringTokenizerUnitWord, locale)
    val result = mutableListOf<JapaneseToken>()
    try {
        while (CFStringTokenizerAdvanceToNextToken(tokenizer) != kCFStringTokenizerTokenNone) {
            val range = CFStringTokenizerGetCurrentTokenRange(tokenizer)
            val (start, length) = range.useContents { location.toInt() to this.length.toInt() }
            val surface = text.substring(start, (start + length).coerceAtMost(text.length))
            val latin = CFBridgingRelease(CFStringTokenizerCopyCurrentTokenAttribute(tokenizer, kCFStringTokenizerAttributeLatinTranscription)) as? String
            val reading = latin?.let { platformTransliterate("Latin-Katakana", it) }
            result += JapaneseToken(surface, reading)
        }
    } finally {
        CFRelease(tokenizer)
        CFRelease(locale)
        CFRelease(localeId)
        CFRelease(cfText)
    }
    return result
}
