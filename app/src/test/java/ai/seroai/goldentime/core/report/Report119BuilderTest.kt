package ai.seroai.goldentime.core.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Report119BuilderTest {

    private val full = Report119Builder.Input(
        subject = "7살 남아",
        symptom = "온몸이 떨리고 부르는 말에 반응 없음",
        onset = "약 2분 전",
        history = "작년에 비슷한 증상 1회",
        action = "갓길 정차 중",
        roadDescription = "경부고속도로 부산 방향 371km",
        latitude = 36.12345,
        longitude = 127.54321,
        vehicle = "흰색 아이오닉5 12가3456",
    )

    @Test
    fun `전체 형식`() {
        assertEquals(
            "[골든타임 자동 요약] 7살 남아 온몸이 떨리고 부르는 말에 반응 없음, 약 2분 전 시작, " +
                "작년에 비슷한 증상 1회, 갓길 정차 중. " +
                "경부고속도로 부산 방향 371km 36.12345N 127.54321E, 흰색 아이오닉5 12가3456. " +
                "보호자 전화 연결 중",
            Report119Builder.build(full),
        )
    }

    @Test
    fun `모르는 항목은 모름`() {
        val minimal = Report119Builder.Input(
            subject = null, symptom = null, onset = null, history = null,
            action = null, roadDescription = null, latitude = null, longitude = null, vehicle = null,
        )
        val text = Report119Builder.build(minimal)
        assertTrue(text.startsWith("[골든타임 자동 요약] 모름 모름, 모름 시작, 모름, 모름."))
        assertTrue(text.contains("모름 모름, 모름. 보호자 전화 연결 중"))
    }

    @Test
    fun `진단어 검출`() {
        assertTrue(Report119Builder.containsDiagnosis("뇌졸중 의심"))
        assertTrue(Report119Builder.containsDiagnosis("심근경색인 것 같아요"))
        assertFalse(Report119Builder.containsDiagnosis("온몸이 떨리고 의식이 없음"))
        assertFalse(Report119Builder.containsDiagnosis("가슴을 움켜쥐고 아파함"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `증상에 진단어가 들어오면 거부`() {
        Report119Builder.build(full.copy(symptom = "뇌졸중 증상"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `병력에 진단어가 들어오면 거부`() {
        Report119Builder.build(full.copy(history = "심근경색 병력"))
    }
}
