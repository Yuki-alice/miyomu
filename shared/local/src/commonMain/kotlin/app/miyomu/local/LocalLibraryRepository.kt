package app.miyomu.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Holds the currently scanned local library. Scanning is deliberately explicit:
 * the UI decides when to refresh so a large library does not rescan on every recomposition.
 */
class LocalLibraryRepository(
    private val scanner: LocalLibraryScanner = LocalLibraryScanner(),
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
