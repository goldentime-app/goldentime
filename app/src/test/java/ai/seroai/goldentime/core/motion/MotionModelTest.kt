package ai.seroai.goldentime.core.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionModelTest {

    @Test
    fun `입력이 없으면 MSDV는 0`() {
        val m = MotionModel()
        repeat(1000) { m.step(Accel(0.0, 0.0), 0.1) }
        assertEquals(0.0, m.msdv, 1e-9)
    }

    @Test
    fun `MSDV는 단조 증가(노출 누적)`() {
        val m = MotionModel()
        var prev = 0.0
        repeat(500) {
            m.step(Accel(0.5, 0.3), 0.1)
            assertTrue(m.msdv >= prev)
            prev = m.msdv
        }
        assertTrue(prev > 0)
    }

    @Test
    fun `대역 밖 정상 성분(DC)은 고역통과가 걸러낸다`() {
        // 일정한 가속(0 Hz)은 0.03 Hz 고역통과에 의해 장기적으로 0으로 수렴해야 한다
        val f = BandPassFilter()
        var last = 0.0
        repeat(60000) { last = f.step(1.0, 0.1) } // 100분간 1 m/s² 고정
        assertTrue("정상 성분 잔류: $last", kotlin.math.abs(last) < 0.01)
    }

    @Test
    fun `위험도 계산과 상한`() {
        assertEquals(0, MotionModel.riskPercent(0.0, 30.0))
        assertEquals(50, MotionModel.riskPercent(15.0, 30.0))
        assertEquals(100, MotionModel.riskPercent(60.0, 30.0)) // 상한 100
        assertEquals(70, MotionModel.riskPercent(21.0, 30.0)) // H2 전환 경계
    }

    @Test
    fun `개인 기준 초기값(가설)`() {
        assertEquals(30.0, Occupant.CHILD.defaultThreshold, 0.0)
        assertEquals(45.0, Occupant.ADULT.defaultThreshold, 0.0)
        assertEquals(65.0, Occupant.DRIVER.defaultThreshold, 0.0)
    }

    @Test
    fun `MISC 응답으로 개인 기준 보정`() {
        // 조건 미달: MISC < 4 또는 MSDV <= 1 → 유지
        assertNull(MotionModel.updatePersonalThreshold(null, 3, 20.0))
        assertNull(MotionModel.updatePersonalThreshold(null, 5, 0.5))
        assertEquals(40.0, MotionModel.updatePersonalThreshold(40.0, 3, 20.0)!!, 0.0)
        // 기존 없으면 MSDV로
        assertEquals(20.0, MotionModel.updatePersonalThreshold(null, 4, 20.0)!!, 0.0)
        // 기존 있으면 0.6·기존 + 0.4·MSDV
        assertEquals(0.6 * 30.0 + 0.4 * 20.0, MotionModel.updatePersonalThreshold(30.0, 4, 20.0)!!, 1e-9)
    }
}
