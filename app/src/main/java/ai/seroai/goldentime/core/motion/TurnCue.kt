package ai.seroai.goldentime.core.motion

import ai.seroai.goldentime.data.navi.TurnDirection

/**
 * 커브 예고 신호 — 멀미 "예보"에서 "완화"로.
 * 다가오는 회전(NaviHelper onTBTInfo)까지 남은 시간을 속도로 환산해, 몇 초 앞에서 미리 알린다.
 * 근거: 예고 신호를 주면 실도로 멀미 증상 감소 (Reuten 외 2024), 몸이 움직임을 예측할 수 있게 하는 원리.
 */
data class TurnCue(val direction: TurnDirection, val secondsAhead: Int)

object TurnCuePlanner {
    /** 예고 시작 (초 전) */
    const val LEAD_MAX_S = 6.0

    /** 이보다 가까우면 이미 회전 중 — 예고하지 않음 */
    const val LEAD_MIN_S = 1.0

    /** 속도가 너무 낮으면(정체·정차) 가속이 작아 예고 불필요 */
    const val MIN_SPEED_KMH = 15.0

    /**
     * @return 지금 예고해야 하면 [TurnCue], 아니면 null
     */
    fun cue(direction: TurnDirection, distanceMeters: Int, speedKmh: Double): TurnCue? {
        if (speedKmh < MIN_SPEED_KMH) return null
        val seconds = distanceMeters / (speedKmh / 3.6)
        if (seconds < LEAD_MIN_S || seconds > LEAD_MAX_S) return null
        return TurnCue(direction, kotlin.math.ceil(seconds).toInt())
    }
}
