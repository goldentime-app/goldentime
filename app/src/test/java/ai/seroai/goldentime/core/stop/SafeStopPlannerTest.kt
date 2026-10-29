package ai.seroai.goldentime.core.stop

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeStopPlannerTest {

    private val bay = SafeStop(null, SafeStopType.EMERGENCY_BAY, 420)
    private val shelter = SafeStop("○○", SafeStopType.DROWSY_SHELTER, 2100)
    private val rest = SafeStop("△△", SafeStopType.REST_AREA, 8600)

    @Test
    fun `가까운 순 두 곳`() {
        assertEquals(listOf(bay, shelter), SafeStopPlanner.ahead(listOf(rest, shelter, bay)))
    }

    @Test
    fun `너무 가깝거나 지나친 곳은 제외`() {
        val tooClose = SafeStop(null, SafeStopType.EMERGENCY_BAY, 80)
        val passed = SafeStop(null, SafeStopType.EMERGENCY_BAY, -300)
        assertEquals(listOf(shelter, rest), SafeStopPlanner.ahead(listOf(tooClose, passed, rest, shelter)))
    }

    @Test
    fun `후보가 없으면 빈 목록`() {
        assertEquals(emptyList<SafeStop>(), SafeStopPlanner.ahead(emptyList()))
    }

    @Test
    fun `거리 표기`() {
        assertEquals("400m 앞", SafeStopPlanner.distanceText(420))
        assertEquals("2.1km 앞", SafeStopPlanner.distanceText(2100))
    }

    @Test
    fun `표시 이름`() {
        assertEquals("비상주차대", bay.displayName)
        assertEquals("○○ 졸음쉼터", shelter.displayName)
    }
}
