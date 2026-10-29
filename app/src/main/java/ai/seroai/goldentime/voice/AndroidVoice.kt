package ai.seroai.goldentime.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Android TextToSpeech(ko-KR) — Pleos TTS SDK 열리면 교체 */
class AndroidSpeaker(context: Context) : Speaker {
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts.language = Locale.KOREAN
        }
    }

    override fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gt-" + text.hashCode())
    }

    override fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}

/** Android SpeechRecognizer(ko-KR) — Pleos STT SDK 열리면 교체. RECORD_AUDIO 권한 필요 */
class AndroidListener(private val context: Context) : Listener {
    companion object { private const val TAG = "AndroidListener" }

    private var recognizer: SpeechRecognizer? = null

    override fun listenOnce(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("recognizer_unavailable")
            return
        }
        cancel()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = texts?.firstOrNull()
                if (best != null) onResult(best) else onError("empty_result")
            }

            override fun onError(error: Int) {
                Log.w(TAG, "STT error $error")
                onError("stt_error_$error")
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            },
        )
    }

    override fun cancel() {
        recognizer?.destroy()
        recognizer = null
    }
}
