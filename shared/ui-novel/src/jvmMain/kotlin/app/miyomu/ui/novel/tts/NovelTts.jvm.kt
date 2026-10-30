package app.miyomu.ui.novel.tts

import androidx.compose.runtime.Composable

/**
 * Desktop has no bundled TTS engine, so the朗读 affordance stays hidden; wiring a native
 * engine (OS speech APIs) is a follow-up.
 */
@Composable
actual fun rememberNovelTts(): NovelTts? = null
