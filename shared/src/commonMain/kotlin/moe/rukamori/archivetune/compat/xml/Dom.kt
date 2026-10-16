package moe.rukamori.archivetune.compat.xml

/**
 * Piccolo DOM XML (sottoinsieme di org.w3c.dom) con un parser in Kotlin, per i testi TTML.
 * Su Android l'originale usava DocumentBuilderFactory, che su iPhone non esiste.
 */
open class Node internal constructor(
    val nodeType: Short,
    open val nodeName: String,
) {
    var parentNode: Node? = null
        internal set
    open val nodeValue: String?
        get() = null
    open val localName: String?
        get() = null
    internal val children = mutableListOf<Node>()

    val childNodes: NodeList
        get() = NodeList(children)

    open val textContent: String
        get() = children.joinToString("") { it.textContent }

    val firstChild: Node?
        get() = children.firstOrNull()

    fun appendChild(child: Node): Node {
        child.parentNode?.children?.remove(child)
        child.parentNode = this
        children += child
        return child
    }

    fun removeChild(child: Node): Node {
        children.remove(child)
        child.parentNode = null
        return child
    }

    fun insertBefore(child: Node, ref: Node?): Node {
        child.parentNode?.children?.remove(child)
        child.parentNode = this
        val index = if (ref == null) -1 else children.indexOf(ref)
        if (index < 0) children += child else children.add(index, child)
        return child
    }

    companion object {
        const val ELEMENT_NODE: Short = 1
        const val ATTRIBUTE_NODE: Short = 2
        const val TEXT_NODE: Short = 3
        const val CDATA_SECTION_NODE: Short = 4
        const val COMMENT_NODE: Short = 8
        const val DOCUMENT_NODE: Short = 9
    }
}

class NodeList internal constructor(
    private val nodes: List<Node>,
) {
    val length: Int
        get() = nodes.size

    fun item(index: Int): Node = nodes[index]
}

class Attr internal constructor(
    name: String,
    private val value: String,
) : Node(ATTRIBUTE_NODE, name) {
    override val nodeValue: String
        get() = value
    override val localName: String
        get() = nodeName.substringAfter(':')
    override val textContent: String
        get() = value
    val name: String
        get() = nodeName
}

class NamedNodeMap internal constructor(
    private val attrs: List<Attr>,
) {
    val length: Int
        get() = attrs.size

    fun item(index: Int): Attr? = attrs.getOrNull(index)

    fun getNamedItem(name: String): Attr? = attrs.firstOrNull { it.nodeName == name }
}

class Element internal constructor(
    val tagName: String,
    attributes: List<Attr>,
) : Node(ELEMENT_NODE, tagName) {
    internal val attributeList: MutableList<Attr> = attributes.toMutableList()

    fun setAttribute(name: String, value: String) {
        attributeList.removeAll { it.nodeName == name }
        attributeList += Attr(name, value).also { it.parentNode = this }
    }

    fun removeAttribute(name: String) {
        attributeList.removeAll { it.nodeName == name }
    }

    override val localName: String
        get() = tagName.substringAfter(':')

    val attributes: NamedNodeMap
        get() = NamedNodeMap(attributeList)

    fun getAttribute(name: String): String = attributeList.firstOrNull { it.nodeName == name }?.nodeValue.orEmpty()

    fun hasAttribute(name: String): Boolean = attributeList.any { it.nodeName == name }

    fun getElementsByTagName(name: String): NodeList {
        val result = mutableListOf<Node>()

        fun visit(node: Node) {
            node.children.forEach { child ->
                if (child is Element && (name == "*" || child.tagName == name)) result += child
                visit(child)
            }
        }
        visit(this)
        return NodeList(result)
    }
}

class Text internal constructor(
    private val value: String,
    cdata: Boolean,
) : Node(if (cdata) CDATA_SECTION_NODE else TEXT_NODE, if (cdata) "#cdata-section" else "#text") {
    override val nodeValue: String
        get() = value
    override val textContent: String
        get() = value
}

class Document internal constructor() : Node(DOCUMENT_NODE, "#document") {
    fun createElement(name: String): Element = Element(name, emptyList())

    fun createTextNode(text: String): Text = Text(text, cdata = false)

    val documentElement: Element?
        get() = children.filterIsInstance<Element>().firstOrNull()

    fun getElementsByTagName(name: String): NodeList = documentElement?.let { root ->
        val all = mutableListOf<Node>()
        if (name == "*" || root.tagName == name) all += root
        val inner = root.getElementsByTagName(name)
        for (i in 0 until inner.length) all += inner.item(i)
        NodeList(all)
    } ?: NodeList(emptyList())
}

class XmlParseException(
    message: String,
) : Exception(message)

/** Parser XML semplice: elementi, attributi, testo, CDATA, commenti; entità standard e numeriche. */
object XmlParser {
    fun parse(source: String): Document {
        val document = Document()
        var current: Node = document
        var i = 0
        val text = StringBuilder()

        fun flushText() {
            if (text.isNotEmpty()) {
                if (current !is Document || text.isNotBlank()) {
                    val node = Text(decodeEntities(text.toString()), cdata = false)
                    node.parentNode = current
                    current.children += node
                }
                text.clear()
            }
        }
        while (i < source.length) {
            val c = source[i]
            if (c != '<') {
                text.append(c)
                i++
                continue
            }
            when {
                source.startsWith("<!--", i) -> {
                    flushText()
                    val end = source.indexOf("-->", i + 4)
                    i = if (end < 0) source.length else end + 3
                }
                source.startsWith("<![CDATA[", i) -> {
                    flushText()
                    val end = source.indexOf("]]>", i + 9)
                    val content = source.substring(i + 9, if (end < 0) source.length else end)
                    val node = Text(content, cdata = true)
                    node.parentNode = current
                    current.children += node
                    i = if (end < 0) source.length else end + 3
                }
                source.startsWith("<?", i) || source.startsWith("<!", i) -> {
                    flushText()
                    val end = source.indexOf('>', i)
                    i = if (end < 0) source.length else end + 1
                }
                source.startsWith("</", i) -> {
                    flushText()
                    val end = source.indexOf('>', i)
                    if (end < 0) throw XmlParseException("Tag di chiusura non terminato")
                    current = current.parentNode ?: throw XmlParseException("Tag di chiusura inatteso")
                    i = end + 1
                }
                else -> {
                    flushText()
                    var j = i + 1
                    var quote: Char? = null
                    while (j < source.length) {
                        val ch = source[j]
                        if (quote != null) {
                            if (ch == quote) quote = null
                        } else if (ch == '"' || ch == '\'') {
                            quote = ch
                        } else if (ch == '>') {
                            break
                        }
                        j++
                    }
                    if (j >= source.length) throw XmlParseException("Tag non terminato")
                    var inner = source.substring(i + 1, j)
                    val selfClosing = inner.endsWith("/")
                    if (selfClosing) inner = inner.dropLast(1)
                    val name = inner.takeWhile { !it.isWhitespace() }
                    if (name.isEmpty()) throw XmlParseException("Tag senza nome")
                    val attrs =
                        ATTRIBUTE_REGEX.findAll(inner.substring(name.length)).map { m ->
                            Attr(m.groupValues[1], decodeEntities(m.groupValues[3].ifEmpty { m.groupValues[4] }))
                        }.toList()
                    val element = Element(name, attrs)
                    attrs.forEach { it.parentNode = element }
                    element.parentNode = current
                    current.children += element
                    if (!selfClosing) current = element
                    i = j + 1
                }
            }
        }
        flushText()
        if (document.documentElement == null) throw XmlParseException("Documento vuoto")
        return document
    }

    private val ATTRIBUTE_REGEX = Regex("""([A-Za-z_:][\w.:-]*)\s*=\s*("([^"]*)"|'([^']*)')""")

    private fun decodeEntities(value: String): String {
        if ('&' !in value) return value
        return ENTITY_REGEX.replace(value) { m ->
            val entity = m.groupValues[1]
            when {
                entity == "amp" -> "&"
                entity == "lt" -> "<"
                entity == "gt" -> ">"
                entity == "quot" -> "\""
                entity == "apos" -> "'"
                entity == "nbsp" -> " "
                entity.startsWith("#x") || entity.startsWith("#X") -> entity.substring(2).toIntOrNull(16)?.let(::codePointToString) ?: m.value
                entity.startsWith("#") -> entity.substring(1).toIntOrNull()?.let(::codePointToString) ?: m.value
                else -> m.value
            }
        }
    }

    private val ENTITY_REGEX = Regex("&(#?[A-Za-z0-9]+);")

    private fun codePointToString(codePoint: Int): String =
        if (codePoint < 0x10000) {
            codePoint.toChar().toString()
        } else {
            val v = codePoint - 0x10000
            charArrayOf((0xD800 + (v shr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar()).concatToString()
        }
}

/** Riscrive il DOM come testo XML (al posto di javax.xml.transform). */
object XmlSerializer {
    fun serialize(
        document: Document,
        includeDeclaration: Boolean,
    ): String =
        buildString {
            if (includeDeclaration) append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            document.children.forEach { write(it) }
        }

    private fun StringBuilder.write(node: Node) {
        when (node) {
            is Element -> {
                append('<').append(node.tagName)
                node.attributeList.forEach { append(' ').append(it.nodeName).append("=\"").append(escape(it.nodeValue, attribute = true)).append('"') }
                if (node.children.isEmpty()) {
                    append("/>")
                } else {
                    append('>')
                    node.children.forEach { write(it) }
                    append("</").append(node.tagName).append('>')
                }
            }
            is Text ->
                if (node.nodeType == Node.CDATA_SECTION_NODE) {
                    append("<![CDATA[").append(node.nodeValue).append("]]>")
                } else {
                    append(escape(node.nodeValue, attribute = false))
                }
            else -> node.children.forEach { write(it) }
        }
    }

    private fun escape(
        value: String,
        attribute: Boolean,
    ): String {
        val sb = StringBuilder(value.length)
        for (c in value) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> if (attribute) sb.append("&quot;") else sb.append(c)
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}

/** android.util.Xml: serializzatore XML in streaming (come XmlSerializer di XmlPull). */
object Xml {
    fun newSerializer(): StreamingXmlSerializer = StreamingXmlSerializer()
}

class StreamingXmlSerializer internal constructor() {
    private var out: moe.rukamori.archivetune.compat.OutputStream? = null
    private val sb = StringBuilder()
    private var tagOpen = false

    fun setOutput(output: moe.rukamori.archivetune.compat.OutputStream, encoding: String?) {
        out = output
    }

    fun startDocument(encoding: String?, standalone: Boolean?) {
        sb.append("<?xml version='1.0' encoding='").append(encoding ?: "UTF-8").append("'")
        if (standalone != null) sb.append(" standalone='").append(if (standalone) "yes" else "no").append("'")
        sb.append(" ?>")
    }

    private fun closeOpenTag() {
        if (tagOpen) {
            sb.append('>')
            tagOpen = false
        }
    }

    fun startTag(namespace: String?, name: String): StreamingXmlSerializer {
        closeOpenTag()
        sb.append('<').append(name)
        tagOpen = true
        return this
    }

    fun attribute(namespace: String?, name: String, value: String): StreamingXmlSerializer {
        sb.append(' ').append(name).append("=\"").append(escape(value, true)).append('"')
        return this
    }

    fun text(text: String): StreamingXmlSerializer {
        closeOpenTag()
        sb.append(escape(text, false))
        return this
    }

    fun endTag(namespace: String?, name: String): StreamingXmlSerializer {
        if (tagOpen) {
            sb.append(" />")
            tagOpen = false
        } else {
            sb.append("</").append(name).append('>')
        }
        return this
    }

    fun endDocument() {
        closeOpenTag()
        flush()
    }

    fun flush() {
        val target = out ?: return
        if (sb.isNotEmpty()) {
            target.write(sb.toString().encodeToByteArray())
            sb.clear()
        }
        target.flush()
    }

    private fun escape(value: String, attribute: Boolean): String {
        val r = StringBuilder(value.length)
        for (c in value) {
            when (c) {
                '&' -> r.append("&amp;")
                '<' -> r.append("&lt;")
                '>' -> r.append("&gt;")
                '"' -> if (attribute) r.append("&quot;") else r.append(c)
                else -> r.append(c)
            }
        }
        return r.toString()
    }
}
