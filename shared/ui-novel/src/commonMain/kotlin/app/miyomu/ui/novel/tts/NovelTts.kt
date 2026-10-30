package app.miyomu.ui.novel.tts

import androidx.compose.runtime.Composable

/**
 * P1-7 阅读增强（最小切片）: text-to-speech for the novel reader. Common contract only —
 * platforms provide their own engine via [rememberNovelTts]; desktop returns null today
 * (no bundled TTS), which disables the朗读 affordance gracefully.
 *
 * Scope: speaks the current chapter's text and reports completion through [speak]'s
 * callback, which the reader uses to auto-advance to the next chapter (P1-7 连读).
 */
interface NovelTts {
    /** True once the platform engine initialized successfully. */
    val supported: Boolean

    /**
     * Starts (or restarts) reading [text] aloud, flushing whatever was playing.
     * [onDone] fires once when the utterance finishes naturally (not on stop/shutdown);
     * it may be invoked from a non-main thread.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null)

    fun stop()

    fun shutdown()
}

@Composable
expect fun rememberNovelTts(): NovelTts?
