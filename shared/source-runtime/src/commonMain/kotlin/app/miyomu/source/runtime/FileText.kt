package app.miyomu.source.runtime

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

internal fun readTextIfExists(path: Path): String? {
    if (!SystemFileSystem.exists(path)) return null
    return SystemFileSystem.source(path).buffered().use { it.readString() }
}

internal fun writeTextAtomically(path: Path, text: String) {
    path.parent?.let { SystemFileSystem.createDirectories(it) }

    val temp = Path(path.toString() + ".tmp")
    SystemFileSystem.sink(temp).buffered().use { it.writeString(text) }
    runCatching { SystemFileSystem.atomicMove(temp, path) }
        .onFailure {
            if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
            SystemFileSystem.atomicMove(temp, path)
        }
}
