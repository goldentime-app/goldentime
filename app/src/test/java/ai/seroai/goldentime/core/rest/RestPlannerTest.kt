package ai.seroai.goldentime.core.rest

import ai.seroai.goldentime.core.motion.Occupant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestPlannerTest {

    @Test
    fun `아이 동승은 2시간 규칙`() {
        assertEquals(120, RestPlanner.recommendedIntervalMinutes(Occupant.CHILD))
        // 96분 주행, 위험도 32% → 24분 뒤 휴식
        assertEquals(24, RestPlanner.minutesUntilRest(96, Occupant.CHILD, 32))
    }

    @Test
    fun `멀미 위험 70 이상이면 지금 쉬기`() {
        assertEquals(0, RestPlanner.minutesUntilRest(10, Occupant.CHILD, 70))
        assertTrue(RestPlanner.shouldSuggestRest(10, Occupant.CHILD, 75))
    }

    @Test
    fun `위험 40 이상이면 간격 25퍼센트 단축`() {
        // 120 → 90분 간격, 60분 주행 → 30분 뒤
        assertEquals(30, RestPlanner.minutesUntilRest(60, Occupant.CHILD, 45))
        assertFalse(RestPlanner.shouldSuggestRest(60, Occupant.CHILD, 45))
    }

    @Test
    fun `한도를 넘겨도 음수가 되지 않는다`() {
        assertEquals(0, RestPlanner.minutesUntilRest(130, Occupant.CHILD, 10))
        assertTrue(RestPlanner.shouldSuggestRest(130, Occupant.CHILD, 10))
    }
}
