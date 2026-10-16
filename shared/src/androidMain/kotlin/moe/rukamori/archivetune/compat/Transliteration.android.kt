package moe.rukamori.archivetune.compat

import com.atilika.kuromoji.ipadic.Tokenizer

private val transliterators = java.util.concurrent.ConcurrentHashMap<String, ThreadLocal<android.icu.text.Transliterator>>()

actual fun platformTransliterate(
    transform: String,
    text: String,
): String =
    transliterators
        .getOrPut(transform) { ThreadLocal.withInitial { android.icu.text.Transliterator.getInstance(transform) } }
        .get()!!
        .transliterate(text)

private val kuromojiTokenizer: Tokenizer by lazy { Tokenizer() }

actual fun platformTokenizeJapanese(text: String): List<JapaneseToken> =
    kuromojiTokenizer.tokenize(text).map { JapaneseToken(it.surface, it.reading) }
