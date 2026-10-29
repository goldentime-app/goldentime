package ai.seroai.goldentime.data.route

import android.content.Context
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 시연 경로 — OSRM(OpenStreetMap)으로 받은 실제 영동고속도로 구간 (assets/demo_route.json).
 * 실차에서는 NaviHelper 경로로 대체. 거리(m) 기준으로 지점을 꺼낸다.
 */
class DemoRoute(
    /** (경도, 위도) */
    val points: List<Pair<Double, Double>>,
    val carStartM: Double,
    val meetM: Double,
    val ambulanceStartM: Double,
) {
    private val cumulative: DoubleArray = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) acc[i] = acc[i - 1] + haversine(points[i - 1], points[i])
    }

    val lengthM: Double get() = cumulative.last()

    /** 경로 시작에서 [m]미터 지점 */
    fun pointAt(m: Double): Pair<Double, Double> {
        val d = m.coerceIn(0.0, lengthM)
        var i = cumulative.indexOfFirst { it >= d }.coerceAtLeast(1)
        if (i >= points.size) i = points.size - 1
        val seg = cumulative[i] - cumulative[i - 1]
        val f = if (seg <= 0) 0.0 else (d - cumulative[i - 1]) / seg
        val a = points[i - 1]
        val b = points[i]
        return Pair(a.first + (b.first - a.first) * f, a.second + (b.second - a.second) * f)
    }

    /** [fromM]~[toM] 구간 좌표 (양 끝 보간 포함) */
    fun slice(fromM: Double, toM: Double): List<Pair<Double, Double>> {
        val lo = minOf(fromM, toM)
        val hi = maxOf(fromM, toM)
        val inner = points.indices.filter { cumulative[it] > lo && cumulative[it] < hi }.map { points[it] }
        return listOf(pointAt(lo)) + inner + listOf(pointAt(hi))
    }

    companion object {
        fun load(context: Context): DemoRoute? = try {
            val json = JSONObject(context.assets.open("demo_route.json").bufferedReader(Charsets.UTF_8).readText())
            val arr = json.getJSONArray("coordinates")
            val pts = (0 until arr.length()).map { arr.getJSONArray(it).let { p -> p.getDouble(0) to p.getDouble(1) } }
            DemoRoute(pts, json.getDouble("carStartM"), json.getDouble("meetM"), json.getDouble("ambulanceStartM"))
        } catch (e: Exception) {
            android.util.Log.w("DemoRoute", "route asset load failed", e)
            null
        }

        fun haversine(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            val r = 6_371_000.0
            val la1 = Math.toRadians(a.second)
            val la2 = Math.toRadians(b.second)
            val dLa = la2 - la1
            val dLo = Math.toRadians(b.first - a.first)
            val h = sin(dLa / 2) * sin(dLa / 2) + cos(la1) * cos(la2) * sin(dLo / 2) * sin(dLo / 2)
            return 2 * r * asin(sqrt(h))
        }
    }
}
