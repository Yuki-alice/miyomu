package app.miyomu.source.runtime

internal interface HtmlDomBridge {
    fun parse(html: String): Int
    fun dispose(docId: Int)
    fun docQuery(docId: Int, selector: String): Int?
    fun docQueryAll(docId: Int, selector: String): String
    fun docGetById(docId: Int, id: String): Int?
    fun elementQuery(elementId: Int, selector: String): Int?
    fun elementQueryAll(elementId: Int, selector: String): String
    fun text(elementId: Int): String
    fun html(elementId: Int): String
    fun attributes(elementId: Int): String
    fun classNames(elementId: Int): String
    fun id(elementId: Int): String
    fun localName(elementId: Int): String
    fun children(elementId: Int): String
    fun parent(elementId: Int): Int?
    fun previousSibling(elementId: Int): Int?
    fun nextSibling(elementId: Int): Int?
    fun close()
}

internal expect fun createHtmlDomBridge(maxDocuments: Int): HtmlDomBridge
