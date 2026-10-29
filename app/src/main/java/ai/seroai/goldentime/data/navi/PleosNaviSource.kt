package ai.seroai.goldentime.data.navi

import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * NaviHelper SDK 3.0.5 실제 구현 (API는 AAR에서 직접 확인).
 * requestTrip→onTripInfo(ETA), addWaypoint, changeRouteOption(COMFORTABLE), onTBTInfo.
 * 권한: pleos.car.permission.NAVI_ROUTE, NAVI_ROUTE_SEARCH (매니페스트).
 *
 * TODO(설명회·문서 확인): TripInfo.eta 단위(분/초) 검증 — 현재 분으로 가정.
 */
class PleosNaviSource(context: Context) : NaviSource {

    companion object {
        private const val TAG = "PleosNaviSource"
        private const val RESPONSE_TIMEOUT_MS = 10_000L
    }

    private val helper = NaviHelper(context)
    private var pendingTrip: CompletableDeferred<TripInfo>? = null
    @Volatile private var lastTurn: TurnInfo? = null

    private val listener = object : NaviHelperEventListener {
        override fun onTripInfo(info: TripInfo) {
            pendingTrip?.complete(info)
        }

        override fun onTBTInfo(infos: List<TBTInfo>) {
            // 가장 가까운 좌/우 회전만 회전 예고(멀미 케어)에 사용
            val next = infos.firstOrNull { it.type.toDirection() != null } ?: return
            lastTurn = TurnInfo(next.type.toDirection()!!, next.distance.toInt())
        }
    }

    fun start(): Boolean = try {
        helper.initialize(
            { Log.i(TAG, "NaviHelper initialized") },
            { e -> Log.w(TAG, "NaviHelper init failed", e) },
        )
        helper.addListener(listener)
        true
    } catch (e: Exception) {
        Log.w(TAG, "NaviHelper start failed — Fake로 대체하세요", e)
        false
    }

    fun stop() {
        try {
            helper.removeListener(listener)
        } finally {
            helper.release()
        }
    }

    override suspend fun etaMinutes(destination: Destination): Int? {
        val deferred = CompletableDeferred<TripInfo>()
        pendingTrip = deferred
        return try {
            helper.requestTrip(
                TripRequest(destination.lat, destination.lon, "", destination.name, ""),
            )
            withTimeoutOrNull(RESPONSE_TIMEOUT_MS) { deferred.await() }?.eta
        } catch (e: Exception) {
            Log.w(TAG, "requestTrip failed", e)
            null
        } finally {
            pendingTrip = null
        }
    }

    override suspend fun addWaypoint(destination: Destination): Boolean = try {
        helper.addWaypoint(
            RequestWaypointInfo(
                destination.lat, destination.lon, destination.name,
                WaypointIndex.FIRST, "", "", "", "",
            ),
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "addWaypoint failed", e)
        false
    }

    override suspend fun setComfortRoute(): Boolean = try {
        helper.changeRouteOption(RouteOption.COMFORTABLE)
        true
    } catch (e: Exception) {
        Log.w(TAG, "changeRouteOption failed", e)
        false
    }

    override fun nextTurn(): TurnInfo? = lastTurn
}

private fun TurnType.toDirection(): TurnDirection? = when (this) {
    TurnType.TURN_LEFT, TurnType.TURN_SHARP_L, TurnType.TURN_KEEP_L -> TurnDirection.LEFT
    TurnType.TURN_RIGHT, TurnType.TURN_SHARP_R, TurnType.TURN_KEEP_R -> TurnDirection.RIGHT
    else -> null
}
