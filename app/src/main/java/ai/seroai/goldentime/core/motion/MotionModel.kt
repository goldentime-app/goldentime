package ai.seroai.goldentime.core.motion

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 멀미 노출량 모델 (MSDV 기반). prototype/index.html의 `bp`, `core`와 수치 동일해야 함.
 *
 * 대역 가중 0.03–0.25 Hz: 1차 저역통과(τL) 뒤 1차 고역통과(τH)를 축별로 적용.
 * MSDV = √∫a_w²dt (m/s^1.5). ISO 2631-1 멀미 지표이나 수직 진동 기준이라 참고용.
 * 근거와 한계: docs/골든타임_멀미모델_근거문헌.md — 기준값은 가설.
 */
object MotionConstants {
    const val BAND_LOW_HZ = 0.03
    const val BAND_HIGH_HZ = 0.25
    val TAU_L = 1.0 / (2.0 * PI * BAND_HIGH_HZ)
    val TAU_H = 1.0 / (2.0 * PI * BAND_LOW_HZ)
}

/** 1차 저역통과 → 1차 고역통과 직렬 (대역 가중). 프로토타입 bp()와 동일. */
class BandPassFilter {
    private var lp = 0.0
    private var hp = 0.0
    private var prevLp = 0.0

    fun step(x: Double, dt: Double): Double {
        lp += (x - lp) * dt / (MotionConstants.TAU_L + dt)
        hp = MotionConstants.TAU_H / (MotionConstants.TAU_H + dt) * (hp + lp - prevLp)
        prevLp = lp
        return hp
    }
}

/** 탑승자 유형별 개인 기준 초기값 (가설 — 화면·문서에 가설임을 유지). */
enum class Occupant(val defaultThreshold: Double, val label: String) {
    CHILD(30.0, "뒷좌석 아이"),
    ADULT(45.0, "조수석 성인"),
    DRIVER(65.0, "운전자"),
}

/** 한 축 입력 쌍: 세로 a_x(제동·가감속), 가로 a_y(회전). 단위 m/s². */
data class Accel(val ax: Double, val ay: Double)

/**
 * MSDV 누적기. step()에 축별 가속도를 넣으면 대역 가중 후 노출량을 누적한다.
 * 실차에서는 a_y = v²·κ (v = 위치 변화 속도, κ = 핸들각/조향비/축거 및 heading 변화로 교차 확인).
 */
class MotionModel {
    private val fx = BandPassFilter()
    private val fy = BandPassFilter()
    private var dose2 = 0.0

    /** 마지막 가중 가속도 크기 |a_w| (화면 표시용) */
    var lastWeightedAccel = 0.0
        private set

    val msdv: Double get() = sqrt(dose2)

    fun step(a: Accel, dt: Double) {
        val aw = hypot(fx.step(a.ax, dt), fy.step(a.ay, dt))
        dose2 += aw * aw * dt
        lastWeightedAccel = aw
    }

    fun reset() {
        dose2 = 0.0
        lastWeightedAccel = 0.0
    }

    companion object {
        /** 위험도(%) = min(100, MSDV / 개인기준 × 100), 음수 방지 후 반올림 */
        fun riskPercent(msdv: Double, threshold: Double): Int =
            max(0.0, min(100.0, msdv / threshold * 100.0)).roundToInt()

        /** H2 대응 화면 전환 기준 */
        const val RESPOND_AT_PERCENT = 70

        /**
         * MISC(0–10) 응답으로 개인 기준 보정:
         * 응답 ≥ 4 이고 MSDV > 1 이면 기존 없으면 MSDV, 있으면 0.6·기존 + 0.4·MSDV.
         * 조건 미달이면 기존 값 유지.
         */
        fun updatePersonalThreshold(current: Double?, misc: Int, msdv: Double): Double? {
            if (misc < 4 || msdv <= 1.0) return current
            return if (current == null) msdv else 0.6 * current + 0.4 * msdv
        }
    }
}

/**
 * 시연용 주행 시뮬레이터 — 프로토타입 core()의 κ, a_x 합성식과 동일.
 * speedKmh(속도), curvesPerMin(분당 커브 수), smoothness(부드러움 0–100).
 */
class MotionSimulator(private val model: MotionModel = MotionModel()) {
    var t = 0.0
        private set

    val msdv: Double get() = model.msdv

    fun step(speedKmh: Double, curvesPerMin: Double, smoothness: Double, dt: Double) {
        val v = speedKmh / 3.6
        val fc = curvesPerMin / 60.0
        val kAmp = 0.0006 + 0.0009 * curvesPerMin
        val kappa = if (fc > 0) kAmp * sin(2 * PI * fc * t) * (0.8 + 0.2 * sin(0.37 * t)) else 0.0
        val ay = v * v * kappa
        val harsh = (100.0 - smoothness) / 100.0
        val ax = -harsh * 1.8 * max(0.0, cos(2 * PI * fc * t)) * (if (fc > 0) 1.0 else 0.0) +
            0.15 * sin(0.05 * t)
        model.step(Accel(ax, ay), dt)
        t += dt
    }
}
