package ai.seroai.goldentime.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.util.Log
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitListener
import ai.pleos.playground.stt.listener.OnServerConnectionListener as SttServerListener
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnServerConnectionListener as TtsServerListener

/**
 * Pleos TextToSpeech SDK 3.3.1 — HYBRID(서버 우선, 끊기면 차량 내 엔진).
 * 터널·산간에서도 응급 안내가 끊기지 않도록 HYBRID. 클라이언트 ID가 없으면 차량 내 엔진만 사용.
 * 초기화에 실패하면 [fallback](Android TTS)으로 발화한다.
 */
class PleosSpeaker(
    context: Context,
    clientId: String,
    clientSecret: String,
    private val fallback: Speaker,
) : Speaker {
    companion object { private const val TAG = "PleosSpeaker" }

    private val tts = TextToSpeech(context, TtsMode.HYBRID)
    @Volatile private var ready = false

    init {
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                tts.setLocale(Locale.KOREA)
                ready = true
                Log.i(TAG, "Pleos TTS ready")
                if (clientId.isNotEmpty()) {
                    tts.registerApp(clientId, clientSecret, object : TtsServerListener {
                        override fun onConnected() { Log.i(TAG, "TTS server connected") }
                        override fun onFailed(errorType: TtsError) { Log.w(TAG, "TTS register failed: $errorType") }
                    })
                }
            }

            override fun onFailure(errorType: TtsError) {
                Log.w(TAG, "TTS init failed: $errorType → Android TTS")
            }
        })
    }

    val usingPleos: Boolean get() = ready

    override fun speak(text: String) {
        if (ready) tts.speak(text, true) else fallback.speak(text)
    }

    override fun stop() {
        if (ready) tts.stop() else fallback.stop()
    }

    fun release() = tts.release()
}

/**
 * Pleos SpeechToText SDK 3.3.1 — 차량 마이크로 1회 인식 (E1 응급 접수).
 * completed=true인 첫 결과를 돌려준다. 초기화 실패 시 [fallback](Android SpeechRecognizer).
 */
class PleosListener(
    context: Context,
    clientId: String,
    clientSecret: String,
    private val fallback: Listener,
) : Listener {
    companion object { private const val TAG = "PleosListener" }

    private val stt = SpeechToText(context, SttMode.HYBRID)
    @Volatile private var ready = false
    private var current: ResultListener? = null

    init {
        stt.initialize(object : SttInitListener {
            override fun onSuccess() {
                stt.setLocale(Locale.KOREA)
                ready = true
                Log.i(TAG, "Pleos STT ready")
                if (clientId.isNotEmpty()) {
                    stt.registerApp(clientId, clientSecret, object : SttServerListener {
                        override fun onConnected() { Log.i(TAG, "STT server connected") }
                        override fun onFailed(errorType: SttError) { Log.w(TAG, "STT register failed: $errorType") }
                    })
                }
            }

            override fun onFailure(errorType: SttError) {
                Log.w(TAG, "STT init failed: $errorType → Android STT")
            }
        })
    }

    override fun listenOnce(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!ready) return fallback.listenOnce(onResult, onError)
        cancel()
        var delivered = false
        val l = object : ResultListener {
            override fun onUpdated(stt: String, completed: Boolean) {
                if (completed && !delivered && stt.isNotBlank()) {
                    delivered = true
                    onResult(stt)
                }
            }

            override fun onError(errorType: SttError) {
                if (!delivered) { delivered = true; onError("pleos_stt_$errorType") }
            }

            override fun onEndedRecognition() {
                if (!delivered) { delivered = true; onError("empty_result") }
            }

            override fun onReady() {}
            override fun onStartedRecognition() {}
            override fun onUpdatedEpdData(on: Long, off: Long) {}
            override fun onUpdatedRms(rms: Double) {}
        }
        current = l
        stt.addListener(l)
        stt.request()
    }

    override fun cancel() {
        current?.let { stt.stop(); stt.removeListener(it) }
        current = null
        fallback.cancel()
    }

    fun release() {
        cancel()
        stt.release()
    }
}
