package app.miyomu.source.runtime

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * Loads real, unmodified third-party Venera sources into our runtime and reports which ones
 * construct.
 *
 * This is the executable half of the "zero-modification" claim: `tools/venera_compat.py` says
 * statically which host APIs a source needs, and this test says whether the script actually
 * parses, instantiates and registers under QuickJS. Construction alone touches class fields,
 * arrow functions, optional chaining, template literals and the `ComicSource` subclass
 * machinery, so it catches dialect gaps that the static audit cannot see.
 *
 * Only construction is exercised — no HTTP. The sources live in `reference/` (gitignored), so
 * the test skips itself on machines without a checkout. Run it with a checkout in place:
 *
 *     ./gradlew :shared:source-runtime:jvmTest --tests '*VeneraSourceCompatTest*'
 *
 * It is a report, not a gate: whether a source *should* load is decided by the static audit.
 */
class VeneraSourceCompatTest {

    @TempDir
    lateinit var root: File

    /** Gradle runs tests from the module directory, so walk up to the repository root. */
    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        return File(System.getProperty("user.dir")).absoluteFile
    }

    private fun checkoutDirs(): List<File> {
        val root = repoRoot()
        return listOf("reference/venera-yuan", "reference/venera")
            .map { File(root, it) }
            .filter { it.isDirectory }
    }

    private fun sources(): List<File> = checkoutDirs()
        .flatMap { dir -> dir.listFiles { file -> file.extension == "js" }?.toList().orEmpty() }
        // Underscore files are the API spec (_venera_.js) and the blank template, not sources.
        .filterNot { it.name.startsWith("_") }
        .sortedBy { it.name }

    @Test
    fun `third-party sources construct in the runtime`() {
        val sources = sources()
        assumeTrue(
            sources.isNotEmpty(),
            "no local Venera source checkout under reference/ — nothing to verify",
        )

        data class Outcome(val name: String, val descriptor: String?, val error: String?)

        val outcomes = sources.map { file ->
            val dataDir = kotlinx.io.files.Path(File(root, file.nameWithoutExtension).absolutePath)
            runCatching {
                runBlocking {
                    val source = loadJsComicSource(file.readText(), dataDir)
                    try {
                        "${source.descriptor.name} [${source.descriptor.id}] v${source.descriptor.version}"
                    } finally {
                        source.close()
                    }
                }
            }.fold(
                onSuccess = { Outcome(file.name, it, null) },
                onFailure = { Outcome(file.name, null, it.stackTraceToString().replace("\n", " \u23ce ").take(500)) },
            )
        }

        val ok = outcomes.filter { it.error == null }
        val failed = outcomes - ok.toSet()

        val report = buildString {
            appendLine("# Venera source construction report")
            appendLine()
            appendLine("${ok.size} / ${outcomes.size} sources constructed unmodified.")
            appendLine()
            appendLine("| source | result |")
            appendLine("|---|---|")
            outcomes.forEach {
                val cell = it.descriptor?.let { d -> "ok — $d" } ?: "FAIL — ${it.error}"
                appendLine("| ${it.name} | $cell |")
            }
        }
        val out = File(repoRoot(), "build/reports/venera-compat.md")
        out.parentFile.mkdirs()
        out.writeText(report)
        println(report)

        assertTrue(ok.isNotEmpty(), "no source constructed — the harness itself is broken")
    }
}
