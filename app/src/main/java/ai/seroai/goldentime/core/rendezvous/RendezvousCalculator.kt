package ai.seroai.goldentime.core.rendezvous

import kotlin.math.max

/**
 * 구급차 랑데부 비교 — 프로토타입 rdv()와 수치 동일해야 함.
 * 모든 입력은 분 단위. 결과는 참고 정보이며 최종 결정은 119 (절대 원칙 2).
 *
 * 실제 입력: 자가용 = NaviHelper requestTrip ETA, 구급차 = 119안전센터 위치 기반 추정.
 */
data class RendezvousInput(
    val ambToHere: Int,      // 구급차 → 현 위치
    val carToHospital: Int,  // 자가용 → 병원 직행
    val carToPoint: Int,     // 자가용 → 접선 지점
    val ambToPoint: Int,     // 구급차 → 접선 지점
    val pointToHospital: Int, // 접선 지점 → 병원 (구급차)
    val hereToHospital: Int,  // 현 위치 → 병원 (구급차)
    val handover: Int,        // 인계 시간
)

enum class RendezvousKind(val label: String) {
    WAIT("그 자리에서 대기"),
    DIRECT("직접 병원으로"),
    MEET("접선 지점에서 구급차 접선"),
}

data class RendezvousOption(
    val kind: RendezvousKind,
    /** 의료진을 만나기까지 (분) */
    val contact: Int,
    /** 병원 도착까지 (분) */
    val hospital: Int,
    val recommended: Boolean = false,
)

object RendezvousCalculator {
    /** 비긴급 추천에서 "만남이 충분히 빠르다"로 보는 상한 (분) */
    const val CONTACT_OK_MINUTES = 20

    /**
     * 추천 규칙:
     * 즉시 처치 필요(urgent) → 만남 최소 (동률이면 병원 최소).
     * 아니면 만남 ≤ 20분인 것 중 병원 최소 (없으면 전체 중 병원 최소).
     */
    fun compare(v: RendezvousInput, urgent: Boolean): List<RendezvousOption> {
        val wait = RendezvousOption(
            RendezvousKind.WAIT,
            contact = v.ambToHere + v.handover,
            hospital = v.ambToHere + v.handover + v.hereToHospital,
        )
        val direct = RendezvousOption(
            RendezvousKind.DIRECT,
            contact = v.carToHospital,
            hospital = v.carToHospital,
        )
        val meetContact = max(v.carToPoint, v.ambToPoint) + v.handover
        val meet = RendezvousOption(
            RendezvousKind.MEET,
            contact = meetContact,
            hospital = meetContact + v.pointToHospital,
        )
        val opts = listOf(wait, direct, meet)

        val best = if (urgent) {
            opts.reduce { a, b ->
                if (b.contact < a.contact || (b.contact == a.contact && b.hospital < a.hospital)) b else a
            }
        } else {
            val ok = opts.filter { it.contact <= CONTACT_OK_MINUTES }
            (ok.ifEmpty { opts }).reduce { a, b -> if (b.hospital < a.hospital) b else a }
        }
        return opts.map { it.copy(recommended = it === best) }
    }
}
