package ai.seroai.goldentime.ui

import ai.seroai.goldentime.data.route.DemoRoute
import ai.seroai.goldentime.ui.theme.Palette
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** OpenFreeMap 야간 스타일 (키 불필요, © OpenStreetMap 기여자) */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

enum class RealMapMode { MEET, CARE }

/**
 * 실제 지도 — MapLibre + OpenFreeMap 타일 위에 실제 영동고속도로 경로(OSRM)와
 * 내 차·구급차·접선 지점(또는 커브·휴게소)을 그린다.
 * 지도·경로를 불러오지 못하면(오프라인 등) 그려 둔 지도([MapPlaceholder])로 대체.
 */
@Composable
fun RealMap(mode: RealMapMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val route = remember { DemoRoute.load(context) }
    var failed by remember { mutableStateOf(route == null) }
    if (failed || route == null) {
        MapPlaceholder(meetPoint = mode == RealMapMode.MEET, modifier)
        return
    }

    val t = rememberInfiniteTransition(label = "realmap")
    val progress by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart), label = "p",
    )
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener { failed = true }
            getMapAsync { m ->
                m.uiSettings.isRotateGesturesEnabled = false
                m.uiSettings.isTiltGesturesEnabled = false
                m.uiSettings.isLogoEnabled = false // 출처 표기(attribution)는 유지
                m.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                    addLayers(style, route, mode)
                    fitCamera(m, route, mode)
                    map = m
                }
            }
        }
    }

    // 지도 수명주기 = 화면 수명주기
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(obs)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // 움직이는 표식 갱신
    LaunchedEffect(map, progress) {
        val style = map?.style ?: return@LaunchedEffect
        when (mode) {
            RealMapMode.MEET -> {
                val carM = route.carStartM + (route.meetM - route.carStartM) * progress * 0.94
                val ambM = route.ambulanceStartM - (route.ambulanceStartM - route.meetM) * progress * 0.9
                style.getSourceAs<GeoJsonSource>("car")?.setGeoJson(point(route.pointAt(carM), "내 차"))
                style.getSourceAs<GeoJsonSource>("amb")?.setGeoJson(point(route.pointAt(ambM), "구급차"))
                style.getSourceAs<GeoJsonSource>("done")?.setGeoJson(line(route.slice(route.carStartM, carM)))
            }
            RealMapMode.CARE -> {
                val carM = route.carStartM + 3000 * progress
                style.getSourceAs<GeoJsonSource>("car")?.setGeoJson(point(route.pointAt(carM), "내 차"))
            }
        }
    }

    Box(modifier.clip(RoundedCornerShape(16.dp))) {
        AndroidView({ mapView }, Modifier.fillMaxSize())
    }
}

private fun point(p: Pair<Double, Double>, label: String? = null) =
    Feature.fromGeometry(Point.fromLngLat(p.first, p.second)).also { f -> label?.let { f.addStringProperty("label", it) } }

private fun line(pts: List<Pair<Double, Double>>) =
    Feature.fromGeometry(LineString.fromLngLats(pts.map { Point.fromLngLat(it.first, it.second) }))

private fun addLayers(style: Style, route: DemoRoute, mode: RealMapMode) {
    val routeColor = Palette.Route.toArgb()
    style.addSource(GeoJsonSource("route", line(route.points)))
    style.addLayer(
        LineLayer("route-glow", "route").withProperties(
            lineColor(routeColor), lineWidth(9f), lineOpacity(0.16f),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        LineLayer("route-line", "route").withProperties(
            lineColor(routeColor), lineWidth(7f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    fun dot(id: String, p: Pair<Double, Double>?, fill: Int, r: Float, stroke: Int = android.graphics.Color.WHITE, label: String? = null) {
        style.addSource(if (p != null) GeoJsonSource(id, point(p, label)) else GeoJsonSource(id))
        style.addLayer(
            CircleLayer("$id-dot", id).withProperties(
                circleColor(fill), circleRadius(r * 1.5f), circleStrokeColor(stroke), circleStrokeWidth(4f),
            ),
        )
        // 이름표 — 점 위 12dp, 지도 배경과 구분되게 halo
        style.addLayer(
            SymbolLayer("$id-label", id).withProperties(
                textField(Expression.get("label")), textFont(arrayOf("Noto Sans Bold")), textSize(28f),
                textColor(Palette.Ink.toArgb()), textHaloColor(Palette.Ground.toArgb()), textHaloWidth(2f),
                textAnchor(Property.TEXT_ANCHOR_BOTTOM), textOffset(arrayOf(0f, -0.9f)),
                textAllowOverlap(true), textIgnorePlacement(true),
            ),
        )
    }
    when (mode) {
        RealMapMode.MEET -> {
            style.addSource(GeoJsonSource("done"))
            style.addLayer(
                LineLayer("done-line", "done").withProperties(
                    lineColor(Palette.Calm.toArgb()), lineWidth(9f), lineCap(Property.LINE_CAP_ROUND),
                ),
            )
            dot("meet", route.pointAt(route.meetM), Palette.Amber.toArgb(), 11f, Palette.Ground.toArgb(), "접선 지점")
            dot("amb", null, Palette.Alarm.toArgb(), 9f)
            dot("car", null, Palette.Route.toArgb(), 9f)
        }
        RealMapMode.CARE -> {
            dot("curve", route.pointAt(route.carStartM + 2400), Palette.Calm.toArgb(), 9f, Palette.Ground.toArgb(), "급커브 · 회전 예고")
            dot("rest", route.pointAt(route.carStartM + 6000), Palette.Amber.toArgb(), 9f, Palette.Ground.toArgb(), "휴게소 6km")
            dot("car", null, Palette.Route.toArgb(), 9f)
        }
    }
}

private fun fitCamera(map: MapLibreMap, route: DemoRoute, mode: RealMapMode) {
    val (from, to) = when (mode) {
        RealMapMode.MEET -> route.carStartM - 500 to route.ambulanceStartM + 500
        RealMapMode.CARE -> route.carStartM - 500 to route.carStartM + 7000
    }
    val b = LatLngBounds.Builder()
    route.slice(from, to).forEach { b.include(LatLng(it.second, it.first)) }
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 60))
}
