package ai.seroai.goldentime.core.rest

import ai.seroai.goldentime.core.motion.Occupant

/**
 * 휴식 권장 계산 — 아동 동승 장거리 주행 케어.
 *
 * 근거(가설로 명시, 문헌 재확인 필요 — docs/골든타임_멀미모델_근거문헌.md에 추가 예정):
 * - 유아·아동은 카시트 장시간 착좌 부담 연구에 따라 "2시간마다 휴식" 규칙이 널리 권장됨.
 * - 멀미 노출(MSDV)이 높아지면 휴식을 앞당긴다.
 * 진단하지 않는다 — 휴식 "권장" 안내만 한다 (절대 원칙 1).
 */
object RestPlanner {

    /** 탑승자별 연속 주행 권장 한도(분) — 가설 초기값 */
    fun recommendedIntervalMinutes(occupant: Occupant): Int = when (occupant) {
        Occupant.CHILD -> 120   // 2시간 규칙
        Occupant.ADULT -> 150
        Occupant.DRIVER -> 120  // 졸음 예방 권장과 동일
    }

    /**
     * 다음 휴식까지 남은 권장 시간(분).
     * 멀미 위험도 70% 이상이면 0(지금 쉬기), 40% 이상이면 간격 25% 단축.
     */
    fun minutesUntilRest(drivenMinutes: Int, occupant: Occupant, riskPercent: Int): Int {
        if (riskPercent >= 70) return 0
        var interval = recommendedIntervalMinutes(occupant)
        if (riskPercent >= 40) interval = interval * 3 / 4
        return (interval - drivenMinutes).coerceAtLeast(0)
    }

    /** 휴식을 권할 때가 됐는가 (H0 휴식 카드 강조·음성 안내 트리거) */
    fun shouldSuggestRest(drivenMinutes: Int, occupant: Occupant, riskPercent: Int): Boolean =
        minutesUntilRest(drivenMinutes, occupant, riskPercent) == 0
}
