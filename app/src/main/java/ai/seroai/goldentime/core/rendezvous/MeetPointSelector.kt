package ai.seroai.goldentime.core.rendezvous

import kotlin.math.max

/**
 * 접선 후보 지점 — 졸음쉼터·휴게소에 더해 주유소·지구대.
 * 근거: 방콕 ALS 구급차 대기 거점에 주유소·경찰서 활용 (Sumrit 2020),
 * 덴마크 구급차-의사 중간 랑데부 운영 (Oelrich 2022).
 */
enum class MeetPointType(val label: String) {
    DROWSY_SHELTER("졸음쉼터"),
    REST_AREA("휴게소"),
    GAS_STATION("주유소"),
    POLICE("지구대"),
}

data class MeetPoint(
    val name: String,
    val type: MeetPointType,
    val carMin: Int,         // 자가용 → 지점 (NaviHelper ETA)
    val ambMin: Int,         // 구급차 → 지점
    val toHospitalMin: Int,  // 지점 → 병원 (구급차)
    val hasAed: Boolean,
) {
    val displayName get() = "$name ${type.label}"
}

/**
 * 응급 유형별 시간 기준선 — 랑데부 추천 규칙에 쓰인다 (진단이 아니라 탑승자가 말한 상황 분류).
 * - 경련: 발작 10분 넘어 첫 처치 시 결과 악화 (Gaínza-Lein 2018) → 만남 10분 목표
 * - 가슴 통증: 구급대 합류 + 병원 사전 통보가 유리 → 구급대가 포함된 선택지 우선
 * - 심정지: 계산하지 않고 즉시 정차·119·CPR (고정 대본)
 */
enum class EmergencyKind(val label: String, val contactTargetMin: Int?) {
    SEIZURE("경련", 10),
    CHEST_PAIN("가슴 통증", 15),
    CARDIAC_ARREST("심정지 의심", null),
    OTHER("기타 응급", 20),
}

object MeetPointSelector {
    /**
     * 만남 시간(둘 중 늦게 도착하는 쪽 + 인계) 최소 지점.
     * 동률이면 AED가 있는 곳, 그다음 병원까지 빠른 곳.
     */
    fun best(points: List<MeetPoint>, handover: Int): MeetPoint? =
        points.minWithOrNull(
            compareBy<MeetPoint> { contact(it, handover) }
                .thenBy { if (it.hasAed) 0 else 1 }
                .thenBy { it.toHospitalMin },
        )

    fun contact(p: MeetPoint, handover: Int) = max(p.carMin, p.ambMin) + handover

    /** 선택한 지점으로 랑데부 입력을 만든다 */
    fun toInput(base: RendezvousInput, p: MeetPoint) =
        base.copy(carToPoint = p.carMin, ambToPoint = p.ambMin, pointToHospital = p.toHospitalMin)

    /**
     * 유형별 추천:
     * 경련 등 즉시 처치 필요 → 만남 최소 ([RendezvousCalculator.compare] urgent).
     * 가슴 통증 → 구급대가 포함된 선택지(대기·접선) 중 만남 최소. 직접 이동은 병원 도착이 15분 이상 빠를 때만.
     */
    fun recommend(v: RendezvousInput, kind: EmergencyKind): List<RendezvousOption> {
        if (kind != EmergencyKind.CHEST_PAIN) {
            return RendezvousCalculator.compare(v, urgent = kind != EmergencyKind.OTHER)
        }
        val opts = RendezvousCalculator.compare(v, urgent = true).map { it.copy(recommended = false) }
        val ems = opts.filter { it.kind != RendezvousKind.DIRECT }
            .minWith(compareBy<RendezvousOption> { it.contact }.thenBy { it.hospital })
        val direct = opts.first { it.kind == RendezvousKind.DIRECT }
        val best = if (direct.hospital + 15 <= ems.hospital) direct else ems
        return opts.map { it.copy(recommended = it.kind == best.kind) }
    }

    /** 기준선 충족 여부 (null이면 기준 없음) */
    fun meetsTarget(option: RendezvousOption, kind: EmergencyKind): Boolean? =
        kind.contactTargetMin?.let { option.contact <= it }
}
