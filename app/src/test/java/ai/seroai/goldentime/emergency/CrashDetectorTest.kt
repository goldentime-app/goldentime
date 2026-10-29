package ai.seroai.goldentime.emergency

import ai.seroai.goldentime.emergency.CrashDetector.State
import org.junit.Assert.assertEquals
import org.junit.Test

class CrashDetectorTest {

    @Test
    fun `에어백 전개 후 10초 질문 카운트다운`() {
        val d = CrashDetector()
        assertEquals(State.Idle, d.state(0))
        d.onAirbagDeployed(0)
        assertEquals(State.Asking(10), d.state(0))
        assertEquals(State.Asking(1), d.state(9_100))
    }

    @Test
    fun `괜찮아요 응답이면 해제`() {
        val d = CrashDetector()
        d.onAirbagDeployed(0)
        d.onDriverOk()
        assertEquals(State.DriverOk, d.state(5_000))
        assertEquals(State.DriverOk, d.state(20_000)) // 이후에도 응급 전환 없음
    }

    @Test
    fun `10초 무응답이면 응급 전환`() {
        val d = CrashDetector()
        d.onAirbagDeployed(0)
        assertEquals(State.Unresponsive, d.state(10_000))
    }

    @Test
    fun `리셋하면 대기 상태`() {
        val d = CrashDetector()
        d.onAirbagDeployed(0)
        d.reset()
        assertEquals(State.Idle, d.state(15_000))
    }
}
