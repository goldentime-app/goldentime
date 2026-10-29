package ai.seroai.goldentime.emergency

/**
 * 119 요약 자동 전송 제어 (절대 원칙 4).
 *
 * - 가족 프로필에서 사전 동의를 켠 경우에만 자동 전송을 준비한다.
 * - 전송 3초 전 화면·음성 고지, "보내지 않기"로 취소 가능.
 * - 심정지 의심은 카운트다운 없이 즉시 전송.
 * - 문자 실패 시 전화만 연결하고 화면 요약을 크게. 전화 연결은 항상 병행.
 *
 * 시간은 밀리초 타임스탬프 주입 (테스트 가능).
 */
class Send119Controller(private val consentGiven: Boolean) {

    companion object {
        const val COUNTDOWN_MS = 3_000L
    }

    sealed interface State {
        /** 자동 전송 없음 (동의 안 함) — 화면에 수동 전송 버튼만 */
        data object ManualOnly : State

        /** 카운트다운 중 — 남은 초 표시, 취소 가능 */
        data class CountingDown(val remainingSeconds: Int) : State

        data object Sent : State
        data object Canceled : State

        /** 문자 실패 → 전화만 연결, 화면 요약 크게 */
        data object SmsFailedCallOnly : State
    }

    private var armedAt: Long? = null
    private var canceled = false
    private var sent = false
    private var smsFailed = false

    /** E2 진입(또는 심정지 감지) 시 호출. immediate = 심정지 의심 → 즉시 전송. */
    fun arm(nowMs: Long, immediate: Boolean = false) {
        canceled = false
        smsFailed = false
        if (!consentGiven && !immediate) {
            armedAt = null
            return
        }
        armedAt = if (immediate) nowMs - COUNTDOWN_MS else nowMs
        if (immediate) sent = true
    }

    /** "보내지 않기" — 전송 전에만 유효 */
    fun cancel(nowMs: Long): Boolean {
        if (sent || state(nowMs) !is State.CountingDown) return false
        canceled = true
        return true
    }

    /** 사용자가 수동으로 "119 요약 보내고 전화" 누름 (취소 후 재전송 포함) */
    fun sendNow() {
        canceled = false
        sent = true
    }

    /** 문자 전송 실패 보고 → 전화만 연결 모드 */
    fun reportSmsFailure() {
        smsFailed = true
    }

    fun state(nowMs: Long): State {
        if (smsFailed) return State.SmsFailedCallOnly
        if (canceled) return State.Canceled
        if (sent) return State.Sent
        val at = armedAt ?: return State.ManualOnly
        val elapsed = nowMs - at
        return if (elapsed >= COUNTDOWN_MS) {
            sent = true
            State.Sent
        } else {
            State.CountingDown((((COUNTDOWN_MS - elapsed) + 999) / 1000).toInt())
        }
    }
}
