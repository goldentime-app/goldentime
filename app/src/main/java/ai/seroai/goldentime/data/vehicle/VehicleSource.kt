package ai.seroai.goldentime.data.vehicle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 신호 추상화 — Pleos Vehicle SDK는 이 인터페이스 뒤에만 둔다 (작업 방식 원칙).
 * PleosVehicleSource: Vehicle State SDK (SteeringWheel, Brake, Seat, Safety, Odometer)
 * FakeVehicleSource: 에뮬레이터 없이 화면·시연 모드 구동용.
 */
interface VehicleSource {
    /** 핸들 각도 (도, -540~540). SteeringWheel registerSteeringWheelAngle */
    val steeringAngle: Flow<Double>

    /** 브레이크 답력 (0~100%). Brake registerBrakePedalStatus */
    val brakePedal: Flow<Double>

    /** 좌석별 착좌 (운전석/조수석/뒷좌석). Seat occupancy */
    val seatOccupancy: Flow<SeatOccupancy>

    /** 에어백 전개. Safety registerSeatAirbagDeployed — Virtual Console에 카드 없음 → 시연 모드로 주입 */
    val airbagDeployed: Flow<Boolean>

    /** 지원 기능 조회 — 미지원이면 음성 안내만 (절대 원칙 6) */
    fun isSupported(capability: VehicleCapability): Boolean

    /** 도어 잠금 해제 — 정차 확인 후에만 (절대 원칙 6). Vehicle Control 준비 중이라 기본 미지원 */
    suspend fun unlockDoors(): Boolean

    /** 실내등·앰비언트 응급색 — Vehicle Control 준비 중이라 기본 미지원 */
    suspend fun setEmergencyLights(on: Boolean): Boolean
}

enum class VehicleCapability { DOOR_UNLOCK, CABIN_LIGHT, AMBIENT_LIGHT }

data class SeatOccupancy(
    val driver: Boolean = true,
    val passenger: Boolean = false,
    val rear: Boolean = false,
)

/** 시연·개발용 가짜 구현. 시연 모드에서 값을 주입한다. */
class FakeVehicleSource : VehicleSource {
    val steeringAngleState = MutableStateFlow(0.0)
    val brakePedalState = MutableStateFlow(0.0)
    val seatOccupancyState = MutableStateFlow(SeatOccupancy(driver = true, rear = true))
    val airbagState = MutableStateFlow(false)

    override val steeringAngle: StateFlow<Double> = steeringAngleState
    override val brakePedal: StateFlow<Double> = brakePedalState
    override val seatOccupancy: StateFlow<SeatOccupancy> = seatOccupancyState
    override val airbagDeployed: StateFlow<Boolean> = airbagState

    var supported: Set<VehicleCapability> = setOf(
        VehicleCapability.DOOR_UNLOCK, VehicleCapability.CABIN_LIGHT, VehicleCapability.AMBIENT_LIGHT,
    )
    var parked = false
    var doorsUnlocked = false
        private set
    var emergencyLightsOn = false
        private set

    override fun isSupported(capability: VehicleCapability) = capability in supported

    override suspend fun unlockDoors(): Boolean {
        // 정차 확인 후에만 (절대 원칙 6)
        if (!parked || !isSupported(VehicleCapability.DOOR_UNLOCK)) return false
        doorsUnlocked = true
        return true
    }

    override suspend fun setEmergencyLights(on: Boolean): Boolean {
        if (!isSupported(VehicleCapability.CABIN_LIGHT)) return false
        emergencyLightsOn = on
        return true
    }
}
