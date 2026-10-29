package ai.seroai.goldentime.data.hospital

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 가까운 자동심장충격기 — 심정지 의심(E6)에서 동승자 안내 */
data class AedSpot(
    val org: String,
    /** 건물 안 설치 위치 (예: "1층 로비") */
    val place: String?,
    val distanceKm: Double?,
    val live: Boolean,
)

interface AedSource {
    suspend fun nearest(lat: Double, lon: Double): AedSpot?
}

/** 시연용 */
class FakeAedSource : AedSource {
    override suspend fun nearest(lat: Double, lon: Double) = AedSpot("□□ 지구대", null, 1.2, live = false)
}

/**
 * 국립중앙의료원 전국 자동심장충격기(AED) 정보 조회 서비스 (B552657/AEDInfoInqireService)
 * getAedLcinfoInqire: WGS84_LON·WGS84_LAT 기준 주변 AED (org 설치기관, buildPlace 설치 위치, wgs84Lat·wgs84Lon).
 * 거리는 좌표로 직접 계산해 가장 가까운 곳을 고른다. 실패하면 [fallback].
 */
class NemcAedSource(
    private val serviceKey: String,
    private val fallback: AedSource = FakeAedSource(),
) : AedSource {
    companion object {
        private const val TAG = "NemcAed"
        private const val URL = "https://apis.data.go.kr/B552657/AEDInfoInqireService/getAedLcinfoInqire"
    }

    override suspend fun nearest(lat: Double, lon: Double): AedSpot? = withContext(Dispatchers.IO) {
        try {
            val items = NemcApi.items(
                URL,
                mapOf("WGS84_LON" to "$lon", "WGS84_LAT" to "$lat", "pageNo" to "1", "numOfRows" to "10"),
                serviceKey,
            )
            items.mapNotNull { it ->
                val aLat = it["wgs84Lat"]?.toDoubleOrNull() ?: return@mapNotNull null
                val aLon = it["wgs84Lon"]?.toDoubleOrNull() ?: return@mapNotNull null
                AedSpot(
                    org = it["org"].orEmpty(),
                    place = it["buildPlace"]?.takeIf { p -> p.isNotBlank() },
                    distanceKm = distanceKm(lat, lon, aLat, aLon),
                    live = true,
                )
            }.minByOrNull { it.distanceKm ?: Double.MAX_VALUE } ?: fallback.nearest(lat, lon)
        } catch (e: Exception) {
            Log.w(TAG, "AED API failed → demo data", e)
            fallback.nearest(lat, lon)
        }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 6371.0 * 2 * atan2(sqrt(h), sqrt(1 - h))
    }
}
