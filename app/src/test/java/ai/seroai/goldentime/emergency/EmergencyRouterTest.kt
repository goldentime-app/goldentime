package ai.seroai.goldentime.emergency

import ai.seroai.goldentime.core.report.Report119Builder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EmergencyRouterTest {

    @Test
    fun `심정지 의심 키워드는 E6`() {
        assertEquals(Screen.E6, EmergencyRouter.route("아빠가 숨을 안 쉬어!"))
        assertEquals(Screen.E6, EmergencyRouter.route("숨을 쉬지 않아요"))
        assertEquals(Screen.E6, EmergencyRouter.route("맥박이 없어요"))
    }

    @Test
    fun `그 외 응급 발화는 E1 접수`() {
        assertEquals(Screen.E1, EmergencyRouter.route("아이가 경련해!"))
        assertEquals(Screen.E1, EmergencyRouter.route("아이가 열이 나요"))
        assertEquals(Screen.E1, EmergencyRouter.route("응급 모드"))
    }

    @Test
    fun `고정 대본에는 진단어가 없다`() {
        listOf(
            FixedScripts.ARREST_STOP, FixedScripts.ARREST_CPR, FixedScripts.ARREST_BEAT,
            FixedScripts.CRASH_ASK, FixedScripts.CRASH_TO_EMERGENCY, FixedScripts.CRASH_SENT,
            FixedScripts.SEND_NOTICE, FixedScripts.SEIZURE_CARE, FixedScripts.NORMAL_SPEED,
            FixedScripts.RDV_PREFIX,
        ).forEach { assertFalse("진단어 포함: $it", Report119Builder.containsDiagnosis(it)) }
    }

    @Test
    fun `고정 대본은 과속을 유도하지 않는다`() {
        // 절대 원칙 5 — "빨리", "속도를 올려" 류 표현 금지
        listOf(
            FixedScripts.ARREST_STOP, FixedScripts.CRASH_SENT, FixedScripts.SEND_NOTICE,
            FixedScripts.NORMAL_SPEED, FixedScripts.RDV_PREFIX,
        ).forEach { s ->
            listOf("빨리 가", "속도를 올리", "서둘러 운전").forEach { bad ->
                assertFalse("과속 유도 표현: $s", s.contains(bad))
            }
        }
    }
}
