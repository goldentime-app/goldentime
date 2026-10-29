package ai.seroai.goldentime.data.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context
import android.util.Log
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pleos FusedLocation SDK 1.1.3 — 위치·진행 방향 수신, 속도는 연속 위치 변화로 추정,
 * 접선 지점 반경 진입은 addProximityAlert로 감지 (E4 → 정차 안내·도어 해제 흐름).
 * 지원되지 않거나 위치가 안 들어오면 [fallback] 속도를 쓴다.
 */
class PleosLocationSource(context: Context, private val fallback: LocationSource) : LocationSource {
    companion object { private const val TAG = "PleosLocation" }

    private val fused = FusedLocation(context)
    private var last: LocationData? = null
    @Volatile private var estimatedKmh: Double? = null
    /** 한 번이라도 주행 속도가 관측됐는지 — 정지 상태의 에뮬레이터(GPS 고정)에서는 대체 속도로 시연 */
    @Volatile private var moved = false
    @Volatile var latest: LocationData? = null
        private set

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            location ?: return
            last?.let { prev ->
                val dt = (location.timestampMillis - prev.timestampMillis) / 1000.0
                if (dt in 0.2..10.0) {
                    val kmh = distanceMeters(prev, location) / dt * 3.6
                    if (kmh < 250) {
                        estimatedKmh = kmh
                        if (kmh > 3) moved = true
                    }
                }
            }
            last = location
            latest = location
        }

        override fun onError(exception: Exception) {
            Log.w(TAG, "location error", exception)
        }
    }

    /** SDK 지원 + 초기화 성공 시 true */
    fun start(): Boolean = try {
        if (!fused.isSupported()) false else {
            fused.initialize()
            fused.registerFusedLocationCallback(listener)
            Log.i(TAG, "FusedLocation started")
            true
        }
    } catch (e: Exception) {
        Log.w(TAG, "FusedLocation unavailable", e)
        false
    }

    /** 실제 이동이 관측된 뒤에는 위치 기반 추정 속도, 그 전에는 대체 속도 */
    override fun speedKmh(): Double? = if (moved) estimatedKmh else fallback.speedKmh()

    /** 접선 지점 반경 진입 알림 — 진입 시 [onEnter] */
    fun watchMeetPoint(lat: Double, lon: Double, radiusMeters: Float, onEnter: () -> Unit) {
        try {
            fused.addProximityAlert(
                ProximityAlertRequest(lat, lon, radiusMeters, setOf(ProximityTransition.ENTER)),
                object : ProximityAlertListener {
                    override fun onRegistered(alertId: String) { Log.i(TAG, "meet alert $alertId") }
                    override fun onTransition(event: ProximityAlertEvent) {
                        if (event.transition == ProximityTransition.ENTER) onEnter()
                    }
                    override fun onError(exception: Exception) { Log.w(TAG, "meet alert error", exception) }
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "addProximityAlert failed", e)
        }
    }

    fun stop() {
        try {
            fused.unregisterFusedLocationCallback(listener)
            fused.release()
        } catch (e: Exception) {
            Log.w(TAG, "release failed", e)
        }
    }

    private fun distanceMeters(a: LocationData, b: LocationData): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }
}
