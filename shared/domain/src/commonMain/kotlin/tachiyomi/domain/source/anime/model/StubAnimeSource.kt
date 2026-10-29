package tachiyomi.domain.source.anime.model

import eu.kanade.tachiyomi.animesource.AnimeSource
import tachiyomi.domain.source.manga.model.SourceNotInstalledException

class StubAnimeSource(
    override val id: Long,
    override val lang: String,
    override val name: String,
) : AnimeSource {

    private val isInvalid: Boolean = name.isBlank() || lang.isBlank()

    override fun toString(): String =
        if (!isInvalid) "$name (${lang.uppercase()})" else id.toString()

    companion object {
        fun from(source: AnimeSource): StubAnimeSource {
            return StubAnimeSource(id = source.id, lang = source.lang, name = source.name)
        }
    }
}