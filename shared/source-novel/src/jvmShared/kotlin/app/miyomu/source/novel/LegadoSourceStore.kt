package app.miyomu.source.novel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Imported legado book sources and the online shelf, persisted as JSON strings in the
 * shared [PreferenceStore] — novel-only data with zero Room schema impact.
 * Sources are read from the store on construction; mutations update both the flow and
 * the store.
 */
class LegadoSourceStore(
    private val store: PreferenceStore,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val mutableSources = MutableStateFlow(sources())
    val sources: StateFlow<List<LegadoBookSource>> = mutableSources.asStateFlow()

    private val mutableShelf = MutableStateFlow(shelf())
    val shelf: StateFlow<List<LegadoOnlineBook>> = mutableShelf.asStateFlow()

    fun sources(): List<LegadoBookSource> = decode(SOURCES_KEY) { raw ->
        runCatching { json.decodeFromString(ListSerializer(LegadoBookSource.serializer()), raw) }
            .getOrDefault(emptyList())
    }

    fun addSource(source: LegadoBookSource) {
        val next = sources().filterNot { it.bookSourceUrl == source.bookSourceUrl } + source
        write(SOURCES_KEY, LegadoBookSource.serializer(), next)
        mutableSources.value = next
    }

    fun removeSource(sourceUrl: String?) {
        val next = sources().filterNot { it.bookSourceUrl == sourceUrl }
        write(SOURCES_KEY, LegadoBookSource.serializer(), next)
        mutableSources.value = next
    }

    fun shelf(): List<LegadoOnlineBook> = decode(SHELF_KEY) { raw ->
        runCatching { json.decodeFromString(ListSerializer(LegadoOnlineBookDto.serializer()), raw) }
            .getOrDefault(emptyList())
            .map { it.toModel() }
    }

    fun addToShelf(book: LegadoOnlineBook) {
        val next = shelf().filterNot { it.bookUrl == book.bookUrl } + book
        writeShelf(next)
    }

    fun removeFromShelf(bookUrl: String) {
        writeShelf(shelf().filterNot { it.bookUrl == bookUrl })
    }

    fun inShelf(bookUrl: String): Boolean = shelf().any { it.bookUrl == bookUrl }

    private fun writeShelf(books: List<LegadoOnlineBook>) {
        write(SHELF_KEY, LegadoOnlineBookDto.serializer(), books.map { it.toDto() })
        mutableShelf.value = books
    }

    private fun <T> decode(key: String, block: (String) -> T): T =
        block(store.getString(key, EMPTY).get())

    private fun <T> write(key: String, serializer: KSerializer<T>, value: List<T>) {
        store.getString(key, EMPTY).set(json.encodeToString(ListSerializer(serializer), value))
    }

    @Serializable
    data class LegadoOnlineBookDto(
        val name: String,
        val author: String? = null,
        val bookUrl: String,
        val tocUrl: String,
        val coverUrl: String? = null,
        val sourceUrl: String? = null,
    ) {
        fun toModel() = LegadoOnlineBook(name, author, bookUrl, tocUrl, coverUrl, sourceUrl)
    }

    companion object {
        private const val EMPTY = "[]"
        private const val SOURCES_KEY = "legado_sources"
        private const val SHELF_KEY = "legado_shelf"
    }
}

private fun LegadoOnlineBook.toDto() = LegadoSourceStore.LegadoOnlineBookDto(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    coverUrl = coverUrl,
    sourceUrl = sourceUrl,
)
