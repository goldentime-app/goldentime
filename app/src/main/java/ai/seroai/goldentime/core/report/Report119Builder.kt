package ai.seroai.goldentime.core.report

/**
 * 119 요약 문자 생성 (절대 원칙 1·4).
 *
 * 형식:
 * [골든타임 자동 요약] {대상} {증상}, {시작} 시작, {병력}, {조치}. {도로명 방향 km} {위도}N {경도}E, {차량}. 보호자 전화 연결 중
 *
 * 6개 항목만(대상, 증상·시작, 병력, 위치·좌표, 차량, 조치). 모르면 "모름". 진단어 금지.
 */
object Report119Builder {

    const val UNKNOWN = "모름"

    /**
     * 병명 추정 단어 — 요약에 들어가면 안 된다 (증상 서술만 허용).
     * 검수된 목록. 필요 시 docs와 함께 갱신.
     */
    val DIAGNOSIS_WORDS = listOf(
        "뇌졸중", "뇌출혈", "뇌경색", "심근경색", "심장마비", "협심증",
        "패혈증", "간질", "뇌전증", "저혈당 쇼크", "아나필락시스",
        "골절", "뇌진탕", "장염", "맹장염", "충수염", "폐렴",
    )

    fun containsDiagnosis(text: String): Boolean =
        DIAGNOSIS_WORDS.any { text.contains(it) }

    data class Input(
        val subject: String?,     // 대상: "7살 남아" 등
        val symptom: String?,     // 증상: "온몸이 떨리고 의식이 없음" (진단어 금지)
        val onset: String?,       // 시작: "약 2분 전"
        val history: String?,     // 병력: "열성경련 병력 있음" 등 보호자 진술
        val action: String?,      // 조치: "갓길 정차 중"
        val roadDescription: String?, // 위치: "경부고속도로 부산 방향 371km"
        val latitude: Double?,
        val longitude: Double?,
        val vehicle: String?,     // 차량: "흰색 아이오닉5 12가3456"
    )

    /** 진단어가 들어 있으면 IllegalArgumentException. 모르는 항목은 "모름". */
    fun build(i: Input): String {
        listOfNotNull(i.subject, i.symptom, i.history, i.action).forEach {
            require(!containsDiagnosis(it)) { "진단어 금지: \"$it\"" }
        }
        val subject = i.subject ?: UNKNOWN
        val symptom = i.symptom ?: UNKNOWN
        val onset = i.onset ?: UNKNOWN
        val history = i.history ?: UNKNOWN
        val action = i.action ?: UNKNOWN
        val road = i.roadDescription ?: UNKNOWN
        val vehicle = i.vehicle ?: UNKNOWN
        val coords = if (i.latitude != null && i.longitude != null) {
            "%.5f".format(i.latitude) + "N " + "%.5f".format(i.longitude) + "E"
        } else {
            UNKNOWN
        }
        return "[골든타임 자동 요약] $subject $symptom, $onset 시작, $history, $action. " +
            "$road $coords, $vehicle. 보호자 전화 연결 중"
    }
}
