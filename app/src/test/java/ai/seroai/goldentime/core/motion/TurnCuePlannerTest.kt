package ai.seroai.goldentime.core.motion

import ai.seroai.goldentime.data.navi.TurnDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnCuePlannerTest {

    @Test
    fun `60km_h에서 80m 앞 커브는 5초 전 예고`() {
        // 60km/h = 16.7m/s → 80m는 4.8초
        val cue = TurnCuePlanner.cue(TurnDirection.LEFT, 80, 60.0)
        assertEquals(TurnCue(TurnDirection.LEFT, 5), cue)
    }

    @Test
    fun `너무 멀면 아직 예고하지 않음`() {
        assertNull(TurnCuePlanner.cue(TurnDirection.RIGHT, 300, 60.0)) // 18초
    }

    @Test
    fun `이미 회전 직전이면 예고하지 않음`() {
        assertNull(TurnCuePlanner.cue(TurnDirection.RIGHT, 10, 60.0)) // 0.6초
    }

    @Test
    fun `정체 구간 저속에서는 예고하지 않음`() {
        assertNull(TurnCuePlanner.cue(TurnDirection.LEFT, 20, 10.0))
    }
}
