package org.projectv.spike.js

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element

class HtmlDomHost(private val maxDocuments: Int = 16) {
    private data class DocumentEntry(
        val document: Document,
        val elementIds: MutableSet<Int> = mutableSetOf(),
    )

    private data class ElementEntry(val documentId: Int, val element: Element)

    private val documents = linkedMapOf<Int, DocumentEntry>()
    private val elements = mutableMapOf<Int, ElementEntry>()
    private var nextId = 1

    @Synchronized
    fun parse(html: String): Int {
        check(documents.size < maxDocuments) {
            "Too many open HtmlDocument instances (max $maxDocuments)"
        }
        val document = Ksoup.parse(html, "")
        val id = nextId++
        documents[id] = DocumentEntry(document)
        return id
    }

    @Synchronized
    fun dispose(docId: Int) {
        val entry = documents.remove(docId) ?: return
        entry.elementIds.forEach(elements::remove)
    }

    @Synchronized
    fun docQuery(docId: Int, selector: String): Int? {
        val entry = documents[docId] ?: return null
        val element = runCatching { entry.document.selectFirst(selector) }.getOrNull() ?: return null
        return register(docId, entry, element)
    }

    @Synchronized
    fun docQueryAll(docId: Int, selector: String): String {
        val entry = documents[docId] ?: return ""
        return runCatching { entry.document.select(selector).asList() }
            .getOrDefault(emptyList())
            .mapNotNull { register(docId, entry, it) }
            .joinToString(",")
    }

    @Synchronized
    fun docGetById(docId: Int, id: String): Int? {
        val entry = documents[docId] ?: return null
        val element = entry.document.getElementById(id) ?: return null
        return register(docId, entry, element)
    }

    @Synchronized
    fun elementQuery(elemId: Int, selector: String): Int? {
        val entry = elements[elemId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val element = runCatching { entry.element.selectFirst(selector) }.getOrNull() ?: return null
        return register(entry.documentId, docEntry, element)
    }

    @Synchronized
    fun elementQueryAll(elemId: Int, selector: String): String {
        val entry = elements[elemId] ?: return ""
        val docEntry = documents[entry.documentId] ?: return ""
        return runCatching { entry.element.select(selector).asList() }
            .getOrDefault(emptyList())
            .mapNotNull { register(entry.documentId, docEntry, it) }
            .joinToString(",")
    }

    @Synchronized
    fun text(elemId: Int): String = element(elemId)?.text().orEmpty()

    @Synchronized
    fun html(elemId: Int): String = element(elemId)?.html().orEmpty()

    @Synchronized
    fun attributes(elemId: Int): String {
        val element = element(elemId) ?: return "{}"
        return element.attributes().asList().joinToString(",", "{", "}") { attribute ->
            "\"${escape(attribute.key)}\":\"${escape(attribute.value)}\""
        }
    }

    @Synchronized
    fun classNames(elemId: Int): String =
        element(elemId)?.classNames()?.joinToString(" ").orEmpty()

    @Synchronized
    fun id(elemId: Int): String = element(elemId)?.id().orEmpty()

    @Synchronized
    fun localName(elemId: Int): String = element(elemId)?.normalName().orEmpty()

    @Synchronized
    fun children(elemId: Int): String {
        val entry = elements[elemId] ?: return ""
        val docEntry = documents[entry.documentId] ?: return ""
        return runCatching { entry.element.children().asList() }
            .getOrDefault(emptyList())
            .mapNotNull { register(entry.documentId, docEntry, it) }
            .joinToString(",")
    }

    @Synchronized
    fun parent(elemId: Int): Int? {
        val entry = elements[elemId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val parent = entry.element.parent() ?: return null
        if (parent is Document) return null
        return register(entry.documentId, docEntry, parent)
    }

    @Synchronized
    fun previousSibling(elemId: Int): Int? {
        val entry = elements[elemId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val sibling = entry.element.previousElementSibling() ?: return null
        return register(entry.documentId, docEntry, sibling)
    }

    @Synchronized
    fun nextSibling(elemId: Int): Int? {
        val entry = elements[elemId] ?: return null
        val docEntry = documents[entry.documentId] ?: return null
        val sibling = entry.element.nextElementSibling() ?: return null
        return register(entry.documentId, docEntry, sibling)
    }

    private fun element(elemId: Int): Element? {
        val entry = elements[elemId] ?: return null
        if (!documents.containsKey(entry.documentId)) return null
        return entry.element
    }

    private fun register(docId: Int, entry: DocumentEntry, element: Element): Int {
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
