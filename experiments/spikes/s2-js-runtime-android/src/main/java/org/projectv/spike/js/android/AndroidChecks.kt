package org.projectv.spike.js.android

import android.content.Context
import android.os.Debug
import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URL

class AndroidChecks(private val context: Context) {

    suspend fun run(baseUrl: String): String {
        val log = StringBuilder()
        fun record(line: String) {
            Log.i(TAG, line)
            log.appendLine(line)
        }

        val script = buildString {
            append("globalThis.__BASE_URL = ").append(jsString(baseUrl)).append(";\n")
            append(readAsset("runtime/preamble.js")).append('\n')
            append(readAsset("runtime/demo_source.js")).append('\n')
            append(readAsset("runtime/html_source.js"))
        }
        val kv = mutableMapOf<String, String>()
        val cookies = SimpleCookieJar()
        val dom = HtmlDomHost()

        quickJs {
            evaluationTimeoutMillis = 10_000
            installHost(kv, cookies, dom, ::record)

            val coldStart = System.nanoTime()
            evaluate<Any?>(script)
            record("S2_ANDROID_COLD_START_MS ${(System.nanoTime() - coldStart) / 1_000_000}")

            val search = evaluate<String>("await __invoke('search', JSON.stringify({keyword:'demo'}))")
            record(
                "S2_ANDROID_CHECK search=${if (search.contains("Demo Comic")) "PASS" else "FAIL"} " +
                    "payload=${search.take(120)}",
            )

            val details = evaluate<String>("await __invoke('details', JSON.stringify({id:'demo'}))")
            record("S2_ANDROID_CHECK details=${if (details.contains("Chapter 1")) "PASS" else "FAIL"}")

            val pages = evaluate<String>("await __invoke('pages', JSON.stringify({comicId:'demo', epId:'1'}))")
            record("S2_ANDROID_CHECK pages=${if (pages.contains("/images/1.jpg")) "PASS" else "FAIL"}")

            val kvResult = evaluate<String>("await __invoke('kv', JSON.stringify({value:'abc'}))")
            record("S2_ANDROID_CHECK kv=${if (kvResult == "abc") "PASS" else "FAIL"}")

            val htmlSearch = evaluate<String>(
                "await __invoke('search', JSON.stringify({sourceKey:'html_demo', keyword:'demo'}))",
            )
            record(
                "S2_ANDROID_CHECK html_search=${if (htmlSearch.contains("Demo Comic")) "PASS" else "FAIL"} " +
                    "payload=${htmlSearch.take(120)}",
            )

            val htmlDetails = evaluate<String>(
                "await __invoke('details', JSON.stringify({sourceKey:'html_demo', id:'demo'}))",
            )
            record(
                "S2_ANDROID_CHECK html_details=${if (htmlDetails.contains("Chapter 1")) "PASS" else "FAIL"}",
            )

            val htmlPages = evaluate<String>(
                "await __invoke('pages', JSON.stringify({sourceKey:'html_demo', comicId:'demo', epId:'1'}))",
            )
            record(
                "S2_ANDROID_CHECK html_pages=${if (htmlPages.contains("/images/1.jpg")) "PASS" else "FAIL"}",
            )

            val htmlDom = evaluate<String>(
                """
                (() => {
                    const document = new HtmlDocument('<div id="root" class="a b"><span data-x="1">Hi</span></div>');
                    try {
                        const root = document.querySelector("#root");
                        const span = root.querySelector("span[data-x]");
                        return JSON.stringify({
                            classes: root.classNames,
                            spanText: span.text,
                            attr: span.attributes["data-x"],
                            localName: span.localName,
                            childCount: root.children.length,
                            parentId: span.parent.id
                        });
                    } finally {
                        document.dispose();
                    }
                })()
                """,
            )
            record(
                "S2_ANDROID_CHECK html_dom=${if (htmlDom.contains("\"spanText\":\"Hi\"")) "PASS" else "FAIL"} " +
                    "payload=$htmlDom",
            )

            val cookieResult = evaluate<String>(
                """
                await (async () => {
                    Network.setCookies(__BASE_URL, [new Cookie({name: 'sid', value: 'abc123'})]);
                    const first = JSON.parse((await Network.get(__BASE_URL + '/api/cookie-check')).body).cookie;
                    const listed = Network.getCookies(__BASE_URL);
                    Network.deleteCookies(__BASE_URL);
                    const second = JSON.parse((await Network.get(__BASE_URL + '/api/cookie-check')).body).cookie;
                    return JSON.stringify({first, count: listed.length, second});
                })()
                """,
            )
            record(
                "S2_ANDROID_CHECK cookies=" +
                    if (
                        cookieResult.contains("\"first\":\"sid=abc123\"") &&
                        cookieResult.contains("\"count\":1") &&
                        cookieResult.contains("\"second\":\"\"")
                    ) {
                        "PASS"
                    } else {
                        "FAIL"
                    } + " payload=$cookieResult",
            )

            val disposeResult = evaluate<String>(
                """
                (() => {
                    const document = new HtmlDocument('<a class="x">x</a>');
                    document.dispose();
                    try {
                        document.querySelector('a');
                        return 'NOT_DISPOSED';
                    } catch (error) {
                        return 'DISPOSED_OK';
                    }
                })()
                """,
            )
            record("S2_ANDROID_CHECK dispose=${if (disposeResult == "DISPOSED_OK") "PASS" else "FAIL"}")

            val concurrencyStart = System.nanoTime()
            val concurrency = evaluate<String>(
                """
                await (async () => {
                    await Promise.all([
                        Network.get(__BASE_URL + '/api/slow?ms=300'),
                        Network.get(__BASE_URL + '/api/slow?ms=300'),
                        Network.get(__BASE_URL + '/api/slow?ms=300'),
                        Network.get(__BASE_URL + '/api/slow?ms=300'),
                        Network.get(__BASE_URL + '/api/slow?ms=300')
                    ]);
                    return 'ok';
                })()
                """,
            )
            record(
                "S2_ANDROID_CONCURRENCY_MS ${(System.nanoTime() - concurrencyStart) / 1_000_000} " +
                    "ok=${concurrency == "ok"}",
            )

            val scriptError = runCatching {
                evaluate<Unit>("throw new Error('boom')")
            }.exceptionOrNull()
            record(
                "S2_ANDROID_CHECK script_error=${if (scriptError != null) "PASS" else "FAIL"} " +
                    "type=${scriptError?.javaClass?.simpleName}",
            )

            val timeout = runCatching {
                withTimeout(1_500) {
                    evaluate<Unit>("while (true) {}")
                }
            }
            record(
                "S2_ANDROID_CHECK timeout=${if (timeout.isFailure) "PASS" else "FAIL"} " +
                    "type=${timeout.exceptionOrNull()?.javaClass?.simpleName}",
            )
        }

        val churnStart = System.nanoTime()
        repeat(100) {
            quickJs {
                evaluate<Int>("1 + 1")
            }
        }
        record("S2_ANDROID_CHURN_100_MS ${(System.nanoTime() - churnStart) / 1_000_000}")
        record("S2_ANDROID_NATIVE_HEAP_MB ${Debug.getNativeHeapAllocatedSize() / 1024 / 1024}")
        record("S2_ANDROID_DONE")
        return log.toString()
    }

    private fun QuickJs.installHost(
        kv: MutableMap<String, String>,
        cookies: SimpleCookieJar,
        dom: HtmlDomHost,
        record: (String) -> Unit,
    ) {
        function<Unit>("__hostLog") { args -> record("[JS] ${args.joinToString(" ")}") }
        asyncFunction<String>("__hostHttpGet") { args ->
            httpGet(args.str(0), cookies)
        }
        function<String?>("__hostKvGet") { args -> kv[args.str(0)] }
        function<Unit>("__hostKvSet") { args ->
            val key = args.str(0)
            if (key.isNotEmpty()) {
                kv[key] = args.getOrNull(1)?.toString().orEmpty()
            }
        }

        function<Unit>("__hostSetCookie") { args ->
            cookies.set(args.str(0), args.str(1), args.str(2), args.str(3))
        }
        function<String>("__hostGetCookies") { args -> cookies.listJson(args.str(0)) }
        function<Unit>("__hostDeleteCookies") { args -> cookies.delete(args.str(0)) }

        function<Int>("__hostParseHtml") { args -> dom.parse(args.str(0)) }
        function<Int?>("__hostDocQuery") { args -> dom.docQuery(args.int(0), args.str(1)) }
        function<String>("__hostDocQueryAll") { args -> dom.docQueryAll(args.int(0), args.str(1)) }
        function<Int?>("__hostDocGetElementById") { args -> dom.docGetById(args.int(0), args.str(1)) }
        function<Int?>("__hostElementQuery") { args -> dom.elementQuery(args.int(0), args.str(1)) }
        function<String>("__hostElementQueryAll") { args -> dom.elementQueryAll(args.int(0), args.str(1)) }
        function<String>("__hostElementText") { args -> dom.text(args.int(0)) }
        function<String>("__hostElementHtml") { args -> dom.html(args.int(0)) }
        function<String>("__hostElementAttributes") { args -> dom.attributes(args.int(0)) }
        function<String>("__hostElementClassNames") { args -> dom.classNames(args.int(0)) }
        function<String>("__hostElementId") { args -> dom.id(args.int(0)) }
        function<String>("__hostElementLocalName") { args -> dom.localName(args.int(0)) }
        function<String>("__hostElementChildren") { args -> dom.children(args.int(0)) }
        function<Int?>("__hostElementParent") { args -> dom.parent(args.int(0)) }
        function<Int?>("__hostElementPreviousSibling") { args -> dom.previousSibling(args.int(0)) }
        function<Int?>("__hostElementNextSibling") { args -> dom.nextSibling(args.int(0)) }
        function<Unit>("__hostDisposeDoc") { args -> dom.dispose(args.int(0)) }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private suspend fun httpGet(url: String, cookies: SimpleCookieJar): String =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Connection", "close")
            cookies.header(url)?.let { connection.setRequestProperty("Cookie", it) }
            try {
                val code = connection.responseCode
                connection.headerFields.entries
                    .firstOrNull { it.key?.equals("Set-Cookie", ignoreCase = true) == true }
                    ?.value
                    ?.let { cookies.storeFromResponse(url, it) }
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("HTTP $code for $url")
                body
            } finally {
                connection.disconnect()
            }
        }

    private fun jsString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val TAG = "S2_ANDROID"
    }
}

private fun Array<Any?>.str(index: Int): String = this.getOrNull(index) as? String ?: ""

private fun Array<Any?>.int(index: Int): Int =
    (this.getOrNull(index) as? Number)?.toInt() ?: -1
