package ai.seroai.goldentime.emergency

/**
 * 응급 발화 라우팅: 심정지 의심 키워드면 E6 (랑데부 계산 생략, 즉시 119 전송),
 * 그 외 응급 발화는 E1 (음성 접수, 질문은 한 번에 하나).
 * 병명을 추정하지 않는다 — 키워드는 행동 분기용일 뿐이다 (절대 원칙 1).
 */
object EmergencyRouter {

    /** 심정지 의심으로 보는 표현 (검수 목록) */
    private val ARREST_KEYWORDS = listOf(
        "숨을 안 쉬", "숨 안 쉬", "숨을 쉬지 않", "숨 쉬지 않",
        "심정지", "맥박이 없", "맥박 없", "심장이 멈",
    )

    fun route(utterance: String): Screen =
        if (ARREST_KEYWORDS.any { utterance.contains(it) }) Screen.E6 else Screen.E1
}
