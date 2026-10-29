package ai.seroai.goldentime.ui

import ai.seroai.goldentime.core.motion.MotionModel
import ai.seroai.goldentime.core.motion.MotionSimulator
import ai.seroai.goldentime.core.motion.Occupant
import ai.seroai.goldentime.core.rendezvous.MeetPoint
import ai.seroai.goldentime.core.rendezvous.MeetPointSelector
import ai.seroai.goldentime.core.rendezvous.MeetPointType
import ai.seroai.goldentime.core.rendezvous.RendezvousInput
import ai.seroai.goldentime.core.rendezvous.RendezvousOption
import ai.seroai.goldentime.data.navi.FakeNaviSource
import kotlinx.coroutines.launch
import ai.seroai.goldentime.data.vehicle.FakeVehicleSource
import ai.seroai.goldentime.emergency.CrashDetector
import ai.seroai.goldentime.emergency.Screen
import ai.seroai.goldentime.emergency.Send119Controller
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ChatLine(val text: String, val fromApp: Boolean)

/**
 * 앱 전역 상태 — prototype/index.html의 state/go()와 동일한 전이 규칙.
 * 시계는 주입 가능 (시연·테스트).
 */
class AppState(val clock: () -> Long = System::currentTimeMillis) {
    var screen by mutableStateOf(Screen.H0)
        private set

    /** 시연 점프 요청 (시나리오 키, 단계 인덱스) — adb 인텐트로 화면 캡처 자동화용 */
    var demoJump by mutableStateOf<Pair<String, Int>?>(null)
    val chat = mutableStateListOf<ChatLine>()
    var crash by mutableStateOf(false)
    var forcedRiskPercent by mutableStateOf<Int?>(null)
    var toast by mutableStateOf("")
    var occupant by mutableStateOf(Occupant.CHILD)

    /** 시연용 주행 시뮬레이터 (실차에서는 VehicleSource 신호로 대체) */
    val simulator = MotionSimulator()
    var personalThreshold: Double? = null

    init {
        // 시연 웜스타트: "이미 감지하고 있는 앱"으로 보이게 위험도 ~32%까지 선진행 (리뷰 P0-8)
        val target = occupant.defaultThreshold * 0.32
        while (simulator.msdv < target && simulator.t < 7200) {
            simulator.step(90.0, 3.0, 55.0, 0.2)
        }
    }

    val vehicle = FakeVehicleSource()
    /** 내비 — MainActivity가 NaviHelper 연결에 성공하면 PleosNaviSource로 교체 */
    var navi: ai.seroai.goldentime.data.navi.NaviSource = FakeNaviSource()

    /** 버튼에서 내비 호출(suspend)을 띄울 범위 — MainActivity가 lifecycleScope로 설정 */
    var scope: kotlinx.coroutines.CoroutineScope? = null

    /** 내비 호출 후 결과를 토스트로 알린다 (연결 안 되면 시연 표시) */
    fun naviAction(onToast: (String) -> Unit, okText: String, block: suspend ai.seroai.goldentime.data.navi.NaviSource.() -> Boolean) {
        val s = scope ?: return onToast("$okText (시연)")
        s.launch {
            val ok = runCatching { navi.block() }.getOrDefault(false)
            onToast(if (ok && navi !is FakeNaviSource) okText else "$okText (시연)")
        }
    }

    /** Vehicle SDK 연결 상태 표시 (시연 바) — MainActivity에서 설정 */
    var sdkStatus by mutableStateOf("Fake 신호")

    /** 실신호 모드: Vehicle SDK 신호가 멀미 모델에 흐르는 중 (MainActivity가 구동) */
    var liveActive by mutableStateOf(false)
    val liveEngine = ai.seroai.goldentime.core.motion.LiveMotionEngine()

    /** 라이브 MSDV의 Compose 상태 사본 — 루프가 갱신해야 화면이 재구성된다 */
    var liveMsdv by mutableStateOf(0.0)

    /** 응급실 후보 (E3) — 공공 API 실시간 병상 또는 시연 데이터. MainActivity가 응급 진입 시 갱신 */
    var erCandidate by mutableStateOf(ai.seroai.goldentime.data.hospital.ErCandidate("○○대병원", 34.0, 5, live = false))

    /** 경로 앞 안전 정차 지점 (시연값) — 실제로는 경로상 비상주차대·졸음쉼터·휴게소 위치 데이터 + 현재 위치 */
    var safeStops by mutableStateOf(
        listOf(
            ai.seroai.goldentime.core.stop.SafeStop(null, ai.seroai.goldentime.core.stop.SafeStopType.EMERGENCY_BAY, 420),
            ai.seroai.goldentime.core.stop.SafeStop("○○", ai.seroai.goldentime.core.stop.SafeStopType.DROWSY_SHELTER, 2100),
            ai.seroai.goldentime.core.stop.SafeStop("△△", ai.seroai.goldentime.core.stop.SafeStopType.REST_AREA, 8600),
        ),
    )

    /** 가까운 AED (E6) — 공공 API 또는 시연 데이터 */
    var nearestAed by mutableStateOf(ai.seroai.goldentime.data.hospital.AedSpot("□□ 지구대", null, 1.2, live = false))

    /** 실신호 착좌 상태 (탑승자 카드) — 실신호 없으면 시연 기본값 */
    var liveSeats by mutableStateOf<ai.seroai.goldentime.data.vehicle.SeatOccupancy?>(null)

    /** 음성 출력 — MainActivity가 AndroidSpeaker로 교체 */
    var speaker: ai.seroai.goldentime.voice.Speaker = ai.seroai.goldentime.voice.FakeSpeaker()

    /** 음성 입력 — E1 마이크. MainActivity가 AndroidListener로 교체 */
    var voiceListener: ai.seroai.goldentime.voice.Listener = ai.seroai.goldentime.voice.FakeListener()

    /** 119 전화 다이얼 (ACTION_DIAL — 자동 발신 아님, 사용자가 통화 버튼 확인) */
    var onDial119: () -> Unit = {}

    /** 현재 응급 사례 — E2 표시와 119 문자 본문의 단일 출처 */
    val currentCase: ai.seroai.goldentime.emergency.EmergencyCase
        get() = if (crash) ai.seroai.goldentime.emergency.EmergencyCase.CRASH
        else ai.seroai.goldentime.emergency.EmergencyCase.SEIZURE

    /** S1에서 켠 사전 동의 (절대 원칙 4) — 시연 기본값 켬 */
    var autoSendConsent by mutableStateOf(true)
    var send = Send119Controller(consentGiven = true)
        private set
    val crashDetector = CrashDetector()

    /** 경련 시작 시각 (E 흐름 진입 시) */
    var emergencyStartMs by mutableStateOf<Long?>(null)

    /** 매초 갱신용 틱 — 화면에서 LaunchedEffect로 올린다 */
    var nowMs by mutableStateOf(clock())

    val threshold: Double get() = personalThreshold ?: occupant.defaultThreshold

    /** 실신호가 흐르면 라이브 모델, 아니면 시연 시뮬레이터. 시연 단계의 강제값이 최우선 */
    private val currentMsdv: Double
        get() = if (liveActive) simulator.msdv + liveMsdv else simulator.msdv

    val riskPercent: Int
        get() = forcedRiskPercent ?: MotionModel.riskPercent(currentMsdv, threshold)

    val msdvDisplay: Double
        get() = forcedRiskPercent?.let { it * threshold / 100.0 } ?: currentMsdv

    /** 랑데부 시연 입력 (CLAUDE.md 테스트 값) — 실제로는 NaviHelper ETA로 채움 */
    var rendezvousInput by mutableStateOf(
        RendezvousInput(
            ambToHere = 20, carToHospital = 38, carToPoint = 7, ambToPoint = 6,
            pointToHospital = 25, hereToHospital = 29, handover = 2,
        ),
    )
    var urgent by mutableStateOf(false)

    /** 탑승자가 말한 상황 분류 (진단 아님) — 랑데부 시간 기준선. 시연 A = 경련 */
    var emergencyKind by mutableStateOf(ai.seroai.goldentime.core.rendezvous.EmergencyKind.SEIZURE)

    /** 접선 후보 (시연값) — 실제로는 경로상 졸음쉼터·휴게소·주유소·지구대 POI + NaviHelper ETA */
    var meetPoints by mutableStateOf(
        listOf(
            MeetPoint("○○", MeetPointType.DROWSY_SHELTER, carMin = 7, ambMin = 6, toHospitalMin = 25, hasAed = false),
            MeetPoint("△△", MeetPointType.GAS_STATION, carMin = 5, ambMin = 9, toHospitalMin = 24, hasAed = false),
            MeetPoint("□□", MeetPointType.POLICE, carMin = 12, ambMin = 8, toHospitalMin = 21, hasAed = true),
        ),
    )

    val meetPoint: MeetPoint?
        get() = MeetPointSelector.best(meetPoints, rendezvousInput.handover)

    val rendezvousOptions: List<RendezvousOption>
        get() {
            val v = meetPoint?.let { MeetPointSelector.toInput(rendezvousInput, it) } ?: rendezvousInput
            return MeetPointSelector.recommend(v, emergencyKind)
        }

    fun go(target: Screen, scripted: Boolean = false, immediateSend: Boolean = false) {
        if (target == Screen.E2 && screen != Screen.E2) {
            send = Send119Controller(consentGiven = autoSendConsent)
            send.arm(clock(), immediate = immediateSend)
        }
        if (target == Screen.E6 && screen != Screen.E6) {
            // 심정지 의심: 카운트다운 없이 즉시 (절대 원칙 4)
            send = Send119Controller(consentGiven = autoSendConsent)
            send.arm(clock(), immediate = true)
        }
        if (!scripted) forcedRiskPercent = null
        if (target.isEmergency && target != Screen.E7 && emergencyStartMs == null && !crash) {
            emergencyStartMs = clock()
        }
        if (target == Screen.H0) crash = false
        if (target == Screen.E1 && screen != Screen.E1 && !scripted) chat.clear()
        screen = target
    }

    fun elapsedEmergency(): String {
        val start = emergencyStartMs ?: return "00:00"
        val s = ((nowMs - start) / 1000 + 120).toInt() // 프로토타입과 동일: 시연상 2분 경과부터
        return "%02d:%02d".format(s / 60, s % 60)
    }

    fun resetEmergency() {
        chat.clear()
        crash = false
        emergencyStartMs = null
        forcedRiskPercent = null
        crashDetector.reset()
    }
}
