package org.projectv.spike.js.ios

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class S2IosTest {

    @Test
    fun sourceReadPath() = runTest {
        val result = IosChecks(::httpGet).run(BASE_URL)
        println("S2_IOS_RESULT\n$result")

        assertTrue(result.contains("S2_IOS_CHECK search=PASS"), "search failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK details=PASS"), "details failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK pages=PASS"), "pages failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK kv=PASS"), "kv failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK html_search=PASS"), "html search failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK html_details=PASS"), "html details failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK html_pages=PASS"), "html pages failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK html_dom=PASS"), "html DOM failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK cookies=PASS"), "cookies failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK dispose=PASS"), "dispose failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK script_error=PASS"), "script error check failed:\n$result")
        assertTrue(result.contains("S2_IOS_CHECK timeout=PASS"), "timeout check failed:\n$result")
        assertTrue(result.contains("S2_IOS_DONE"), "checks did not finish:\n$result")
    }

    private companion object {
        const val BASE_URL = "http://127.0.0.1:18080"
    }
}
