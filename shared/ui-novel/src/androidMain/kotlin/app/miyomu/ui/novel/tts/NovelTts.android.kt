package app.miyomu.ui.novel.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun rememberNovelTts(): NovelTts? {
    val context = LocalContext.current
    val engine = remember(context) { AndroidNovelTts(context) }
    DisposableEffect(engine) {
        onDispose { engine.shutdown() }
    }
    return engine
}

/**
 * Platform TTS through Android's [TextToSpeech] (API 21+). Initialization is asynchronous —
 * [supported] is snapshot state so the朗读 affordance appears once the engine is ready.
 */
private class AndroidNovelTts(context: Context) : NovelTts {

    private var ready by mutableStateOf(false)

    private var engine: TextToSpeech? = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { engine?.language = Locale.SIMPLIFIED_CHINESE }
        }
    }

    override val supported: Boolean
        get() = ready

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun speak(text: String, onDone: (() -> Unit)?) {
        if (!ready) return
        val current = engine ?: return
        if (onDone != null) {
            current.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    // Fires on a binder thread; the reader callback hops to the main looper.
                    if (utteranceId == UTTERANCE_ID) mainHandler.post { onDone() }
                }

                @Deprecated("Platform contract override", replaceWith = ReplaceWith(""))
                override fun onError(utteranceId: String?) = Unit
            })
        }
        current.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        engine?.stop()
    }

    override fun shutdown() {
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "miyomu-novel-tts"
    }
}
