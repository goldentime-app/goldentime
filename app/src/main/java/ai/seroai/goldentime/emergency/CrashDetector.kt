package ai.seroai.goldentime.emergency

/**
 * 사고 자동 감지 (E7): 에어백 전개 신호(Safety registerSeatAirbagDeployed) 수신 후
 * 10초 카운트다운. "괜찮아요" 응답이면 해제, 무응답이면 응급 모드(E2, 사고 요약)로 전환.
 */
class CrashDetector {

    companion object {
        const val RESPONSE_WINDOW_MS = 10_000L
    }

    sealed interface State {
        data object Idle : State
        data class Asking(val remainingSeconds: Int) : State
        data object DriverOk : State

        /** 무응답 → 응급 모드 전환, 119 전송은 Send119Controller가 담당 */
        data object Unresponsive : State
    }

    private var airbagAt: Long? = null
    private var driverOk = false

    fun onAirbagDeployed(nowMs: Long) {
        airbagAt = nowMs
        driverOk = false
    }

    fun onDriverOk() {
        driverOk = true
    }

    fun reset() {
        airbagAt = null
        driverOk = false
    }

    fun state(nowMs: Long): State {
        val at = airbagAt ?: return State.Idle
        if (driverOk) return State.DriverOk
        val elapsed = nowMs - at
        return if (elapsed >= RESPONSE_WINDOW_MS) {
            State.Unresponsive
        } else {
            State.Asking((((RESPONSE_WINDOW_MS - elapsed) + 999) / 1000).toInt())
        }
    }
}
