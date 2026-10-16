/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ai

import moe.rukamori.archivetune.compat.xml.Document
import moe.rukamori.archivetune.compat.xml.Element
import moe.rukamori.archivetune.compat.xml.XmlParser
import moe.rukamori.archivetune.compat.xml.XmlSerializer

import moe.rukamori.archivetune.lyrics.LyricsUtils

data class AiLyricsSegment(
    val id: Int,
    val text: String,
)

sealed interface AiLyricsDocument {
    val formatName: String
    val segments: List<AiLyricsSegment>

    fun rebuild(translations: Map<Int, String>): String
}

object AiLyricsDocumentParser {
    fun parse(rawLyrics: String): AiLyricsDocument =
        if (LyricsUtils.isTtml(rawLyrics)) {
            parseTtml(rawLyrics).getOrElse { parseLineBased(rawLyrics, formatName = "TTML fallback") }
        } else {
            parseLineBased(
                rawLyrics = rawLyrics,
                formatName = if (rawLyrics.lineSequence().any { SyncedLineRegex.containsMatchIn(it) }) "synced LRC" else "plain text",
            )
        }

    private fun parseLineBased(
        rawLyrics: String,
        formatName: String,
    ): AiLyricsDocument {
        val lines = rawLyrics.split('\n')
        val templates = ArrayList<LineTemplate>(lines.size)
        val segments = ArrayList<AiLyricsSegment>()
        val seenSyncedLineKeys = HashSet<String>()
        lines.forEach { line ->
            val syncedMatch = SyncedLineRegex.matchEntire(line)
            if (syncedMatch != null) {
                val syncedLineKey = syncedMatch.groupValues[1].filterNot { it.isWhitespace() }
                if (!seenSyncedLineKeys.add(syncedLineKey)) return@forEach

                val content = syncedMatch.groupValues[3]
                val segmentId =
                    if (content.isBlank()) {
                        null
                    } else {
                        segments.size.also { segments.add(AiLyricsSegment(it, content.trim())) }
                    }
                templates.add(
                    LineTemplate(
                        prefix = syncedMatch.groupValues[1],
                        separator = syncedMatch.groupValues[2],
                        suffix = syncedMatch.groupValues[4],
                        content = content.trim(),
                        segmentId = segmentId,
                        original = line,
                    ),
                )
            } else {
                val plainMatch = PlainLineRegex.matchEntire(line)
                val content = plainMatch?.groupValues?.get(2).orEmpty()
                val segmentId =
                    if (content.isBlank()) {
                        null
                    } else {
                        segments.size.also { segments.add(AiLyricsSegment(it, content.trim())) }
                    }
                templates.add(
                    LineTemplate(
                        prefix = plainMatch?.groupValues?.get(1).orEmpty(),
                        separator = "",
                        suffix = plainMatch?.groupValues?.get(3).orEmpty(),
                        content = content.trim(),
                        segmentId = segmentId,
                        original = line,
                    ),
                )
            }
        }
        return LineBasedLyricsDocument(formatName = formatName, templates = templates, segments = segments)
    }

    private fun parseTtml(rawLyrics: String): Result<AiLyricsDocument> =
        runCatching {
            val document = XmlParser.parse(rawLyrics)
            val paragraphs = ArrayList<TtmlParagraph>()
            val segments = ArrayList<AiLyricsSegment>()
            val elements = document.getElementsByTagName("*")
            for (index in 0 until elements.length) {
                val element = elements.item(index) as? Element ?: continue
                if (!element.tagName.endsWith("p", ignoreCase = true)) continue
                val text = element.textContent?.trim().orEmpty()
                if (text.isBlank()) continue
                val segmentId = segments.size
                val lineKey =
                    readAttributeBySuffix(element, "key")
                        ?: "archivetune-translation-$segmentId".also { key -> element.setAttribute("key", key) }
                segments.add(AiLyricsSegment(segmentId, text))
                paragraphs.add(TtmlParagraph(element = element, segmentId = segmentId, key = lineKey))
            }
            require(segments.isNotEmpty()) { "TTML has no translatable text" }
            TtmlLyricsDocument(
                document = document,
                originalHadDeclaration = rawLyrics.trimStart().startsWith("<?xml", ignoreCase = true),
                paragraphs = paragraphs,
                segments = segments,
            )
        }

    private fun readAttributeBySuffix(
        element: Element,
        suffix: String,
    ): String? {
        val directValue = element.getAttribute(suffix).takeIf { it.isNotBlank() }
        if (directValue != null) return directValue

        val attrs = element.attributes ?: return null
        for (i in 0 until attrs.length) {
            val node = attrs.item(i) ?: continue
            val name = node.nodeName ?: continue
            if (name.equals(suffix, ignoreCase = true) || name.endsWith(":$suffix", ignoreCase = true)) {
                val value = node.nodeValue?.trim()
                if (!value.isNullOrEmpty()) return value
            }
        }
        return null
    }

    private val SyncedLineRegex = Regex("""^(\s*(?:\[[^\]]+])+)(\s*)(.*?)(\s*)$""")
    private val PlainLineRegex = Regex("""^(\s*)(.*?)(\s*)$""")
}

private data class LineTemplate(
    val prefix: String,
    val separator: String,
    val suffix: String,
    val content: String,
    val segmentId: Int?,
    val original: String,
)

private data class LineBasedLyricsDocument(
    override val formatName: String,
    private val templates: List<LineTemplate>,
    override val segments: List<AiLyricsSegment>,
) : AiLyricsDocument {
    override fun rebuild(translations: Map<Int, String>): String =
        templates.joinToString("\n") { template ->
            val id = template.segmentId ?: return@joinToString template.original
            val translated = translations[id]?.trim().orEmpty()
            if (translated.isBlank() || translated.equals(template.content, ignoreCase = true)) {
                template.original
            } else {
                val translatedLine = "${template.prefix}${template.separator}$translated${template.suffix}"
                "${template.original}\n$translatedLine"
            }
        }
}

private data class TtmlParagraph(
    val element: Element,
    val segmentId: Int,
    val key: String,
)

private data class TtmlLyricsDocument(
    private val document: Document,
    private val originalHadDeclaration: Boolean,
    private val paragraphs: List<TtmlParagraph>,
    override val segments: List<AiLyricsSegment>,
) : AiLyricsDocument {
    override val formatName: String = "TTML"

    override fun rebuild(translations: Map<Int, String>): String {
        removeArchiveTuneTranslationElements(document.documentElement)

        val translatedParagraphs =
            paragraphs.mapNotNull { paragraph ->
                val translated =
                    translations[paragraph.segmentId]
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank() &&
                                !it.equals(
                                    paragraph.element.textContent
                                        ?.trim()
                                        .orEmpty(),
                                    ignoreCase = true,
                                )
                        }
                        ?: return@mapNotNull null
                paragraph to translated
            }

        if (translatedParagraphs.isNotEmpty()) {
            val metadata = findOrCreateMetadataElement(document, document.documentElement)
            val translationElement =
                document.createElement("translation").apply {
                    setAttribute("data-archivetune", "translation")
                }

            translatedParagraphs.forEach { (paragraph, translated) ->
                val textElement =
                    document.createElement("text").apply {
                        setAttribute("for", paragraph.key)
                        appendChild(document.createTextNode(translated))
                    }
                translationElement.appendChild(textElement)
            }

            metadata.appendChild(translationElement)
        }
        return transform(document, originalHadDeclaration)
    }

    private fun removeArchiveTuneTranslationElements(root: Element) {
        val elements = root.getElementsByTagName("*")
        val removableElements = ArrayList<Element>()
        for (index in 0 until elements.length) {
            val element = elements.item(index) as? Element ?: continue
            if (element.tagName.endsWith("translation", ignoreCase = true) &&
                element.getAttribute("data-archivetune") == "translation"
            ) {
                removableElements.add(element)
            }
        }
        removableElements.forEach { element ->
            element.parentNode?.removeChild(element)
        }
    }

    private fun findOrCreateMetadataElement(
        document: Document,
        root: Element,
    ): Element {
        val elements = root.getElementsByTagName("*")
        for (index in 0 until elements.length) {
            val element = elements.item(index) as? Element ?: continue
            if (element.tagName.endsWith("metadata", ignoreCase = true)) return element
        }

        val metadata = document.createElement("metadata")
        val head = findOrCreateHeadElement(document, root)
        head.appendChild(metadata)
        return metadata
    }

    private fun findOrCreateHeadElement(
        document: Document,
        root: Element,
    ): Element {
        val elements = root.getElementsByTagName("*")
        for (index in 0 until elements.length) {
            val element = elements.item(index) as? Element ?: continue
            if (element.tagName.endsWith("head", ignoreCase = true)) return element
        }

        val head = document.createElement("head")
        root.insertBefore(head, root.firstChild)
        return head
    }

    private fun transform(
        document: Document,
        includeDeclaration: Boolean,
    ): String {
        return XmlSerializer.serialize(document, includeDeclaration)
    }
}
