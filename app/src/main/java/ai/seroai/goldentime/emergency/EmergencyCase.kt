package ai.seroai.goldentime.emergency

import ai.seroai.goldentime.core.report.Report119Builder

/**
 * 응급 사례 데이터 — E2 화면 표시와 119 문자 본문(Report119Builder)의 단일 출처.
 * 실서비스에서는 E1 음성 접수·가족 프로필·위치에서 채워진다.
 */
data class EmergencyCase(
    val subject: String,
    val symptom: String,      // 진단어 금지 — Report119Builder가 검사
    val onset: String,
    val history: String,
    val action: String,
    val roadDescription: String,
    val latitude: Double,
    val longitude: Double,
    val vehicle: String,
) {
    fun toReportInput() = Report119Builder.Input(
        subject = subject, symptom = symptom, onset = onset, history = history,
        action = action, roadDescription = roadDescription,
        latitude = latitude, longitude = longitude, vehicle = vehicle,
    )

    /** 119 문자신고 본문 (절대 원칙 1·4 검사 포함) */
    fun smsText(): String = Report119Builder.build(toReportInput())

    companion object {
        /** 시연: 시나리오 A — 아이 경련 */
        val SEIZURE = EmergencyCase(
            subject = "4세 남아 (뒷좌석)",
            symptom = "경련, 발열",
            onset = "약 2분 전",
            history = "열성경련 이력",
            action = "옆으로 눕힘",
            roadDescription = "영동고속도로 강릉방향 123km",
            latitude = 37.43848, longitude = 128.018,
            vehicle = "흰색 SUV · ○○가 1234",
        )

        /** 시연: 시나리오 B — 충돌 무응답 */
        val CRASH = EmergencyCase(
            subject = "운전자·동승자",
            symptom = "충돌 감지, 응답 없음",
            onset = "방금",
            history = "모름",
            action = "없음",
            roadDescription = "영동고속도로 강릉방향 123km",
            latitude = 37.43848, longitude = 128.018,
            vehicle = "흰색 SUV · 탑승 3명",
        )
    }
}
