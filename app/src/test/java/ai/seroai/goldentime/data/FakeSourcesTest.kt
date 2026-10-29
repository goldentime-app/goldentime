package ai.seroai.goldentime.data

import ai.seroai.goldentime.data.navi.Destination
import ai.seroai.goldentime.data.navi.FakeNaviSource
import ai.seroai.goldentime.data.vehicle.FakeVehicleSource
import ai.seroai.goldentime.data.vehicle.VehicleCapability
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSourcesTest {

    @Test
    fun `주행 중에는 도어 잠금 해제 거부(절대 원칙 6)`() = runTest {
        val v = FakeVehicleSource()
        v.parked = false
        assertFalse(v.unlockDoors())
        v.parked = true
        assertTrue(v.unlockDoors())
        assertTrue(v.doorsUnlocked)
    }

    @Test
    fun `미지원 기능은 실패 반환(음성 안내만)`() = runTest {
        val v = FakeVehicleSource()
        v.supported = emptySet()
        v.parked = true
        assertFalse(v.unlockDoors())
        assertFalse(v.setEmergencyLights(true))
        assertFalse(v.isSupported(VehicleCapability.DOOR_UNLOCK))
    }

    @Test
    fun `내비 경유지 추가와 편안한 경로`() = runTest {
        val n = FakeNaviSource()
        n.etaByName["졸음쉼터"] = 5
        assertEquals(5, n.etaMinutes(Destination("졸음쉼터", 37.4, 127.6)))
        assertTrue(n.addWaypoint(Destination("휴게소", 37.3, 127.5)))
        assertEquals(1, n.waypoints.size)
        assertTrue(n.setComfortRoute())
        assertTrue(n.comfortRoute)
    }
}
