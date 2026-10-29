package ai.seroai.goldentime

import ai.seroai.goldentime.data.location.FakeLocationSource
import ai.seroai.goldentime.data.vehicle.PleosVehicleSource
import ai.seroai.goldentime.emergency.Screen
import ai.seroai.goldentime.ui.AppState
import ai.seroai.goldentime.ui.GoldenTimeApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 시연 사례의 접선 지점(○○ 졸음쉼터) 좌표 — assets/demo_route.json 실제 영동고속도로 경로 위 */
private const val MEET_LAT = 37.44250
private const val MEET_LON = 128.08271

class MainActivity : ComponentActivity() {
    private val appState = AppState()
    private var pleosSource: PleosVehicleSource? = null

    private var naviSource: ai.seroai.goldentime.data.navi.PleosNaviSource? = null
    private var speaker: ai.seroai.goldentime.voice.PleosSpeaker? = null
    private var voiceIn: ai.seroai.goldentime.voice.PleosListener? = null
    private var location: ai.seroai.goldentime.data.location.PleosLocationSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        org.maplibre.android.MapLibre.getInstance(this) // 실제 지도 (OpenFreeMap 타일)
        // Vehicle SDK 연결 시도 — 실패하면 Fake 신호로 동작 (시연 모드는 항상 가능)
        pleosSource = PleosVehicleSource(this).takeIf { it.start() }
        appState.sdkStatus = if (pleosSource != null) "Vehicle SDK 연결됨" else "Fake 신호"
        pleosSource?.let { startLiveSignals(it) }

        // NaviHelper — 실패해도 Fake로 동작
        naviSource = ai.seroai.goldentime.data.navi.PleosNaviSource(this).takeIf { it.start() }
        naviSource?.let { appState.navi = it }
        appState.scope = lifecycleScope

        // 음성: Pleos TTS/STT SDK (HYBRID). 초기화 실패 시 Android TTS/STT로 자동 대체
        val id = BuildConfig.PLEOS_CLIENT_ID
        val secret = BuildConfig.PLEOS_CLIENT_SECRET
        speaker = ai.seroai.goldentime.voice.PleosSpeaker(this, id, secret, ai.seroai.goldentime.voice.AndroidSpeaker(this))
        appState.speaker = speaker!!
        voiceIn = ai.seroai.goldentime.voice.PleosListener(this, id, secret, ai.seroai.goldentime.voice.AndroidListener(this))
        appState.voiceListener = voiceIn!!

        // 위치: Pleos FusedLocation — 속도 추정 + 접선 지점 반경 진입 감지
        location = ai.seroai.goldentime.data.location.PleosLocationSource(this, FakeLocationSource())
            .takeIf { it.start() }
        location?.let { watchMeetPointOnE4(it) }
        refreshErCandidateOnEmergency()

        val needed = arrayOf(android.Manifest.permission.RECORD_AUDIO, "pleos.car.permission.FUSED_LOCATION")
            .filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)

        // 119 전화: ACTION_DIAL(자동 발신 아님) — 발신·SMS 자동화 허용 여부는 설명회 확인
        appState.onDial119 = {
            try {
                startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:119"),
                    ),
                )
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "dial failed", e)
            }
        }
        handleDemoIntent(intent)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                GoldenTimeApp(appState)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDemoIntent(intent)
    }

    /** adb shell am start ... --es gt_demo A --ei gt_step 3 → 시연 시나리오 해당 단계로 이동 */
    private fun handleDemoIntent(intent: android.content.Intent?) {
        val key = intent?.getStringExtra("gt_demo") ?: return
        appState.demoJump = key to intent.getIntExtra("gt_step", -1)
    }

    /** 응급 모드 진입 시 현재 위치 기준 응급실 후보·실시간 가용 병상·가까운 AED 조회 (키 없으면 시연 데이터) */
    private fun refreshErCandidateOnEmergency() {
        val key = BuildConfig.DATA_GO_KEY
        if (key.isEmpty()) return
        val hospitals = ai.seroai.goldentime.data.hospital.NemcHospitalSource(key)
        val aeds = ai.seroai.goldentime.data.hospital.NemcAedSource(key)
        lifecycleScope.launch {
            androidx.compose.runtime.snapshotFlow { appState.screen.isEmergency }.collect { emergency ->
                if (emergency) {
                    val here = location?.latest
                    val lat = here?.latitude ?: MEET_LAT
                    val lon = here?.longitude ?: MEET_LON
                    hospitals.nearestAvailable(lat, lon)?.let { appState.erCandidate = it }
                    aeds.nearest(lat, lon)?.let { appState.nearestAed = it }
                }
            }
        }
    }

    /** E4 진입 시 접선 지점 반경 300m 알림 등록 → 진입하면 E5(정차·도어 해제·인계) */
    private fun watchMeetPointOnE4(loc: ai.seroai.goldentime.data.location.PleosLocationSource) {
        lifecycleScope.launch {
            androidx.compose.runtime.snapshotFlow { appState.screen }.collect { screen ->
                if (screen == Screen.E4) {
                    loc.watchMeetPoint(MEET_LAT, MEET_LON, 300f) {
                        runOnUiThread { if (appState.screen == Screen.E4) appState.go(Screen.E5) }
                    }
                }
            }
        }
    }

    /** 실신호 → 멀미 모델·탑승자·사고 감지 연결. 속도는 위치 변화 추정(에뮬레이터는 순항 가정) */
    private fun startLiveSignals(src: PleosVehicleSource) {
        val fallbackLocation = FakeLocationSource()
        var steeringDeg = 0.0
        var brakePressed = false
        lifecycleScope.launch { src.steeringAngle.collect { steeringDeg = it } }
        lifecycleScope.launch { src.brakePedal.collect { brakePressed = it > 50.0 } }
        lifecycleScope.launch { src.seatOccupancy.collect { appState.liveSeats = it } }
        lifecycleScope.launch {
            src.airbagDeployed.collect { deployed ->
                // 에어백 전개 → E7 (Virtual Console에 카드가 없어 시연 모드로도 주입 가능)
                if (deployed && appState.screen != Screen.E7) {
                    appState.crashDetector.onAirbagDeployed(appState.clock())
                    appState.go(Screen.E7)
                }
            }
        }
        lifecycleScope.launch {
            appState.liveActive = true
            appState.sdkStatus = "Vehicle SDK 연결됨 · 실신호"
            var n = 0
            while (isActive) {
                appState.liveEngine.step(steeringDeg, (location ?: fallbackLocation).speedKmh() ?: 0.0, brakePressed, 0.1)
                appState.liveMsdv = appState.liveEngine.msdv
                if (n++ % 20 == 0) {
                    android.util.Log.d("GtLive", "steer=%.1f brake=%b msdv=%.3f".format(steeringDeg, brakePressed, appState.liveEngine.msdv))
                }
                delay(100)
            }
        }
    }

    override fun onDestroy() {
        pleosSource?.stop()
        naviSource?.stop()
        speaker?.release()
        voiceIn?.release()
        location?.stop()
        super.onDestroy()
    }
}
