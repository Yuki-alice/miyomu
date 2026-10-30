package app.miyomu.source.novel

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Evaluates the legado rule dialect against an HTML element or a JSON document. This is an
 * independent implementation of the documented rule grammar (no legado code), covering the
 * subset that real community sources overwhelmingly use:
 *
 * - `a || b` — first non-empty alternative wins
 * - `a && b` — results joined with newlines
 * - `<js>…</js>` / `@js:` — whole-rule JavaScript with a `result`/`baseUrl` binding
 * - `{{expr}}` — inline JavaScript interpolation
 * - CSS selectors with a trailing `@text|@ownText|@html|@textNodes|@attr` extractor
 *   (`@css:` makes the prefix explicit)
 * - `@json:` / `$.` paths — a small JSONPath subset (`$.a.b[0]`, `$.list[*].name`)
 * - `##regex##replacement` — trailing regex replace
 *
 * `||`/`&&`/`##` are only recognized outside `{{}}` and `<js>` blocks.
 */
class LegadoRuleEvaluator(private val js: LegadoJs) {

    suspend fun string(rule: String?, element: Element?, baseUrl: String): String? =
        rule?.let { evalRule(it, element = element, jsonText = null, baseUrl = baseUrl, current = null) }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    suspend fun string(rule: String?, jsonText: String, baseUrl: String): String? =
        rule?.let { evalRule(it, element = null, jsonText = jsonText, baseUrl = baseUrl, current = null) }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** Evaluates a list rule over an HTML context; each match keeps its [Element]. */
    suspend fun list(rule: String?, element: Element): List<Element> {
        rule ?: return emptyList()
        val head = splitTopLevel(rule, "||").firstOrNull()?.trim() ?: return emptyList()
        val (selector, _) = splitAttr(head.removePrefix("@css:"))
        if (selector.isBlank()) return emptyList()
        return element.select(selector).toList()
    }

    /** Evaluates a JSON list rule; each match is the serialized node. */
    suspend fun jsonList(rule: String?, jsonText: String): List<String> {
        rule ?: return emptyList()
        val head = splitTopLevel(rule, "||").firstOrNull()?.trim() ?: return emptyList()
        val path = head.removePrefix("@json:")
        return evalJsonPathList(path, jsonText)
    }

    /** Field-level evaluation inside one list item (HTML element or serialized JSON node). */
    suspend fun itemString(rule: String?, element: Element?, jsonText: String?, baseUrl: String): String? =
        rule?.let { evalRule(it, element = element, jsonText = jsonText, baseUrl = baseUrl, current = null) }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private suspend fun evalRule(
        raw: String,
        element: Element?,
        jsonText: String?,
        baseUrl: String,
        current: String?,
    ): String? {
        var rule = raw.trim()
        if (rule.isEmpty()) return null

        if (rule.startsWith("<js>") && rule.endsWith("</js>")) {
            return js.eval(rule.substring(4, rule.length - 5), bindings(current, baseUrl))
        }
        if (rule.startsWith("@js:")) {
            return js.eval(rule.removePrefix("@js:"), bindings(current, baseUrl))
        }

        val (body, replaceRegex, replacement) = splitReplace(rule)

        val alternatives = splitTopLevel(body, "||")
        for (alternative in alternatives) {
            val value = evalSingle(alternative.trim(), element, jsonText, baseUrl, current)
            if (!value.isNullOrBlank()) {
                return applyReplace(value, replaceRegex, replacement)
            }
        }
        return null
    }

    private suspend fun evalSingle(
        rule: String,
        element: Element?,
        jsonText: String?,
        baseUrl: String,
        current: String?,
    ): String? {
        val conjunction = splitTopLevel(rule, "&&")
        if (conjunction.size > 1) {
            return conjunction.mapNotNull { part ->
                evalSingle(part.trim(), element, jsonText, baseUrl, current)
            }.joinToString("\n")
                .takeIf { it.isNotBlank() }
        }

        // Whole-rule JS survives the || split as its own alternative.
        if (rule.startsWith("<js>") && rule.endsWith("</js>")) {
            return js.eval(rule.substring(4, rule.length - 5), bindings(current, baseUrl))
        }
        if (rule.startsWith("@js:")) {
            return js.eval(rule.removePrefix("@js:"), bindings(current, baseUrl))
        }

        if (rule.contains("{{")) {
            return evalInterpolated(rule, element, jsonText, baseUrl, current)
        }

        return when {
            rule.startsWith("@json:") || rule.startsWith("$.") ->
                evalJsonPath(rule.removePrefix("@json:"), jsonText ?: return null)
            rule.startsWith("@css:") -> evalCss(rule.removePrefix("@css:"), element ?: return null)
            else ->
                // A JSON context only understands JSON paths; anything else is CSS.
                if (element != null) evalCss(rule, element) else null
        }
    }

    /** Mixed literal + `{{expr}}` rules concatenate; a single expression replaces the rule. */
    private suspend fun evalInterpolated(
        rule: String,
        element: Element?,
        jsonText: String?,
        baseUrl: String,
        current: String?,
    ): String? {
        val tokens = tokenizeInterpolation(rule)
        if (tokens.size == 1 && tokens[0].isExpression) {
            return js.eval(tokens[0].text, bindings(current, baseUrl))
        }
        val buffer = StringBuilder()
        for (token in tokens) {
            if (token.isExpression) {
                buffer.append(js.eval(token.text, bindings(current, baseUrl)).orEmpty())
            } else {
                buffer.append(token.text)
            }
        }
        return buffer.toString().takeIf { it.isNotEmpty() }
    }

    private fun tokenizeInterpolation(rule: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val buffer = StringBuilder()
        var i = 0
        while (i < rule.length) {
            if (i + 1 < rule.length && rule[i] == '{' && rule[i + 1] == '{') {
                val end = rule.indexOf("}}", i + 2)
                if (end < 0) {
                    buffer.append(rule[i]); i++
                    continue
                }
                if (buffer.isNotEmpty()) tokens += Token(buffer.toString(), false)
                buffer.setLength(0)
                tokens += Token(rule.substring(i + 2, end), true)
                i = end + 2
            } else {
                buffer.append(rule[i]); i++
            }
        }
        if (buffer.isNotEmpty()) tokens += Token(buffer.toString(), false)
        return tokens
    }

    private data class Token(val text: String, val isExpression: Boolean)

    private fun evalCss(rule: String, root: Element): String? {
        val (selector, attr) = splitAttr(rule)
        if (selector.isBlank()) return null

        // Community rules can contain selectors ksoup rejects; treat them as no match.
        val nodes = runCatching { root.select(selector).toList() }.getOrDefault(emptyList())
        if (nodes.isEmpty()) return null

        val target = nodes.first()
        return when (attr) {
            null -> target.text()
            "text" -> target.text()
            "ownText" -> target.ownText()
            "html" -> target.html()
            "textNodes" -> target.children()
                .joinToString("\n") { it.ownText() }
                .ifBlank { target.ownText() }
            else -> target.attr(attr).takeIf { it.isNotBlank() }
        }?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun splitAttr(rule: String): Pair<String, String?> {
        val at = rule.lastIndexOf('@')
        if (at <= 0) return rule to null
        val tail = rule.substring(at + 1)
        if (!tail.matches(ATTR_TAIL)) return rule to null
        return rule.substring(0, at).trim() to tail
    }

    private fun evalJsonPath(path: String, jsonText: String): String? {
        val element = runCatching { Json.parseToJsonElement(jsonText) }.getOrNull() ?: return null
        return walkJsonPath(element, path)
    }

    private fun evalJsonPathList(path: String, jsonText: String): List<String> {
        val element = runCatching { Json.parseToJsonElement(jsonText) }.getOrNull() ?: return emptyList()
        return when (val result = walkJsonRaw(element, path)) {
            is JsonArray -> result.map { (it as? JsonPrimitive)?.content ?: it.toString() }
            null -> emptyList()
            else -> listOf((result as? JsonPrimitive)?.content ?: result.toString())
        }
    }

    private fun walkJsonPath(element: JsonElement, path: String): String? {
        val raw = walkJsonRaw(element, path) ?: return null
        return when (raw) {
            is JsonPrimitive -> raw.content
            is JsonArray -> raw.joinToString("\n") { (it as? JsonPrimitive)?.content ?: it.toString() }
            else -> raw.toString()
        }
    }

    private fun walkJsonRaw(element: JsonElement, path: String): JsonElement? {
        var current: JsonElement = element
        val normalized = path.trim().removePrefix("$").removePrefix(".")
        if (normalized.isEmpty()) return current

        for (part in normalized.split('.')) {
            val match = PATH_PART.matchEntire(part) ?: return null
            val name = match.groupValues[1]
            val index = match.groupValues[3]

            current = when {
                name.isEmpty() -> current
                current is JsonObject -> current[name] ?: return null
                current is JsonArray -> current.mapNotNull { (it as? JsonObject)?.get(name) }
                    .let(::JsonArray)
                else -> return null
            }
            when {
                index == "*" -> {
                    current = when (current) {
                        is JsonArray -> current
                        else -> return null
                    }
                }
                index.isNotEmpty() -> {
                    val array = current as? JsonArray ?: return null
                    current = array.getOrNull(index.toInt()) ?: return null
                }
            }
        }
        return current
    }

    private fun splitReplace(rule: String): Triple<String, String?, String?> {
        val at = rule.indexOf("##")
        if (at < 0) return Triple(rule, null, null)
        val head = rule.substring(0, at).trim()
        val tail = rule.substring(at + 2)
        val parts = tail.split("##")
        return Triple(head, parts.getOrNull(0), parts.getOrNull(1) ?: "")
    }

    private fun applyReplace(value: String, regex: String?, replacement: String?): String {
        regex ?: return value
        return runCatching { value.replace(Regex(regex), replacement ?: "") }.getOrDefault(value)
    }

    /** Splits on [separator] only outside `{{}}` and `<js>…</js>` regions. */
    private fun splitTopLevel(rule: String, separator: String): List<String> {
        val parts = mutableListOf<String>()
        val buffer = StringBuilder()
        var i = 0
        var braceDepth = 0
        var inJsBlock = false
        while (i < rule.length) {
            if (!inJsBlock && i + 1 < rule.length && rule[i] == '{' && rule[i + 1] == '{') {
                braceDepth++
                buffer.append("{{")
                i += 2
                continue
            }
            if (!inJsBlock && braceDepth > 0 && i + 1 < rule.length && rule[i] == '}' && rule[i + 1] == '}') {
                braceDepth--
                buffer.append("}}")
                i += 2
                continue
            }
            if (!inJsBlock && rule.startsWith("<js>", i)) {
                inJsBlock = true
                buffer.append("<js>")
                i += 4
                continue
            }
            if (inJsBlock && rule.startsWith("</js>", i)) {
                inJsBlock = false
                buffer.append("</js>")
                i += 5
                continue
            }
            if (braceDepth == 0 && !inJsBlock && rule.startsWith(separator, i)) {
                parts += buffer.toString()
                buffer.setLength(0)
                i += separator.length
                continue
            }
            buffer.append(rule[i])
            i++
        }
        parts += buffer.toString()
        return parts
    }

    private fun bindings(current: String?, baseUrl: String): Map<String, String> = buildMap {
        put("result", current ?: "")
        put("baseUrl", baseUrl)
    }

    private companion object {
        val ATTR_TAIL = Regex("[a-zA-Z][a-zA-Z0-9_-]*")
        val PATH_PART = Regex("^([A-Za-z_][A-Za-z0-9_\\-]*)(\\[(\\*|\\d+)\\])?$")
    }
}

/** JS bridge for `{{}}` and `<js>` rules. */
interface LegadoJs {
    suspend fun eval(expression: String, bindings: Map<String, String>): String?
}

/** Fallback used in tests and previews: JS rules evaluate to nothing. */
object LegadoNoJs : LegadoJs {
    override suspend fun eval(expression: String, bindings: Map<String, String>): String? = null
}

/** QuickJS-backed engine; one instance per source, close after use. */
class LegadoQuickJsEngine : LegadoJs, AutoCloseable {

    private val quickJs = com.dokar.quickjs.QuickJs.create(jobDispatcher = kotlinx.coroutines.Dispatchers.Default)

    override suspend fun eval(expression: String, bindings: Map<String, String>): String? = try {
        val prelude = bindings.entries.joinToString("; ") { (key, value) ->
            "var $key = ${JsonPrimitive(value)};"
        }
        quickJs.evaluate<String>("$prelude\nString(($expression))")
    } catch (_: Throwable) {
        null
    }

    override fun close() {
        quickJs.close()
    }
}

/** Parses an HTML snippet for rule evaluation convenience (tests and thin callers). */
fun parseLegadoHtml(html: String): Element = Ksoup.parse(html, "")
