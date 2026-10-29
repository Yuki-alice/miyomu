package eu.kanade.tachiyomi.data.download.anime.model

/**
 * CMP port stub: the download pipeline arrives in Phase C; the UI only needs the State enum.
 */
class AnimeDownload private constructor() {

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }
}