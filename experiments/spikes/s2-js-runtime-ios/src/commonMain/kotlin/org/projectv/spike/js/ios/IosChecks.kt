package org.projectv.spike.js.ios

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.withTimeout
import kotlin.time.TimeSource

class IosChecks(
    private val httpGet: suspend (String, SimpleCookieJar) -> String,
) {

    suspend fun run(baseUrl: String): String {
        val log = StringBuilder()
        fun record(line: String) {
            println(line)
            log.appendLine(line)
        }

        val script = buildString {
            append("globalThis.__BASE_URL = ").append(jsString(baseUrl)).append(";\n")
            append(JsScripts.preamble).append('\n')
            append(JsScripts.demoSource).append('\n')
            append(JsScripts.htmlSource)
        }
        val kv = mutableMapOf<String, String>()
        val cookies = SimpleCookieJar()
        val dom = HtmlDomHost()

        quickJs {
            evaluationTimeoutMillis = 10_000
            installHost(kv, cookies, dom, ::record)

            val coldStart = TimeSource.Monotonic.markNow()
            evaluate<Any?>(script)
            record("S2_IOS_COLD_START_MS ${coldStart.elapsedNow().inWholeMilliseconds}")

            val search = evaluate<String>("await __invoke('search', JSON.stringify({keyword:'demo'}))")
            record("S2_IOS_CHECK search=${if (search.contains("Demo Comic")) "PASS" else "FAIL"} payload=${search.take(120)}")

            val details = evaluate<String>("await __invoke('details', JSON.stringify({id:'demo'}))")
            record("S2_IOS_CHECK details=${if (details.contains("Chapter 1")) "PASS" else "FAIL"}")

            val pages = evaluate<String>("await __invoke('pages', JSON.stringify({comicId:'demo', epId:'1'}))")
            record("S2_IOS_CHECK pages=${if (pages.contains("/images/1.jpg")) "PASS" else "FAIL"}")

            val kvResult = evaluate<String>("await __invoke('kv', JSON.stringify({value:'abc'}))")
            record("S2_IOS_CHECK kv=${if (kvResult == "abc") "PASS" else "FAIL"}")

            val htmlSearch = evaluate<String>(
                "await __invoke('search', JSON.stringify({sourceKey:'html_demo', keyword:'demo'}))",
            )
            record(
                "S2_IOS_CHECK html_search=${if (htmlSearch.contains("Demo Comic")) "PASS" else "FAIL"} " +
                    "payload=${htmlSearch.take(120)}",
            )

            val htmlDetails = evaluate<String>(
                "await __invoke('details', JSON.stringify({sourceKey:'html_demo', id:'demo'}))",
            )
            record("S2_IOS_CHECK html_details=${if (htmlDetails.contains("Chapter 1")) "PASS" else "FAIL"}")

            val htmlPages = evaluate<String>(
                "await __invoke('pages', JSON.stringify({sourceKey:'html_demo', comicId:'demo', epId:'1'}))",
            )
            record("S2_IOS_CHECK html_pages=${if (htmlPages.contains("/images/1.jpg")) "PASS" else "FAIL"}")

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
                "S2_IOS_CHECK html_dom=${if (htmlDom.contains("\"spanText\":\"Hi\"")) "PASS" else "FAIL"} " +
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
                "S2_IOS_CHECK cookies=" +
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
            record("S2_IOS_CHECK dispose=${if (disposeResult == "DISPOSED_OK") "PASS" else "FAIL"}")

            val concurrencyStart = TimeSource.Monotonic.markNow()
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
                "S2_IOS_CONCURRENCY_MS ${concurrencyStart.elapsedNow().inWholeMilliseconds} " +
                    "ok=${concurrency == "ok"}",
            )

            val scriptError = runCatching {
                evaluate<Unit>("throw new Error('boom')")
            }.exceptionOrNull()
            record(
                "S2_IOS_CHECK script_error=${if (scriptError != null) "PASS" else "FAIL"} " +
                    "type=${scriptError?.let { it::class.simpleName }}",
            )

            val timeout = runCatching {
                withTimeout(1_500) {
                    evaluate<Unit>("while (true) {}")
                }
            }
            record(
                "S2_IOS_CHECK timeout=${if (timeout.isFailure) "PASS" else "FAIL"} " +
                    "type=${timeout.exceptionOrNull()?.let { it::class.simpleName }}",
            )
        }

        val churnStart = TimeSource.Monotonic.markNow()
        repeat(100) {
            quickJs {
                evaluate<Int>("1 + 1")
            }
        }
        record("S2_IOS_CHURN_100_MS ${churnStart.elapsedNow().inWholeMilliseconds}")
        record("S2_IOS_DONE")
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

    private fun jsString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private fun Array<Any?>.str(index: Int): String = this.getOrNull(index) as? String ?: ""

private fun Array<Any?>.int(index: Int): Int =
    (this.getOrNull(index) as? Number)?.toInt() ?: -1
