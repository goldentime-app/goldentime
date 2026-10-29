package ai.seroai.goldentime.core.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 경로 예보가 프로토타입(prototype/index.html)과 같은 숫자를 내야 한다.
 * 기준: dt=0.2, 부드러움 60 —
 *   추천 경로  [120분 100km/h 커브0.5, 38분 55km/h 커브8] ≈ MSDV 45
 *   편안한 경로 [140분 100km/h 커브0.5,  9분 60km/h 커브5] ≈ MSDV 23
 */
class RouteForecastTest {

    private val fast = listOf(
        RouteSegment(120, 100.0, 0.5),
        RouteSegment(38, 55.0, 8.0),
    )
    private val comfy = listOf(
        RouteSegment(140, 100.0, 0.5),
        RouteSegment(9, 60.0, 5.0),
    )

    @Test
    fun `추천 경로 예상 MSDV 약 45`() {
        assertEquals(45.0, RouteForecast.msdv(fast), 1.0)
    }

    @Test
    fun `편안한 경로 예상 MSDV 약 23`() {
        assertEquals(23.0, RouteForecast.msdv(comfy), 1.0)
    }

    @Test
    fun `편안한 경로가 추천 경로보다 노출이 낮다`() {
        assertTrue(RouteForecast.msdv(comfy) < RouteForecast.msdv(fast))
    }
}
