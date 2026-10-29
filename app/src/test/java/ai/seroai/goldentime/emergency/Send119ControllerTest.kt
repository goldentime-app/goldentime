package ai.seroai.goldentime.emergency

import ai.seroai.goldentime.emergency.Send119Controller.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 절대 원칙 4: 사전 동의 + 3초 취소, 심정지는 즉시, 문자 실패 시 전화만. */
class Send119ControllerTest {

    @Test
    fun `동의하면 3초 카운트다운 후 전송`() {
        val c = Send119Controller(consentGiven = true)
        c.arm(nowMs = 0)
        assertEquals(State.CountingDown(3), c.state(0))
        assertEquals(State.CountingDown(1), c.state(2_100))
        assertEquals(State.Sent, c.state(3_000))
    }

    @Test
    fun `카운트다운 중 보내지 않기로 취소`() {
        val c = Send119Controller(consentGiven = true)
        c.arm(0)
        assertTrue(c.cancel(1_500))
        assertEquals(State.Canceled, c.state(5_000)) // 시간이 지나도 전송 안 됨
    }

    @Test
    fun `전송 후에는 취소 불가`() {
        val c = Send119Controller(consentGiven = true)
        c.arm(0)
        assertEquals(State.Sent, c.state(3_500))
        assertFalse(c.cancel(3_600))
        assertEquals(State.Sent, c.state(4_000))
    }

    @Test
    fun `동의 없으면 자동 전송 없음(수동만)`() {
        val c = Send119Controller(consentGiven = false)
        c.arm(0)
        assertEquals(State.ManualOnly, c.state(10_000))
        c.sendNow() // 사용자가 직접 누름
        assertEquals(State.Sent, c.state(10_001))
    }

    @Test
    fun `심정지 의심은 카운트다운 없이 즉시 전송`() {
        val c = Send119Controller(consentGiven = true)
        c.arm(0, immediate = true)
        assertEquals(State.Sent, c.state(0))
    }

    @Test
    fun `심정지 즉시 전송은 동의 설정과 무관`() {
        val c = Send119Controller(consentGiven = false)
        c.arm(0, immediate = true)
        assertEquals(State.Sent, c.state(0))
    }

    @Test
    fun `취소 후 수동 재전송 가능`() {
        val c = Send119Controller(consentGiven = true)
        c.arm(0)
        c.cancel(1_000)
        c.sendNow()
        assertEquals(State.Sent, c.state(2_000))
    }

    @Test
    fun `문자 실패 시 전화만 연결 상태`() {
        val c = Send119Controller(consentGiven = true)
        c.arm(0)
        c.state(3_000) // 전송 시도
        c.reportSmsFailure()
        assertEquals(State.SmsFailedCallOnly, c.state(3_500))
    }
}
