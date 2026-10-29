package app.miyomu.reader.novel

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

/**
 * Novel reader typography and presentation preferences, backed by the same
 * [PreferenceStore] as the ported Aniyomi groups (one JSON file per platform).
 */
class NovelReaderPreferences(
    private val store: PreferenceStore,
) {

    fun readingMode() = store.getEnum("novel_reader_mode", NovelReadingMode.PAGED)

    fun theme() = store.getEnum("novel_reader_theme", NovelReaderTheme.LIGHT)

    /** Body text size in sp. */
    fun fontSize() = store.getInt("novel_reader_font_size", DEFAULT_FONT_SIZE)

    /** Line spacing as a multiplier of the font's natural line height. */
    fun lineHeight() = store.getFloat("novel_reader_line_height", DEFAULT_LINE_HEIGHT)

    /** Horizontal reader padding in dp (page margins). */
    fun pagePadding() = store.getInt("novel_reader_page_padding", DEFAULT_PAGE_PADDING)

    /** Extra spacing between paragraphs in dp. */
    fun paragraphSpacing() = store.getInt("novel_reader_paragraph_spacing", DEFAULT_PARAGRAPH_SPACING)

    companion object {
        const val DEFAULT_FONT_SIZE = 18
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 32

        const val DEFAULT_LINE_HEIGHT = 1.4f
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 2.0f

        const val DEFAULT_PAGE_PADDING = 24
        const val MIN_PAGE_PADDING = 0
        const val MAX_PAGE_PADDING = 64

        const val DEFAULT_PARAGRAPH_SPACING = 8
        const val MIN_PARAGRAPH_SPACING = 0
        const val MAX_PARAGRAPH_SPACING = 32

        fun clampFontSize(value: Int) = value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        fun clampLineHeight(value: Float) = value.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT)
        fun clampPagePadding(value: Int) = value.coerceIn(MIN_PAGE_PADDING, MAX_PAGE_PADDING)
        fun clampParagraphSpacing(value: Int) = value.coerceIn(MIN_PARAGRAPH_SPACING, MAX_PARAGRAPH_SPACING)
    }
}
