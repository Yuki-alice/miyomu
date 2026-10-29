package app.miyomu.reader.novel

/** How chapter text is presented. Persisted as an ordinal-stable int. */
enum class NovelReadingMode {
    /** Fixed pages of text, advanced by swipes / taps / arrow keys. */
    PAGED,

    /** Continuous vertical list of paragraphs. */
    SCROLL,
    ;

    companion object {
        fun fromPersisted(value: Int): NovelReadingMode =
            entries.getOrElse(value) { PAGED }
    }
}

/** Reader surface colors. The concrete colors live in the UI layer; this names the choice. */
enum class NovelReaderTheme {
    LIGHT,
    SEPIA,
    DARK,
    BLACK,
    ;

    companion object {
        fun fromPersisted(value: Int): NovelReaderTheme =
            entries.getOrElse(value) { LIGHT }
    }
}
