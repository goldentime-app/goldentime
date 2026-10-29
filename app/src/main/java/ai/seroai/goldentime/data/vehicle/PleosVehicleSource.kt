package ai.seroai.goldentime.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.SeatAirbagDeployedListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Pleos Vehicle SDK 2.3.2 실제 구현.
 * API 시그니처는 SDK AAR에서 직접 확인 (Vehicle(context).initialize(), getSteeringWheel() 등).
 * 필요 권한(조회): pleos.car.permission.CAR_DRIVE(핸들각·브레이크), CAR_SEATS(착좌), CAR_SAFETY(에어백).
 *
 * - 제어(도어·조명)는 Vehicle Control 준비 중(신청 불가)이므로 미지원 처리 → 음성 안내만 (절대 원칙 6).
 * - start() 실패 시 호출부에서 FakeVehicleSource로 대체한다.
 */
class PleosVehicleSource(context: Context) : VehicleSource {

    companion object {
        private const val TAG = "PleosVehicleSource"
    }

    private val vehicle = Vehicle(context)

    private val steeringState = MutableStateFlow(0.0)
    private val brakeState = MutableStateFlow(0.0)
    private val seatState = MutableStateFlow(SeatOccupancy())
    private val airbagState = MutableStateFlow(false)

    override val steeringAngle: Flow<Double> = steeringState
    override val brakePedal: Flow<Double> = brakeState
    override val seatOccupancy: Flow<SeatOccupancy> = seatState
    override val airbagDeployed: Flow<Boolean> = airbagState

    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) {
            angle?.let { steeringState.value = it.toDouble() }
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "steering listener failed", e)
        }
    }

    // SDK는 브레이크를 Boolean(답력 유무)으로 준다 → 0/100으로 매핑
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) {
            pressed?.let { brakeState.value = if (it) 100.0 else 0.0 }
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "brake listener failed", e)
        }
    }

    private val driverSeatListener = seatListener { occ ->
        seatState.value = seatState.value.copy(driver = occ)
    }
    private val passengerSeatListener = seatListener { occ ->
        seatState.value = seatState.value.copy(passenger = occ)
    }

    // 복합 area(예: ROW_2_LEFT|ROW_2_RIGHT)는 SEAT_OCCUPANCY에서 미지원 → 좌석별 개별 등록
    private var rearLeftOccupied = false
    private var rearRightOccupied = false
    private val rearLeftListener = seatListener { occ ->
        rearLeftOccupied = occ
        seatState.value = seatState.value.copy(rear = rearLeftOccupied || rearRightOccupied)
    }
    private val rearRightListener = seatListener { occ ->
        rearRightOccupied = occ
        seatState.value = seatState.value.copy(rear = rearLeftOccupied || rearRightOccupied)
    }

    private fun seatListener(onUpdate: (Boolean) -> Unit) = object : SeatOccupancyListener {
        override fun onSeatOccupancyUpdated(occupied: Boolean?) {
            occupied?.let(onUpdate)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "seat listener failed", e)
        }
    }

    private val airbagListener = object : SeatAirbagDeployedListener {
        override fun onSeatAirbagDeployUpdated(deployed: Boolean?) {
            if (deployed == true) airbagState.value = true
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "airbag listener failed", e)
        }
    }

    private val driverArea = SeatArea(SeatValue.ROW_1_LEFT)
    private val passengerArea = SeatArea(SeatValue.ROW_1_RIGHT)
    private val rearLeftArea = SeatArea(SeatValue.ROW_2_LEFT)
    private val rearRightArea = SeatArea(SeatValue.ROW_2_RIGHT)

    /** SDK 초기화 + 리스너 등록. 예외 없이 반환되면 true (연결 보장은 아님 — 공식 문서 기준). */
    fun start(): Boolean = try {
        vehicle.initialize()
        vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener)
        vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        vehicle.getSeat().registerSeatOccupancy(driverArea, driverSeatListener)
        vehicle.getSeat().registerSeatOccupancy(passengerArea, passengerSeatListener)
        vehicle.getSeat().registerSeatOccupancy(rearLeftArea, rearLeftListener)
        vehicle.getSeat().registerSeatOccupancy(rearRightArea, rearRightListener)
        vehicle.getSafety().registerSeatAirbagDeployed(driverArea, airbagListener)
        // 현재값 1회 조회
        vehicle.getSteeringWheel().getCurrentSteeringWheelAngle(
            { it?.let { v -> steeringState.value = v.toDouble() } },
            { e -> Log.w(TAG, "steering angle read failed", e) },
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "Vehicle SDK init failed — Fake로 대체하세요", e)
        false
    }

    fun stop() {
        try {
            // 등록했던 동일 객체·area로 해제 후 release (공식 가이드)
            vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener)
            vehicle.getBrake().unregisterBrakePedalStatus(brakeListener)
            vehicle.getSeat().unregisterSeatOccupancy(driverArea, driverSeatListener)
            vehicle.getSeat().unregisterSeatOccupancy(passengerArea, passengerSeatListener)
            vehicle.getSeat().unregisterSeatOccupancy(rearLeftArea, rearLeftListener)
            vehicle.getSeat().unregisterSeatOccupancy(rearRightArea, rearRightListener)
            vehicle.getSafety().unregisterSeatAirbagDeployed(driverArea, airbagListener)
        } catch (e: Exception) {
            Log.w(TAG, "unregister failed", e)
        } finally {
            vehicle.release()
        }
    }

    // Vehicle Control SDK는 준비 중(신청 불가) → 제어는 모두 미지원, 음성 안내만 (절대 원칙 6)
    override fun isSupported(capability: VehicleCapability): Boolean = false

    override suspend fun unlockDoors(): Boolean = false

    override suspend fun setEmergencyLights(on: Boolean): Boolean = false
}
