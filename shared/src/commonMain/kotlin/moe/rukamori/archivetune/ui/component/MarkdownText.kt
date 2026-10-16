/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * Testo Markdown (note di rilascio, descrizioni). L'originale usava Markwon in una TextView Android;
 * qui un piccolo renderer Compose: titoli, elenchi, grassetto, corsivo, codice, barrato e link.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val blocks = remember(markdown) { parseBlocks(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val headingStyle =
                        when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                    Text(inline(block.text, linkColor), style = headingStyle, color = color, fontWeight = FontWeight.Bold)
                }

                is MdBlock.Bullet ->
                    Row(Modifier.padding(start = (block.indent * 12).dp)) {
                        Text(block.marker, style = style, color = color, modifier = Modifier.padding(end = 6.dp))
                        Text(inline(block.text, linkColor), style = style, color = color)
                    }

                is MdBlock.Paragraph -> Text(inline(block.text, linkColor), style = style, color = color)

                MdBlock.Rule -> Text("", style = style.copy(fontSize = 0.5.em))
            }
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock

    data class Bullet(val marker: String, val text: String, val indent: Int) : MdBlock

    data class Paragraph(val text: String) : MdBlock

    data object Rule : MdBlock
}

private fun parseBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flush() {
        if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }
    markdown.replace("\r\n", "\n").lines().forEach { raw ->
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        val indent = (line.length - trimmed.length) / 2
        val heading = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
        val bullet = Regex("^([-*+])\\s+(\\[[ xX]]\\s+)?(.*)$").find(trimmed)
        val numbered = Regex("^(\\d+[.)])\\s+(.*)$").find(trimmed)
        when {
            trimmed.isEmpty() -> flush()
            trimmed.matches(Regex("^([-*_])\\1{2,}$")) -> {
                flush()
                blocks += MdBlock.Rule
            }
            heading != null -> {
                flush()
                blocks += MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2])
            }
            bullet != null -> {
                flush()
                val check = bullet.groupValues[2]
                val marker = if (check.isBlank()) "•" else if (check.contains('x', ignoreCase = true)) "☑" else "☐"
                blocks += MdBlock.Bullet(marker, bullet.groupValues[3], indent)
            }
            numbered != null -> {
                flush()
                blocks += MdBlock.Bullet(numbered.groupValues[1], numbered.groupValues[2], indent)
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed.replace(Regex("<br\\s*/?>"), "\n"))
            }
        }
    }
    flush()
    return blocks
}

private val inlinePattern =
    Regex("(\\*\\*|__)(.+?)\\1|(\\*|_)(.+?)\\3|`([^`]+)`|~~(.+?)~~|\\[([^\\]]+)]\\(([^)\\s]+)[^)]*\\)|(https?://[^\\s)]+)")

private fun inline(
    text: String,
    linkColor: Color,
): AnnotatedString =
    buildAnnotatedString {
        appendInline(text, linkColor)
    }

private fun AnnotatedString.Builder.appendInline(
    text: String,
    linkColor: Color,
) {
    var last = 0
    for (match in inlinePattern.findAll(text)) {
        append(text.substring(last, match.range.first))
        val g = match.groupValues
        when {
            g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(g[2], linkColor) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(g[4], linkColor) }
            g[5].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(g[5]) }
            g[6].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { appendInline(g[6], linkColor) }
            g[7].isNotEmpty() ->
                withLink(LinkAnnotation.Url(g[8], TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))) {
                    append(g[7])
                }
            g[9].isNotEmpty() ->
                withLink(LinkAnnotation.Url(g[9], TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))) {
                    append(g[9])
                }
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}
