package ai.seroai.goldentime.core.motion

import kotlin.math.abs
import kotlin.math.tan

/**
 * 실차 신호 → 멀미 모델 입력 변환 (CLAUDE.md 멀미 모델 명세).
 *
 * 가로 가속 a_y = v²·κ, κ = tan(핸들각/조향비)/축거  (자전거 모델 근사)
 * 세로 가속 a_x = 속도 변화 (브레이크 신호 보조)
 *
 * 기하 상수는 아이오닉5급 승용 기준 가설값 — 실차 보정 항목.
 */
class LiveMotionEngine(
    private val model: MotionModel = MotionModel(),
    private val steeringRatio: Double = 15.0,
    private val wheelbaseMeters: Double = 3.0,
) {
    companion object {
        /** 승용차 횡가속 물리 한계 근사 (0.8g) */
        const val MAX_LATERAL_MS2 = 7.8
    }

    private var prevSpeedMs: Double? = null

    val msdv: Double get() = model.msdv
    val lastWeightedAccel: Double get() = model.lastWeightedAccel

    /**
     * 주기 호출 (dt초 간격).
     * @param steeringDeg 핸들 각도 (도, -540~540)
     * @param speedKmh 추정 속도 (km/h)
     * @param brakePressed 브레이크 답입 여부 (Pleos는 Boolean)
     */
    fun step(steeringDeg: Double, speedKmh: Double, brakePressed: Boolean, dt: Double) {
        val v = speedKmh / 3.6
        val wheelRad = Math.toRadians(steeringDeg / steeringRatio)
        val kappa = tan(wheelRad) / wheelbaseMeters
        // 실차 횡가속 한계(~0.8g)로 클램프 — 시뮬레이터 극단 입력이 비물리적 노출을 만들지 않게
        val ay = (v * v * kappa).coerceIn(-MAX_LATERAL_MS2, MAX_LATERAL_MS2)

        // 세로: 속도 변화 기반, 브레이크 신호는 감속 보조 (속도 신호가 느릴 때)
        val dv = prevSpeedMs?.let { (v - it) / dt } ?: 0.0
        prevSpeedMs = v
        val ax = if (brakePressed && abs(dv) < 0.5) -2.0 else dv

        model.step(Accel(ax, ay), dt)
    }

    fun reset() {
        model.reset()
        prevSpeedMs = null
    }
}
