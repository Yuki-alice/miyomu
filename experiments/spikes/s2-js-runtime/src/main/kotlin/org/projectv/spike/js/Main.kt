package org.projectv.spike.js

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val http: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .build()

fun main() = runBlocking {
    val server = FixtureServer()
    println("S2_FIXTURE_BASE ${server.baseUrl}")
    try {
        runChecks(server.baseUrl)
    } finally {
        server.close()
    }
}

private suspend fun runChecks(baseUrl: String) {
    val script = buildString {
        append("globalThis.__BASE_URL = ").append(jsString(baseUrl)).append(";\n")
        append(resource("/runtime/preamble.js")).append('\n')
        append(resource("/runtime/demo_source.js")).append('\n')
        append(resource("/runtime/html_source.js"))
    }
    val kv = mutableMapOf<String, String>()
    val cookieJar = SimpleCookieJar()
    val dom = HtmlDomHost()

    val coldStart = System.nanoTime()
    quickJs {
        evaluationTimeoutMillis = 10_000
        installHost(kv, cookieJar, dom)
        evaluate<Any?>(script)
        println("S2_COLD_START_MS ${(System.nanoTime() - coldStart) / 1_000_000}")

        val search = evaluate<String>("await __invoke('search', JSON.stringify({keyword:'demo'}))")
        check(search.contains("Demo Comic")) { "search result missing title: $search" }
        println("S2_CHECK search=PASS payload=${search.take(160)}")

        val details = evaluate<String>("await __invoke('details', JSON.stringify({id:'demo'}))")
        check(details.contains("Chapter 1")) { "details missing chapter: $details" }
        println("S2_CHECK details=PASS payload=${details.take(160)}")

        val pages = evaluate<String>("await __invoke('pages', JSON.stringify({comicId:'demo', epId:'1'}))")
        check(pages.contains("/images/1.jpg")) { "pages missing image: $pages" }
        println("S2_CHECK pages=PASS payload=$pages")

        val kvResult = evaluate<String>("await __invoke('kv', JSON.stringify({value:'abc'}))")
        check(kvResult == "abc") { "kv mismatch: $kvResult" }
        println("S2_CHECK kv=PASS")

        val htmlSearch = evaluate<String>(
            "await __invoke('search', JSON.stringify({sourceKey:'html_demo', keyword:'demo'}))",
        )
        check(htmlSearch.contains("Demo Comic")) { "html search missing title: $htmlSearch" }
        println("S2_CHECK html_search=PASS payload=${htmlSearch.take(160)}")

        val htmlDetails = evaluate<String>(
            "await __invoke('details', JSON.stringify({sourceKey:'html_demo', id:'demo'}))",
        )
        check(htmlDetails.contains("Chapter 1")) { "html details missing chapter: $htmlDetails" }
        println("S2_CHECK html_details=PASS payload=${htmlDetails.take(160)}")

        val htmlPages = evaluate<String>(
            "await __invoke('pages', JSON.stringify({sourceKey:'html_demo', comicId:'demo', epId:'1'}))",
        )
        check(htmlPages.contains("/images/1.jpg")) { "html pages missing image: $htmlPages" }
        println("S2_CHECK html_pages=PASS payload=$htmlPages")

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
        check(htmlDom.contains("\"spanText\":\"Hi\"")) { "html DOM mismatch: $htmlDom" }
        println("S2_CHECK html_dom=PASS payload=$htmlDom")

        val cookies = evaluate<String>(
            """
            await (async () => {
                Network.setCookies(__BASE_URL, [new Cookie({name: 'sid', value: 'abc123', domain: '127.0.0.1'})]);
                const first = JSON.parse((await Network.get(__BASE_URL + '/api/cookie-check')).body).cookie;
                const listed = Network.getCookies(__BASE_URL);
                Network.deleteCookies(__BASE_URL);
                const second = JSON.parse((await Network.get(__BASE_URL + '/api/cookie-check')).body).cookie;
                return JSON.stringify({first, count: listed.length, second});
            })()
            """,
        )
        check(cookies.contains("\"first\":\"sid=abc123\"")) { "cookie not sent: $cookies" }
        check(cookies.contains("\"count\":1")) { "cookie listing failed: $cookies" }
        check(cookies.contains("\"second\":\"\"")) { "cookie delete failed: $cookies" }
        println("S2_CHECK cookies=PASS payload=$cookies")

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
        check(disposeResult == "DISPOSED_OK") { "dispose check failed: $disposeResult" }
        println("S2_CHECK dispose=PASS")

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
        check(concurrency == "ok") { "concurrency failed: $concurrency" }
        println("S2_CONCURRENCY_MS ${(System.nanoTime() - concurrencyStart) / 1_000_000}")

        val scriptError = runCatching {
            evaluate<Unit>("throw new Error('boom')")
        }.exceptionOrNull()
        println(
            "S2_CHECK script_error=${if (scriptError != null) "PASS" else "FAIL"} " +
                "type=${scriptError?.javaClass?.simpleName}",
        )

        val timeoutStart = System.nanoTime()
        val timeout = runCatching {
            withTimeout(1_500) {
                evaluate<Unit>("while (true) {}")
            }
        }
        val elapsed = (System.nanoTime() - timeoutStart) / 1_000_000
        println(
            "S2_CHECK timeout=${if (timeout.isFailure) "PASS" else "FAIL"} " +
                "elapsed_ms=$elapsed type=${timeout.exceptionOrNull()?.javaClass?.simpleName}",
        )
    }

    val churnStart = System.nanoTime()
    repeat(100) {
        quickJs {
            evaluate<Int>("1 + 1")
        }
    }
    val usedMb = Runtime.getRuntime().let { (it.totalMemory() - it.freeMemory()) / 1024 / 1024 }
    println("S2_CHURN_100_MS ${(System.nanoTime() - churnStart) / 1_000_000}")
    println("S2_MEMORY_USED_MB $usedMb")
}

private fun QuickJs.installHost(
    kv: MutableMap<String, String>,
    cookies: SimpleCookieJar,
    dom: HtmlDomHost,
) {
    function<Unit>("__hostLog") { args ->
        println("[JS] ${args.joinToString(" ")}")
    }
    asyncFunction<String>("__hostHttpGet") { args ->
        val url = args.str(0)
        httpGet(url, cookies)
    }
    function<String?>("__hostKvGet") { args ->
        kv[args.str(0)]
    }
    function<Unit>("__hostKvSet") { args ->
        val key = args.str(0)
        if (key.isNotEmpty()) {
            kv[key] = args.getOrNull(1)?.toString().orEmpty()
        }
    }

    function<Unit>("__hostSetCookie") { args ->
        cookies.set(args.str(0), args.str(1), args.str(2), args.str(3))
    }
    function<String>("__hostGetCookies") { args ->
        cookies.listJson(args.str(0))
    }
    function<Unit>("__hostDeleteCookies") { args ->
        cookies.delete(args.str(0))
    }

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

private fun Array<Any?>.str(index: Int): String = this.getOrNull(index) as? String ?: ""

private fun Array<Any?>.int(index: Int): Int =
    (this.getOrNull(index) as? Number)?.toInt() ?: -1

private suspend fun httpGet(url: String, cookies: SimpleCookieJar): String = withContext(Dispatchers.IO) {
    val builder = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(10))
        .GET()
    cookies.header(url)?.let { builder.header("Cookie", it) }
    val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    cookies.storeFromResponse(url, response.headers().allValues("Set-Cookie"))
    if (response.statusCode() !in 200..299) {
        error("HTTP ${response.statusCode()} for $url")
    }
    response.body()
}

private fun resource(path: String): String =
    ResourceAnchor::class.java.getResourceAsStream(path)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Missing resource: $path")

private fun jsString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private object ResourceAnchor
