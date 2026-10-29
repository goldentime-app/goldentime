package ai.seroai.goldentime.voice

/**
 * 음성 추상화 — Pleos TTS/STT SDK(PleosVoice.kt)가 기본, 실패 시 Android 기본(AndroidVoice.kt)으로 대체.
 * 생명 관련 발화는 FixedScripts 상수만 (절대 원칙 3).
 */
interface Speaker {
    fun speak(text: String)
    fun stop()
}

interface Listener {
    /** 한 번의 발화 인식 시작. 결과는 콜백으로 */
    fun listenOnce(onResult: (String) -> Unit, onError: (String) -> Unit)
    fun cancel()
}

/** 시연·테스트용: 발화를 기록만 */
class FakeSpeaker : Speaker {
    val spoken = mutableListOf<String>()
    override fun speak(text: String) { spoken += text }
    override fun stop() {}
}

class FakeListener(var nextUtterance: String? = null) : Listener {
    override fun listenOnce(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val u = nextUtterance
        if (u != null) onResult(u) else onError("no_input")
    }
    override fun cancel() {}
}
