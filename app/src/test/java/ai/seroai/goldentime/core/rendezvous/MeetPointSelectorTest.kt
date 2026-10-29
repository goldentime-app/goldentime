package ai.seroai.goldentime.core.rendezvous

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetPointSelectorTest {

    private val base = RendezvousInput(
        ambToHere = 20, carToHospital = 38, carToPoint = 7, ambToPoint = 6,
        pointToHospital = 25, hereToHospital = 29, handover = 2,
    )

    private val shelter = MeetPoint("○○", MeetPointType.DROWSY_SHELTER, carMin = 7, ambMin = 6, toHospitalMin = 25, hasAed = false)
    private val gas = MeetPoint("△△", MeetPointType.GAS_STATION, carMin = 5, ambMin = 9, toHospitalMin = 22, hasAed = false)
    private val police = MeetPoint("□□", MeetPointType.POLICE, carMin = 12, ambMin = 4, toHospitalMin = 20, hasAed = true)

    @Test
    fun `만남 시간 최소 지점 선택 - 졸음쉼터 9분`() {
        val p = MeetPointSelector.best(listOf(gas, police, shelter), handover = 2)
        assertEquals(shelter, p)
        assertEquals(9, MeetPointSelector.contact(shelter, 2))
    }

    @Test
    fun `만남 동률이면 AED 있는 곳`() {
        val policeTie = police.copy(carMin = 7, ambMin = 7)
        val p = MeetPointSelector.best(listOf(shelter, policeTie), handover = 2)
        assertEquals(policeTie, p)
    }

    @Test
    fun `후보가 없으면 null`() {
        assertEquals(null, MeetPointSelector.best(emptyList(), 2))
    }

    @Test
    fun `경련은 만남 최소 추천 + 10분 기준 충족`() {
        val opts = MeetPointSelector.recommend(base, EmergencyKind.SEIZURE)
        val rec = opts.first { it.recommended }
        assertEquals(RendezvousKind.MEET, rec.kind)
        assertTrue(MeetPointSelector.meetsTarget(rec, EmergencyKind.SEIZURE)!!)
        // 대기 22분은 기준 미충족
        assertFalse(MeetPointSelector.meetsTarget(opts.first { it.kind == RendezvousKind.WAIT }, EmergencyKind.SEIZURE)!!)
    }

    @Test
    fun `가슴 통증은 직접 이동이 크게 빠르지 않으면 구급대 포함 선택지`() {
        // 접선 9/34, 직접 38/38 → 접선
        val rec = MeetPointSelector.recommend(base, EmergencyKind.CHEST_PAIN).first { it.recommended }
        assertEquals(RendezvousKind.MEET, rec.kind)
    }

    @Test
    fun `가슴 통증이라도 직접 이동이 15분 이상 빠르면 직접`() {
        // 접선 만남 27, 병원 52 / 대기 22/51 / 직접 30/30 → 직접이 21분 빠름
        val far = base.copy(carToHospital = 30, carToPoint = 25, ambToPoint = 25)
        val rec = MeetPointSelector.recommend(far, EmergencyKind.CHEST_PAIN).first { it.recommended }
        assertEquals(RendezvousKind.DIRECT, rec.kind)
    }

    @Test
    fun `심정지는 기준선 없음`() {
        assertEquals(null, EmergencyKind.CARDIAC_ARREST.contactTargetMin)
    }
}
