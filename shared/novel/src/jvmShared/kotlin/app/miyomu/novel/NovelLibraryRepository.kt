package app.miyomu.novel

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalFileRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Holds the currently scanned novel library. Mirrors the comic-side
 * [app.miyomu.local.LocalLibraryRepository]: scanning stays explicit so a large
 * library is not rescanned on every recomposition.
 */
class NovelLibraryRepository(
    private val scanner: NovelLibraryScanner = NovelLibraryScanner(),
) {
    private val mutableBooks = MutableStateFlow<List<LocalBook>>(emptyList())
    val books: StateFlow<List<LocalBook>> = mutableBooks.asStateFlow()

    suspend fun refresh(root: LocalFileRef) {
        mutableBooks.value = withContext(Dispatchers.Default) {
            runCatching { scanner.scan(root) }
                .getOrElse { emptyList() }
        }
    }
}
