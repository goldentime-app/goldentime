package ai.seroai.goldentime.core.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LiveMotionEngineTest {

    @Test
    fun `직진이면 노출 누적 없음`() {
        val e = LiveMotionEngine()
        repeat(3000) { e.step(steeringDeg = 0.0, speedKmh = 100.0, brakePressed = false, dt = 0.1) }
        assertEquals(0.0, e.msdv, 0.05)
    }

    @Test
    fun `대역 내 핸들 왕복은 노출을 누적한다`() {
        val e = LiveMotionEngine()
        // 0.1 Hz 좌우 왕복 (멀미 대역 0.03–0.25 Hz), ±90°, 90 km/h
        var t = 0.0
        repeat(6000) {
            val deg = 90.0 * sin(2 * PI * 0.1 * t)
            e.step(deg, 90.0, brakePressed = false, dt = 0.1)
            t += 0.1
        }
        assertTrue("MSDV=${e.msdv}", e.msdv > 1.0)
    }

    @Test
    fun `일정한 조향(DC)은 대역 가중으로 걸러진다`() {
        val sustained = LiveMotionEngine()
        repeat(6000) { sustained.step(90.0, 90.0, false, 0.1) } // 10분간 꺾은 채 유지
        val oscillating = LiveMotionEngine()
        var t = 0.0
        repeat(6000) {
            oscillating.step(90.0 * sin(2 * PI * 0.1 * t), 90.0, false, 0.1)
            t += 0.1
        }
        assertTrue(oscillating.msdv > sustained.msdv)
    }

    @Test
    fun `브레이크 답입은 감속 성분을 더한다`() {
        val withBrake = LiveMotionEngine()
        val without = LiveMotionEngine()
        var t = 0.0
        repeat(3000) {
            // 간헐 제동 (0.05 Hz 주기로 답입)
            val braking = sin(2 * PI * 0.05 * t) > 0.5
            withBrake.step(0.0, 80.0, braking, 0.1)
            without.step(0.0, 80.0, false, 0.1)
            t += 0.1
        }
        assertTrue(withBrake.msdv > without.msdv)
    }
}
