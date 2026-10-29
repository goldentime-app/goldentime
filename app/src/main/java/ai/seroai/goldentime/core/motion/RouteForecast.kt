package ai.seroai.goldentime.core.motion

/**
 * 출발 전 경로 멀미 예보 — 같은 MSDV 모델을 경로 구간에 미리 돌린다.
 * 프로토타입 forecast()와 동일: dt = 0.2초, 부드러움 60 고정.
 * 실제 입력은 NaviHelper 회전 정보(TBT)·예상 속도에서 만든다.
 */
data class RouteSegment(
    val minutes: Int,
    val speedKmh: Double,
    val curvesPerMin: Double,
)

object RouteForecast {
    const val DT = 0.2
    const val SMOOTHNESS = 60.0

    fun msdv(segments: List<RouteSegment>): Double {
        val sim = MotionSimulator()
        for (seg in segments) {
            val steps = (seg.minutes * 60 / DT).toInt() // min*300
            repeat(steps) { sim.step(seg.speedKmh, seg.curvesPerMin, SMOOTHNESS, DT) }
        }
        return sim.msdv
    }
}
