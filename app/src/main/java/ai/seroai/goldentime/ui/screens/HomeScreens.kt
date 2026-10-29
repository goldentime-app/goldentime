package ai.seroai.goldentime.ui.screens

import ai.seroai.goldentime.core.motion.RouteForecast
import ai.seroai.goldentime.core.motion.RouteSegment
import ai.seroai.goldentime.core.rest.RestPlanner
import ai.seroai.goldentime.emergency.Screen
import ai.seroai.goldentime.ui.AppState
import ai.seroai.goldentime.ui.BigButton
import ai.seroai.goldentime.ui.CardTitle
import ai.seroai.goldentime.ui.CheckRow
import ai.seroai.goldentime.ui.Gauge
import ai.seroai.goldentime.ui.GhostButton
import ai.seroai.goldentime.ui.ActionBar
import ai.seroai.goldentime.ui.RealMap
import ai.seroai.goldentime.ui.SeatMap
import ai.seroai.goldentime.ui.TurnArrow
import ai.seroai.goldentime.ui.PanelCard
import ai.seroai.goldentime.ui.SeatIndicator
import ai.seroai.goldentime.ui.TopBar
import ai.seroai.goldentime.ui.theme.GtType
import ai.seroai.goldentime.ui.theme.Palette
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

/** 시연용 경로 (프로토타입 ROUTES와 동일) */
private val FAST = listOf(RouteSegment(120, 100.0, 0.5), RouteSegment(38, 55.0, 8.0))
private val COMFY = listOf(RouteSegment(140, 100.0, 0.5), RouteSegment(9, 60.0, 5.0))
private val fastMsdv by lazy { RouteForecast.msdv(FAST) }
private val comfyMsdv by lazy { RouteForecast.msdv(COMFY) }

/** H0 홈 — 멀미(5) : 탑승자·휴식(7), 응급 버튼은 하단 오른쪽 고정 */
@Composable
fun HomeScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("일상", Palette.Calm)
        Row(
            Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val risk = state.riskPercent
            PanelCard(Modifier.weight(5f).fillMaxSize()) {
                CardTitle("멀미 위험 · ${state.occupant.label}")
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Gauge(risk, if (risk >= 70) Palette.Amber else Palette.Calm, "누적 노출 / 개인 기준", Modifier.size(220.dp))
                }
                // 수치 대신 신호 출처 (차량특화성) — MSDV 원시값은 예보 화면에서
                Text(
                    "핸들 각도·속도로 실시간 추정",
                    color = Palette.Ink2, fontSize = GtType.Body,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Column(Modifier.weight(7f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PanelCard(Modifier.weight(1f).fillMaxWidth()) {
                    CardTitle("탑승자 · 좌석 신호")
                    // 실신호(Seat occupancy)가 있으면 그대로, 없으면 시연 기본값
                    val seats = state.liveSeats
                    val driver = seats?.driver ?: true
                    val passenger = seats?.passenger ?: true
                    val rear = seats?.rear ?: true
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SeatMap(driver, passenger, rear, Modifier.size(width = 112.dp, height = 168.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                listOfNotNull("운전석".takeIf { driver }, "조수석".takeIf { passenger }, "뒷좌석".takeIf { rear })
                                    .joinToString(" · ") + " 탑승",
                                color = Palette.Ink2, fontSize = GtType.Body,
                            )
                            Text("뒷좌석 아이 4세를\n기준으로 케어해요", color = Palette.Ink, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                PanelCard(Modifier.weight(1f).fillMaxWidth()) {
                    CardTitle("휴식 · ${state.occupant.label} 기준")
                    // 아동 동승 2시간 규칙(가설) + 멀미 위험 연동 — RestPlanner
                    val drivenMin = 96
                    val untilRest = RestPlanner.minutesUntilRest(drivenMin, state.occupant, state.riskPercent)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                        if (untilRest == 0) {
                            Text("지금 휴게소에 들르는 걸 권해요", color = Palette.Amber, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Palette.Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)) { append("권장 휴식까지 ") }
                                    withStyle(SpanStyle(color = Palette.Amber, fontSize = 32.sp, fontWeight = FontWeight.Bold)) { append("${untilRest}분") }
                                },
                            )
                        }
                        Text(
                            "연속 주행 ${drivenMin / 60}시간 ${drivenMin % 60}분 · 다음 휴게소 안성 18km",
                            color = Palette.Ink2, fontSize = GtType.Body,
                        )
                    }
                }
            }
        }
        ActionBar(onEmergency = { state.go(Screen.E1) }, hint = "\u201c응급 모드\u201d라고 말해도 돼요") {
            GhostButton("멀미 예보", fontSize = 20) { state.go(Screen.H1) }
            GhostButton("가족 프로필", fontSize = 20) { state.go(Screen.S1) }
        }
    }
}

/** H1 출발 전 멀미 예보 — 두 경로 예상 MSDV 비교, changeRouteOption(COMFORTABLE) */
@Composable
fun ForecastScreen(state: AppState, onToast: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("멀미 예보", Palette.Calm, driving = false)
        Text(
            "출발 전 멀미 예보 · 서울 → 강릉",
            color = Palette.Ink, fontSize = GtType.H2, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 18.dp),
        )
        Row(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val thr = state.threshold
            RouteCard("빠른 경로 (내비 기본)", "2시간 41분 · 굽은 길 38분 · MSDV %.0f".format(fastMsdv), fastMsdv, thr, recommended = false, Modifier.weight(1f))
            RouteCard("편안한 경로", "2시간 49분 · 굽은 길 9분 · MSDV %.0f".format(comfyMsdv), comfyMsdv, thr, recommended = true, Modifier.weight(1f))
            PanelCard(Modifier.weight(1.05f).fillMaxSize()) {
                CardTitle("이렇게 준비해요 · ${state.occupant.label} 기준 %.0f".format(thr))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
                    CheckRow("화면 대신 앞 창밖 보기")
                    CheckRow("회전 예고 켜기")
                    CheckRow("굽은 길 전 휴게소 들르기")
                }
            }
        }
        ActionBar(onEmergency = { state.go(Screen.E1) }) {
            BigButton("편안한 경로로 출발", color = Palette.Calm, textColor = Palette.PaperInk, fontSize = 20) {
                state.naviAction(onToast, "편안한 경로로 바꿨어요 · changeRouteOption") { setComfortRoute() }
                state.go(Screen.H0)
            }
            GhostButton("빠른 경로 그대로", fontSize = 20) { state.go(Screen.H0) }
        }
    }
}

@Composable
private fun RouteCard(name: String, detail: String, msdv: Double, threshold: Double, recommended: Boolean, modifier: Modifier) {
    val pct = (msdv / threshold * 100).roundToInt()
    PanelCard(modifier.fillMaxSize(), borderColor = if (recommended) Palette.Calm else null) {
        CardTitle("$name · 예상 노출")
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Gauge(
                    min(100, pct),
                    when {
                        pct >= 100 -> Palette.Alarm
                        pct >= 70 -> Palette.Amber
                        else -> Palette.Calm
                    },
                    "예상 위험도",
                    Modifier.size(220.dp),
                )
                if (pct >= 100) {
                    Text("개인 기준 초과 $pct%", color = Palette.Alarm, fontSize = GtType.Body, fontWeight = FontWeight.Bold)
                }
            }
        }
        val (time, msdvText) = detail.split(" · MSDV ").let { it[0] to it.getOrElse(1) { "" } }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(time, color = Palette.Ink, fontSize = GtType.Body, lineHeight = 26.sp)
            if (msdvText.isNotEmpty()) Text("멀미 노출량 MSDV $msdvText", color = Palette.Ink2, fontSize = GtType.Caption)
        }
    }
}

/** 커브 예고 배너 — 뒷좌석 화면에도 같은 신호 (몸이 움직임을 미리 알게 해 멀미 완화) */
@Composable
private fun TurnCueBanner(cue: ai.seroai.goldentime.core.motion.TurnCue, modifier: Modifier = Modifier) {
    val left = cue.direction == ai.seroai.goldentime.data.navi.TurnDirection.LEFT
    Row(
        modifier.background(Palette.Ground.copy(alpha = 0.92f), RoundedCornerShape(50))
            .border(2.dp, Palette.Calm, RoundedCornerShape(50))
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TurnArrow(left, Palette.Calm, Modifier.size(36.dp))
        Text(
            "${cue.secondsAhead}초 뒤 ${if (left) "왼쪽" else "오른쪽"} 커브",
            color = Palette.Ink, fontSize = GtType.Title, fontWeight = FontWeight.Bold,
        )
        Text("앞을 보세요", color = Palette.Calm, fontSize = GtType.Body, fontWeight = FontWeight.SemiBold)
    }
}

/** H2 멀미 대응 — 위험도 70% 이상 시 표시. addWaypoint */
@Composable
fun MotionCareScreen(state: AppState, onToast: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("일상 케어", Palette.Calm)
        Row(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(Modifier.weight(1.6f).fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    RealMap(ai.seroai.goldentime.ui.RealMapMode.CARE, Modifier.fillMaxSize())
                    // 커브 예고: 실차는 NaviHelper onTBTInfo, 시연은 60km/h로 12초마다 왼쪽 커브에 접근
                    val turn = state.navi.nextTurn() ?: run {
                        val t = (state.nowMs / 1000.0) % 12.0
                        ai.seroai.goldentime.data.navi.TurnInfo(
                            ai.seroai.goldentime.data.navi.TurnDirection.LEFT, ((12.0 - t) * 16.7).toInt(),
                        )
                    }
                    ai.seroai.goldentime.core.motion.TurnCuePlanner.cue(turn.direction, turn.distanceMeters, 60.0)?.let { cue ->
                        TurnCueBanner(cue, Modifier.align(Alignment.TopStart).padding(16.dp))
                    }
                }
            }
            PanelCard(Modifier.weight(1f).fillMaxSize(), borderColor = Palette.Amber) {
                Text("멀미 위험이 높아지고 있어요", color = Palette.Amber, fontSize = GtType.Title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Gauge(state.riskPercent, Palette.Amber, "위험도", Modifier.size(176.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    CheckRow("뒷좌석 화면 끄고 앞 보기", 20)
                    CheckRow("커브 예고 신호", 20)
                    CheckRow("부드러운 운전 안내", 20)
                }
            }
        }
        ActionBar(onEmergency = { state.go(Screen.E1) }) {
            BigButton("휴게소 6km 경유", color = Palette.Calm, textColor = Palette.PaperInk, fontSize = 20) {
                state.naviAction(onToast, "휴게소를 경유지로 추가했어요 · addWaypoint") {
                    addWaypoint(ai.seroai.goldentime.data.navi.Destination("휴게소", 37.4432, 128.08063))
                }
                state.go(Screen.H0)
            }
            GhostButton("괜찮아요", fontSize = 20) { state.go(Screen.H0) }
        }
    }
}

/** H3 주의 모드 — 발열 등 비응급. 목록 3개까지, "첫 번째"로 음성 선택 */
@Composable
fun CautionScreen(state: AppState, onToast: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("주의", Palette.Amber)
        Column(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.height(72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("“아이가 열이 나요”", color = Palette.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("가까운 진료·약국을 찾았어요 · 경유지로 바로 추가해요", color = Palette.Ink2, fontSize = GtType.Body)
            }
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "○○아동병원 야간진료" to "4.2km · 9분 · 22시까지",
                    "△△약국" to "1.8km · 5분 · 영업 중",
                    "□□의원" to "6.0km · 13분 · 21시까지",
                ).forEach { (name, detail) ->
                    PanelCard(Modifier.weight(1f).fillMaxWidth()) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(name, color = Palette.Ink, fontSize = GtType.Title, fontWeight = FontWeight.Bold)
                                Text(detail, color = Palette.Ink2, fontSize = GtType.Body)
                            }
                            BigButton("경유", color = Palette.Amber, textColor = Palette.PaperInk, fontSize = 20, modifier = Modifier.width(112.dp)) {
                                // 시연 좌표 — 실서비스는 의료기관 검색 결과 좌표
                                state.naviAction(onToast, "${name}을(를) 경유지로 추가했어요") {
                                    addWaypoint(ai.seroai.goldentime.data.navi.Destination(name, 37.44, 128.05))
                                }
                                state.go(Screen.H0)
                            }
                        }
                    }
                }
            }
        }
        ActionBar(onEmergency = { state.go(Screen.E1) }, hint = "숨쉬기 힘들어하거나 경련하면") {
            GhostButton("홈으로", fontSize = 20) { state.go(Screen.H0) }
        }
    }
}

/** S1 가족 건강 프로필 — 정차 중에만 편집, 차량에만 저장 */
@Composable
fun FamilyProfileScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("설정", Palette.Calm, driving = false)
        Row(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(2f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(
                    "아이 · 4세" to "열성경련 이력 · 알레르기 없음",
                    "어머니 · 68세" to "고혈압 · 복용약 2종",
                    "보호자 연락처" to "2명 등록",
                ).forEach { (name, detail) ->
                    PanelCard(Modifier.weight(1f).fillMaxWidth()) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(name, color = Palette.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text(detail, color = Palette.Ink2, fontSize = GtType.Body)
                            }
                            GhostButton("편집") { }
                        }
                    }
                }
            }
            PanelCard(Modifier.weight(1f).fillMaxSize()) {
                CardTitle("개인정보")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                    Text("· 차량에만 저장", color = Palette.Ink, fontSize = GtType.Body)
                    Text(
                        "· 응급 시 119 요약 자동 전송 · " + if (state.autoSendConsent) "켬" else "끔",
                        color = Palette.Ink, fontSize = GtType.Body,
                    )
                    Text("· 언제든 삭제", color = Palette.Ink, fontSize = GtType.Body)
                }
            }
        }
        ActionBar(onEmergency = { state.go(Screen.E1) }) {
            GhostButton("홈으로", fontSize = 20) { state.go(Screen.H0) }
        }
    }
}
