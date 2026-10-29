package ai.seroai.goldentime.emergency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyCaseTest {

    @Test
    fun `시연 사례는 유효한 119 문자를 만든다`() {
        val sms = EmergencyCase.SEIZURE.smsText()
        assertTrue(sms.startsWith("[골든타임 자동 요약] 4세 남아 (뒷좌석) 경련, 발열, 약 2분 전 시작"))
        assertTrue(sms.contains("37.43848N 128.01800E"))
        assertTrue(sms.contains("흰색 SUV"))
        assertTrue(sms.endsWith("보호자 전화 연결 중"))
    }

    @Test
    fun `충돌 사례도 형식 유지`() {
        val sms = EmergencyCase.CRASH.smsText()
        assertTrue(sms.contains("충돌 감지, 응답 없음"))
        assertTrue(sms.contains("탑승 3명"))
    }

    @Test
    fun `화면 표시와 문자 본문이 같은 출처를 쓴다`() {
        val c = EmergencyCase.SEIZURE
        assertEquals(c.symptom, c.toReportInput().symptom)
        assertEquals(c.vehicle, c.toReportInput().vehicle)
    }
}
