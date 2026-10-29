package app.miyomu.danmaku

/**
 * Auto-matching helper: searches dandanplay for the anime title and picks the episode whose
 * title parses to the same episode number (falls back to the first result). Returns null when
 * nothing matches or loading fails; callers treat danmaku as absent in that case.
 */
suspend fun DanmakuRepository.matchTrack(animeTitle: String, episodeNumber: Double?): DanmakuTrack? {
    val episodes = runCatching { searchEpisodes(animeTitle) }.getOrDefault(emptyList())
    if (episodes.isEmpty()) return null
    val chosen = episodeNumber
        ?.let { number -> episodes.firstOrNull { parseDanmakuEpisodeNumber(it.episodeTitle) == number } }
        ?: episodes.first()
    return runCatching { loadTrack(chosen.episodeId) }.getOrNull()
}

/** Parses "第 12 话", "第12集", "EP12", "S01E02", "12" style titles. */
fun parseDanmakuEpisodeNumber(title: String): Double? {
    val text = title.trim()
    val patterns = listOf(
        Regex("""第\s*(\d+(?:\.\d+)?)\s*[集话期回]"""),
        Regex("""(?i)\b(?:S\d+\s*E|EP|E)(\d+(?:\.\d+)?)\b"""),
        Regex("""^(\d+(?:\.\d+)?)$"""),
    )
    for (pattern in patterns) {
        val match = pattern.find(text) ?: continue
        return match.groupValues[1].toDoubleOrNull()
    }
    return null
}
