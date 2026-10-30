package app.miyomu.source.runtime

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KsoupHtmlDomBridgeTest {

    @Test
    fun evictsTheOldestDocumentAtCapacity() {
        val bridge = KsoupHtmlDomBridge(maxDocuments = 2)
        try {
            val first = bridge.parse("<div id='first'>first</div>")
            val second = bridge.parse("<div id='second'>second</div>")
            assertNotNull(bridge.docQuery(first, "#first"))

            val third = bridge.parse("<div id='third'>third</div>")

            assertNull(bridge.docQuery(first, "#first"))
            assertNotNull(bridge.docQuery(second, "#second"))
            assertNotNull(bridge.docQuery(third, "#third"))
        } finally {
            bridge.close()
        }
    }
}
