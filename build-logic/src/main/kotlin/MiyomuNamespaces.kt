import org.gradle.api.Project

internal fun Project.miyomuNamespace(): String {
    val path = path.removePrefix(":")
    return when (path) {
        "app:shared" -> "app.miyomu.shared"
        else -> "app.miyomu." + path.replace(':', '.')
    }
}
