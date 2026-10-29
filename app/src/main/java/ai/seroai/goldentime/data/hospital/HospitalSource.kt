package ai.seroai.goldentime.data.hospital

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 응급실 후보 — 랑데부 화면(E3) 하단에 표시 */
data class ErCandidate(
    val name: String,
    val distanceKm: Double?,
    /** 응급실 실시간 가용 병상 수 (hvec). 음수면 포화 */
    val erBeds: Int?,
    val live: Boolean,
)

interface HospitalSource {
    /** 현재 위치 기준 가까운 응급의료기관 중 가용 병상이 있는 곳 우선 */
    suspend fun nearestAvailable(lat: Double, lon: Double): ErCandidate?
}

/** 시연용 — API 키가 없거나 호출 실패 시 */
class FakeHospitalSource : HospitalSource {
    override suspend fun nearestAvailable(lat: Double, lon: Double) =
        ErCandidate("○○대병원", 34.0, 5, live = false)
}

/**
 * 국립중앙의료원 전국 응급의료기관 정보 조회 서비스 (공공데이터포털 B552657/ErmctInfoInqireService).
 * 1) getEgytLcinfoInqire: 위경도 기준 가까운 응급의료기관(hpid, 거리)
 * 2) getEmrrmRltmUsefulSckbdInfoInqire: 시도·시군구별 응급실 실시간 가용 병상(hvec)
 * 두 결과를 hpid로 합쳐 가용 병상이 있는 가장 가까운 곳을 고른다. 실패하면 [fallback].
 */
class NemcHospitalSource(
    private val serviceKey: String,
    private val fallback: HospitalSource = FakeHospitalSource(),
) : HospitalSource {
    companion object {
        private const val TAG = "NemcHospital"
        private const val BASE = "https://apis.data.go.kr/B552657/ErmctInfoInqireService"
    }

    override suspend fun nearestAvailable(lat: Double, lon: Double): ErCandidate? = withContext(Dispatchers.IO) {
        try {
            val near = items(
                "getEgytLcinfoInqire",
                mapOf("WGS84_LON" to "$lon", "WGS84_LAT" to "$lat", "pageNo" to "1", "numOfRows" to "10"),
            )
            if (near.isEmpty()) return@withContext fallback.nearestAvailable(lat, lon)
            // 가까운 기관의 주소로 시도·시군구를 얻어 실시간 병상 조회
            val addr = near.first()["dutyAddr"].orEmpty().split(" ")
            val beds = if (addr.size >= 2) {
                items(
                    "getEmrrmRltmUsefulSckbdInfoInqire",
                    mapOf("STAGE1" to addr[0], "STAGE2" to addr[1], "pageNo" to "1", "numOfRows" to "50"),
                ).associate { it["hpid"].orEmpty() to it["hvec"]?.toIntOrNull() }
            } else emptyMap()
            val ranked = near.map {
                ErCandidate(
                    name = it["dutyName"].orEmpty(),
                    distanceKm = it["distance"]?.toDoubleOrNull(),
                    erBeds = beds[it["hpid"].orEmpty()],
                    live = true,
                )
            }
            ranked.firstOrNull { (it.erBeds ?: 0) > 0 } ?: ranked.first()
        } catch (e: Exception) {
            Log.w(TAG, "NEMC API failed → demo data", e)
            fallback.nearestAvailable(lat, lon)
        }
    }

    private fun items(op: String, params: Map<String, String>) =
        NemcApi.items("$BASE/$op", params, serviceKey)
}
