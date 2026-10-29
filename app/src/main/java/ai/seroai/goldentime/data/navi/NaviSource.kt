package ai.seroai.goldentime.data.navi

/**
 * 내비 추상화 — NaviHelper SDK는 이 인터페이스 뒤에만 둔다.
 * PleosNaviSource: requestTrip→onTripInfo, requestRouteWithWaypoints, addWaypoint,
 *                  changeRouteOption(COMFORTABLE), onTBTInfo, onWayPointArrived
 */
interface NaviSource {
    /** 목적지까지 ETA(분) 조회. NaviHelper requestTrip */
    suspend fun etaMinutes(destination: Destination): Int?

    /** 경유지 추가. addWaypoint */
    suspend fun addWaypoint(destination: Destination): Boolean

    /** 편안한 경로 옵션. changeRouteOption(COMFORTABLE) */
    suspend fun setComfortRoute(): Boolean

    /** 다가오는 회전 정보(TBT) — 회전 예고용. onTBTInfo */
    fun nextTurn(): TurnInfo?
}

data class Destination(val name: String, val lat: Double, val lon: Double)

data class TurnInfo(
    val direction: TurnDirection,
    val distanceMeters: Int,
)

enum class TurnDirection(val announce: String) {
    LEFT("곧 왼쪽으로 돌아요"),
    RIGHT("곧 오른쪽으로 돌아요"),
}

/** 시연·개발용 가짜 구현 */
class FakeNaviSource : NaviSource {
    var etaByName: MutableMap<String, Int> = mutableMapOf()
    val waypoints = mutableListOf<Destination>()
    var comfortRoute = false
        private set
    var upcomingTurn: TurnInfo? = null

    override suspend fun etaMinutes(destination: Destination): Int? = etaByName[destination.name]

    override suspend fun addWaypoint(destination: Destination): Boolean {
        waypoints += destination
        return true
    }

    override suspend fun setComfortRoute(): Boolean {
        comfortRoute = true
        return true
    }

    override fun nextTurn(): TurnInfo? = upcomingTurn
}
