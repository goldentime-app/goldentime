package ai.seroai.goldentime.core.stop

/**
 * 안전 정차 지점 안내 — "안전한 곳에 바로 멈추세요"에 구체적인 곳을 붙인다.
 * 고속도로 갓길 정차는 2차 사고 위험이 커서 후보에서 뺀다.
 *
 * 선택 기준은 '경로 앞 거리'뿐이다. 차량 속도·가속도·주변 물체로 지점별 위험도를 매기지 않는다
 * (그런 위험도 최소화 선택은 선행 특허 US 11,279,347의 청구 범위). 안내만 하고 차량을 제어하지 않는다.
 */
enum class SafeStopType(val label: String) {
    EMERGENCY_BAY("비상주차대"),
    DROWSY_SHELTER("졸음쉼터"),
    REST_AREA("휴게소"),
}

data class SafeStop(
    val name: String?,
    val type: SafeStopType,
    /** 경로 앞 거리 (m). 지나친 곳은 음수 */
    val aheadMeters: Int,
) {
    val displayName get() = listOfNotNull(name, type.label).joinToString(" ")
}

object SafeStopPlanner {
    /** 이보다 가까우면 이미 진입하기 어려움 — 고정 거리 (속도와 무관) */
    const val MIN_LEAD_METERS = 150

    /** 화면에 보여줄 개수 (주행 중 화면 원칙: 짧게) */
    const val MAX_SHOWN = 2

    /** 경로 앞에 있고 진입 가능한 거리 이상인 곳을 가까운 순으로 */
    fun ahead(stops: List<SafeStop>): List<SafeStop> =
        stops.filter { it.aheadMeters >= MIN_LEAD_METERS }
            .sortedBy { it.aheadMeters }
            .take(MAX_SHOWN)

    fun distanceText(m: Int): String =
        if (m < 1000) "${(m / 50) * 50}m 앞" else "%.1fkm 앞".format(m / 1000.0)
}
