package app.miyomu.source.runtime

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element

internal actual fun createHtmlDomBridge(maxDocuments: Int): HtmlDomBridge =
    KsoupHtmlDomBridge(maxDocuments = maxDocuments)

internal class KsoupHtmlDomBridge(
    private val maxDocuments: Int = 16,
    private val maxElements: Int = 20_000,
) : HtmlDomBridge {

    private data class DocumentEntry(
        val document: Document,
        val elementIds: MutableSet<Int> = mutableSetOf(),
    )

    private data class ElementEntry(val documentId: Int, val element: Element)

    private val documents = linkedMapOf<Int, DocumentEntry>()
    private val elements = mutableMapOf<Int, ElementEntry>()
    private var nextId = 1

    @Synchronized
    override fun parse(html: String): Int {
        // Sources do not always call dispose() (Venera relies on GC); evicting the oldest
        // document keeps the cap without failing the whole source mid-request.
        if (documents.size >= maxDocuments) {
            documents.keys.firstOrNull()?.let(::dispose)
        }
        val document = Ksoup.parse(html, "")
        val id = nextId++
        documents[id] = DocumentEntry(document)
        return id
    }

    @Synchronized
    override fun dispose(docId: Int) {
        val entry = documents.remove(docId) ?: return
        entry.elementIds.forEach(elements::remove)
    }

    @Synchronized
    override fun docQuery(docId: Int, selector: String): Int? {
        val entry = documents[docId] ?: return null
        val element = runCatching { entry.document.selectFirst(selector) }.getOrNull() ?: return null
        return register(docId, entry, element)
    }

    @Synchronized
    override fun docQueryAll(docId: Int, selector: String): String {
        val entry = documents[docId] ?: return ""
        return runCatching { entry.document.select(selector).asList() }
            .getOrDefault(emptyList())
            .mapNotNull { register(docId, entry, it) }
            .joinToString(",")
    }

    @Synchronized
    override fun docGetById(docId: Int, id: String): Int? {
        val entry = documents[docId] ?: return null
        val element = entry.document.getElementById(id) ?: return null
        return register(docId, entry, element)
    }

    @Synchronized
    override fun elementQuery(elementId: Int, selector: String): Int? {
        val entry = elements[elementId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val element = runCatching { entry.element.selectFirst(selector) }.getOrNull() ?: return null
        return register(entry.documentId, docEntry, element)
    }

    @Synchronized
    override fun elementQueryAll(elementId: Int, selector: String): String {
        val entry = elements[elementId] ?: return ""
        val docEntry = documents[entry.documentId] ?: return ""
        return runCatching { entry.element.select(selector).asList() }
            .getOrDefault(emptyList())
            .mapNotNull { register(entry.documentId, docEntry, it) }
            .joinToString(",")
    }

    @Synchronized
    override fun text(elementId: Int): String = element(elementId)?.text().orEmpty()

    @Synchronized
    override fun html(elementId: Int): String = element(elementId)?.html().orEmpty()

    @Synchronized
    override fun attributes(elementId: Int): String {
        val element = element(elementId) ?: return "{}"
        return element.attributes().asList().joinToString(",", "{", "}") { attribute ->
            "\"${escape(attribute.key)}\":\"${escape(attribute.value)}\""
        }
    }

    @Synchronized
    override fun classNames(elementId: Int): String =
        element(elementId)?.classNames()?.joinToString(" ").orEmpty()

    @Synchronized
    override fun id(elementId: Int): String = element(elementId)?.id().orEmpty()

    @Synchronized
    override fun localName(elementId: Int): String = element(elementId)?.normalName().orEmpty()

    @Synchronized
    override fun children(elementId: Int): String {
        val entry = elements[elementId] ?: return ""
        val docEntry = documents[entry.documentId] ?: return ""
        return runCatching { entry.element.children().asList() }
            .getOrDefault(emptyList())
            .mapNotNull { register(entry.documentId, docEntry, it) }
            .joinToString(",")
    }

    @Synchronized
    override fun parent(elementId: Int): Int? {
        val entry = elements[elementId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val parent = entry.element.parent() ?: return null
        if (parent is Document) return null
        return register(entry.documentId, docEntry, parent)
    }

    @Synchronized
    override fun previousSibling(elementId: Int): Int? {
        val entry = elements[elementId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val sibling = entry.element.previousElementSibling() ?: return null
        return register(entry.documentId, docEntry, sibling)
    }

    @Synchronized
    override fun nextSibling(elementId: Int): Int? {
        val entry = elements[elementId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val sibling = entry.element.nextElementSibling() ?: return null
        return register(entry.documentId, docEntry, sibling)
    }

    @Synchronized
    override fun close() {
        documents.clear()
        elements.clear()
    }

    private fun element(elementId: Int): Element? {
        val entry = elements[elementId] ?: return null
        if (!documents.containsKey(entry.documentId)) return null
        return entry.element
    }

    private fun register(docId: Int, entry: DocumentEntry, element: Element): Int {
        check(elements.size < maxElements) {
            "Too many HtmlElement handles (max $maxElements)"
        }
        val id = nextId++
        elements[id] = ElementEntry(docId, element)
        entry.elementIds.add(id)
        return id
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
