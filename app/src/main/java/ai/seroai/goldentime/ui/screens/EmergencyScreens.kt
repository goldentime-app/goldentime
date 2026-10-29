package ai.seroai.goldentime.ui.screens

import ai.seroai.goldentime.core.rendezvous.RendezvousKind
import ai.seroai.goldentime.emergency.CrashDetector
import ai.seroai.goldentime.emergency.FixedScripts
import ai.seroai.goldentime.emergency.Screen
import ai.seroai.goldentime.emergency.Send119Controller
import ai.seroai.goldentime.ui.AppState
import ai.seroai.goldentime.ui.BigButton
import ai.seroai.goldentime.ui.CardTitle
import ai.seroai.goldentime.ui.CheckRow
import ai.seroai.goldentime.ui.CprBeatDots
import ai.seroai.goldentime.ui.GhostButton
import ai.seroai.goldentime.ui.ListeningMic
import ai.seroai.goldentime.ui.RealMap
import ai.seroai.goldentime.ui.PanelCard
import ai.seroai.goldentime.ui.RendezvousTimeline
import ai.seroai.goldentime.ui.TopBar
import ai.seroai.goldentime.ui.theme.GtType
import ai.seroai.goldentime.ui.theme.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** E1 응급 접수 — 질문은 한 번에 하나. 대화가 주인공 */
@Composable
fun EmergencyListenScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("응급", Palette.Alarm)
        Row(Modifier.weight(1f).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 마이크 탭 → 실제 음성 인식 1회 (SpeechRecognizer ko-KR). 심정지 키워드면 E6
                Box(
                    Modifier.clickable {
                        state.voiceListener.listenOnce(
                            onResult = { text ->
                                state.chat.add(ai.seroai.goldentime.ui.ChatLine(text, fromApp = false))
                                val target = ai.seroai.goldentime.emergency.EmergencyRouter.route(text)
                                if (target != Screen.E1) state.go(target)
                            },
                            onError = { /* 인식 실패 — 시연 모드 단계 재생으로 진행 가능 */ },
                        )
                    },
                ) {
                    ListeningMic(Modifier.size(240.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("듣고 있어요", color = Palette.Ink, fontSize = GtType.Title, fontWeight = FontWeight.SemiBold)
            }
            PanelCard(Modifier.weight(2.4f).fillMaxSize(), emergency = true) {
                CardTitle("대화")
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (state.chat.isEmpty()) {
                        Text("무엇이 필요하신가요? 상황을 말씀해 주세요.", color = Palette.Ink2, fontSize = GtType.Title)
                    }
                    // 말풍선: 골든타임(왼쪽, 붉은 테두리) / 탑승자(오른쪽, 밝은 면)
                    val recent = state.chat.takeLast(3)
                    val focus = recent.lastOrNull { it.fromApp }
                    recent.forEach { line ->
                        val app = line.fromApp
                        val current = line === focus
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = if (app) Alignment.Start else Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(if (app) "골든타임" else "탑승자", color = Palette.Ink2, fontSize = GtType.Caption)
                            Box(
                                Modifier.fillMaxWidth(0.82f).wrapContentWidth(if (app) Alignment.Start else Alignment.End)
                                    .background(
                                        if (app && current) Palette.Alarm.copy(alpha = 0.16f) else if (app) Color.Transparent else Palette.Panel2,
                                        RoundedCornerShape(
                                            topStart = if (app) 4.dp else 20.dp, topEnd = if (app) 20.dp else 4.dp,
                                            bottomStart = 20.dp, bottomEnd = 20.dp,
                                        ),
                                    )
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                            ) {
                                Text(
                                    line.text,
                                    color = (if (app) Palette.AlarmText else Palette.Ink).copy(alpha = if (current || !app) 1f else 0.55f),
                                    fontSize = if (current) 28.sp else GtType.Title,
                                    fontWeight = if (app) FontWeight.Bold else FontWeight.Medium,
                                    lineHeight = if (current) 36.sp else 30.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** E2 상황 요약 · 119 전송 (3초 취소, 절대 원칙 4) */
@Composable
fun Summary119Screen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("응급", Palette.Alarm)
        Row(Modifier.weight(1f).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(Modifier.weight(0.9f).fillMaxSize(), emergency = true) {
                CardTitle("상황 요약 · 119 문자신고로 전송")
                // 화면 표시와 문자 본문의 단일 출처: EmergencyCase (Report119Builder가 형식·진단어 검사)
                val case = state.currentCase
                val rows = listOf(
                    "대상" to case.subject,
                    (if (state.crash) "상황" else "증상") to "${case.symptom} · ${case.onset} 시작",
                    "병력" to case.history,
                    "위치" to case.roadDescription,
                    "차량" to case.vehicle,
                    "조치" to case.action,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)) {
                    rows.forEachIndexed { i, (k, v) ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Rule.copy(alpha = 0.6f)))
                        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
                            Text(k, color = Palette.Ink2, fontSize = GtType.Body, modifier = Modifier.weight(0.26f))
                            Column(Modifier.weight(1f)) {
                                Text(v, color = Palette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (k == "위치") {
                                    Text(
                                        "%.5fN %.5fE".format(case.latitude, case.longitude),
                                        color = Palette.Ink2, fontSize = GtType.Caption,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1.2f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 전송 완료 후에는 완료 카드를 낮게 — 행동 지침이 주인공 (카운트다운·취소 상태는 크게)
                val sent = state.send.state(state.nowMs) is Send119Controller.State.Sent
                SendStateCard(state, if (sent) Modifier.fillMaxWidth().height(150.dp) else Modifier.weight(0.75f).fillMaxWidth())
                PanelCard(Modifier.weight(1.25f).fillMaxWidth(), emergency = true) {
                    CardTitle("지금 할 일")
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(
                            if (state.crash) "안전한 곳에 차를 세우고 비상등을 켜세요." else "아이를 옆으로 눕히고, 입에 아무것도 넣지 마세요.",
                            color = Palette.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
                        )
                        Spacer(Modifier.height(18.dp))
                        if (state.crash) {
                            Text("119에 사고 정보 전송", color = Palette.AlarmText, fontSize = GtType.Body, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(14.dp))
                            SafeStopLine(state)
                        } else {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Palette.Ink2, fontSize = GtType.Body)) { append("경련 시간  ") }
                                    withStyle(SpanStyle(color = Palette.Gold, fontSize = 48.sp, fontWeight = FontWeight.Bold)) {
                                        append(state.elapsedEmergency())
                                    }
                                },
                            )
                        }
                    }
                    if (!state.crash) {
                        BigButton("이동 방법 보기", color = Palette.Panel2, textColor = Palette.Ink, fontSize = 18, modifier = Modifier.fillMaxWidth()) {
                            state.go(Screen.E3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SendStateCard(state: AppState, modifier: Modifier) {
    val sendState = state.send.state(state.nowMs)
    val border = if (sendState is Send119Controller.State.Sent) Palette.Calm else Palette.Alarm
    PanelCard(modifier, emergency = true, borderColor = border) {
        Column(
            Modifier.weight(1f).fillMaxWidth().then(if (sendState is Send119Controller.State.Sent) Modifier else Modifier.heightIn(min = 120.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            when (sendState) {
                is Send119Controller.State.CountingDown -> {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Palette.Gold, fontSize = GtType.Display, fontWeight = FontWeight.Bold)) {
                                append("${sendState.remainingSeconds}")
                            }
                            withStyle(SpanStyle(color = Palette.Ink, fontSize = GtType.H2, fontWeight = FontWeight.Bold)) {
                                append("초 뒤 119에 요약을 보내요")
                            }
                        },
                    )
                    Text("위치·상황 요약을 문자로 먼저 보내고 전화를 연결합니다", color = Palette.Ink2, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    GhostButton(FixedScripts.SEND_CANCEL_LABEL, 18) { state.send.cancel(state.clock()) }
                }
                is Send119Controller.State.Sent -> {
                    Text("✓ 119에 요약을 보냈어요", color = Palette.Calm, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("전화 연결 중 · 상담원 질문에만 답하세요", color = Palette.AlarmText, fontSize = GtType.Body, fontWeight = FontWeight.SemiBold)
                }
                is Send119Controller.State.Canceled -> {
                    Text("전송을 취소했어요", color = Palette.Ink, fontSize = GtType.H2, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    BigButton("119 요약 보내고 전화", fontSize = 20) { state.send.sendNow() }
                }
                is Send119Controller.State.ManualOnly -> {
                    Text("자동 전송이 꺼져 있어요", color = Palette.Ink, fontSize = GtType.H2, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    BigButton("119 요약 보내고 전화", fontSize = 20) { state.send.sendNow() }
                }
                is Send119Controller.State.SmsFailedCallOnly -> {
                    Text("문자 전송 실패 · 전화만 연결", color = Palette.Amber, fontSize = GtType.H2, fontWeight = FontWeight.Bold)
                    Text("왼쪽 요약을 상담원에게 읽어 주세요", color = Palette.Ink, fontSize = GtType.Body)
                }
            }
        }
    }
}

/** E3 랑데부 비교 — 이 앱의 킬러 장면. 숫자 3개가 주인공, 최종 결정은 119 */
@Composable
fun RendezvousScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("응급", Palette.Alarm)
        Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("어떻게 갈까요?", color = Palette.Ink, fontSize = GtType.H2, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.background(Palette.Amber.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text("119 안내가 우선입니다", color = Palette.Amber, fontSize = GtType.Body, fontWeight = FontWeight.SemiBold)
                }
                state.emergencyKind.contactTargetMin?.let { target ->
                    Box(
                        Modifier.background(Palette.Panel2, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${state.emergencyKind.label} · ${target}분 안에 의료진 만나기 목표",
                            color = Palette.Ink, fontSize = GtType.Body, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            val options = state.rendezvousOptions
            val maxHosp = options.maxOf { it.hospital }
            val waitContact = options.first { it.kind == RendezvousKind.WAIT }.contact
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                options.forEach { o ->
                    val accent = if (o.recommended) Palette.Calm else Palette.Ink2
                    PanelCard(
                        Modifier.weight(1f).fillMaxSize(), emergency = true,
                        borderColor = if (o.recommended) Palette.Calm else null,
                    ) {
                        Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (o.recommended) {
                                Box(Modifier.background(Palette.Calm, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("추천", color = Palette.Ground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Box(Modifier.height(64.dp), contentAlignment = Alignment.CenterStart) {
                            Text(displayName(o.kind, state), color = Palette.Ink, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(
                            Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("의료진 만남", color = Palette.Ink2, fontSize = GtType.Body)
                            Text(
                                buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = if (o.recommended) Palette.Calm else Palette.Ink,
                                            fontSize = if (o.recommended) GtType.Display else 40.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    ) { append("${o.contact}") }
                                    withStyle(
                                        SpanStyle(
                                            color = if (o.recommended) Palette.Calm else Palette.Ink,
                                            fontSize = GtType.H2,
                                        ),
                                    ) { append("분") }
                                },
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("병원 도착 ${o.hospital}분", color = Palette.Ink2, fontSize = GtType.Body)
                            ai.seroai.goldentime.core.rendezvous.MeetPointSelector.meetsTarget(o, state.emergencyKind)?.let { ok ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (ok) "목표 시간 안" else "목표 시간 초과",
                                    color = if (ok) Palette.Calm else Palette.AlarmText,
                                    fontSize = GtType.Body, fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (o.recommended && waitContact > o.contact) {
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "대기보다 ${waitContact - o.contact}분 빨리 만나요",
                                    color = Palette.Calm, fontSize = GtType.Body, lineHeight = 26.sp,
                                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                                )
                            }
                        }
                        RendezvousTimeline(o.contact, o.hospital, maxHosp, accent)
                    }
                }
            }
            // 접선 후보 비교 — 졸음쉼터·휴게소·주유소·지구대 중 만남이 가장 빠른 곳 (동률이면 AED 보유)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("접선 후보", color = Palette.Ink2, fontSize = GtType.Body)
                val chosen = state.meetPoint
                state.meetPoints.sortedBy { ai.seroai.goldentime.core.rendezvous.MeetPointSelector.contact(it, state.rendezvousInput.handover) }
                    .forEach { p ->
                        val sel = p == chosen
                        Box(
                            Modifier.background(if (sel) Palette.Calm.copy(alpha = 0.16f) else Palette.Panel2, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "${p.displayName} ${ai.seroai.goldentime.core.rendezvous.MeetPointSelector.contact(p, state.rendezvousInput.handover)}분" +
                                    if (p.hasAed) " · AED" else "",
                                color = if (sel) Palette.Calm else Palette.Ink2,
                                fontSize = GtType.Body, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
            }
            PanelCard(Modifier.fillMaxWidth(), emergency = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val er = state.erCandidate
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Palette.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)) {
                                append("응급실 후보 · ${er.name} ")
                            }
                            val beds = er.erBeds
                            withStyle(
                                SpanStyle(
                                    color = if (beds == null || beds > 0) Palette.Calm else Palette.AlarmText,
                                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                ),
                            ) {
                                append(if (beds == null) "병상 확인 중" else if (beds > 0) "응급실 가용 ${beds}병상" else "응급실 포화")
                            }
                            er.distanceKm?.let { d ->
                                withStyle(SpanStyle(color = Palette.Ink2, fontSize = 20.sp)) { append(" · %.0fkm".format(d)) }
                            }
                            if (er.live) withStyle(SpanStyle(color = Palette.Ink2, fontSize = 15.sp)) { append("  실시간") }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    val rec = options.first { it.recommended }
                    BigButton(
                        when (rec.kind) {
                            RendezvousKind.MEET -> "접선 지점으로"
                            RendezvousKind.DIRECT -> "병원으로 안내"
                            RendezvousKind.WAIT -> "이 자리에서 대기"
                        },
                        color = Palette.Calm, textColor = Palette.PaperInk, fontSize = 20,
                    ) {
                        // 접선 지점을 차량 내비 경유지로 (NaviHelper addWaypoint). 119 안내가 우선
                        if (rec.kind == RendezvousKind.MEET) {
                            state.naviAction({}, "접선 지점 경유") {
                                addWaypoint(ai.seroai.goldentime.data.navi.Destination(state.meetPoint?.displayName ?: "접선 지점", 37.44250, 128.08271))
                            }
                        }
                        state.go(Screen.E4)
                    }
                }
            }
        }
    }
}

/** 랑데부 옵션 화면 표기 — 접선은 구체 지명으로 (E4와 일치) */
private fun displayName(kind: RendezvousKind, state: AppState): String = when (kind) {
    RendezvousKind.MEET -> "${state.meetPoint?.displayName ?: "접선 지점"}에서\n구급차 접선"
    else -> kind.label
}

/** E4 접선 지점 이동 — 정상 속도 (절대 원칙 5) */
@Composable
fun MeetNavScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("응급", Palette.Alarm)
        Row(Modifier.weight(1f).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(Modifier.weight(1.8f).fillMaxSize(), emergency = true) {
                RealMap(ai.seroai.goldentime.ui.RealMapMode.MEET, Modifier.weight(1f))
                // 범례 — 지도 위 표식 (실제 영동고속도로 구간, OpenStreetMap)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(Palette.Route to "내 차", Palette.Alarm to "구급차", Palette.Amber to "접선 지점").forEach { (c, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(14.dp).background(c, CircleShape))
                            Text(label, color = Palette.Ink2, fontSize = GtType.Caption)
                        }
                    }
                }
            }
            PanelCard(Modifier.weight(1f).fillMaxSize(), emergency = true) {
                CardTitle("접선 지점")
                Text(state.meetPoint?.displayName ?: "접선 지점", color = Palette.Ink, fontSize = GtType.H2, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Palette.Gold, fontSize = 40.sp, fontWeight = FontWeight.Bold)) { append("5분") }
                        withStyle(SpanStyle(color = Palette.Calm, fontSize = 24.sp, fontWeight = FontWeight.Bold)) { append("  6.2km") }
                    },
                )
                Text("구급차 도착 예상 6분", color = Palette.Ink, fontSize = GtType.Body)
                Text("가족에게 위치 공유 중", color = Palette.Ink2, fontSize = GtType.Body)
                Spacer(Modifier.height(12.dp))
                Text(FixedScripts.NORMAL_SPEED, color = Palette.Amber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                GhostButton("도착 (시연)", modifier = Modifier.fillMaxWidth()) { state.go(Screen.E5) }
            }
        }
    }
}

/** E5 도착·인계 — 정차 확인 후 도어 해제, 구급대원용 밝은 시트 */
@Composable
fun HandoverScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("응급", Palette.Alarm, driving = false)
        Row(Modifier.weight(1f).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(Modifier.weight(2.1f).fillMaxSize(), background = Palette.HandoverSurface, borderColor = Palette.Rule) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("구급대원 인계 정보", color = Palette.Ink, fontSize = GtType.Title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("차량에서 자동 정리 · 진단 아님", color = Palette.Ink2, fontSize = GtType.Caption)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    listOf(
                        "대상" to "4세 남아",
                        "증상" to "경련, 발열 (오늘 아침부터)",
                        "경련 시작" to "14:28 · 경과 ${state.elapsedEmergency()}",
                        "조치" to "옆으로 눕힘",
                        "병력·알레르기" to "열성경련 이력 · 알레르기 없음",
                    ).forEachIndexed { i, (k, v) ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Rule))
                        Row(Modifier.heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(k, color = Palette.Ink2, fontSize = GtType.Body, modifier = Modifier.width(132.dp))
                            Text(
                                v, color = if (k == "경련 시작") Palette.Gold else Palette.Ink,
                                fontSize = GtType.Title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            PanelCard(Modifier.weight(1f).fillMaxSize(), emergency = true) {
                Text("도착했어요", color = Palette.Calm, fontSize = GtType.H2, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                    CheckRow("정차 확인")
                    CheckRow("문 잠금 해제됨")
                    CheckRow("실내등 켜짐")
                }
                BigButton("이송 병원 따라가기", color = Palette.Calm, textColor = Palette.PaperInk, fontSize = 20, modifier = Modifier.fillMaxWidth()) {
                    state.resetEmergency()
                    state.go(Screen.H0)
                }
            }
        }
    }
}

/** E6 심정지 의심 — 고정 대본만, 랑데부 생략, 즉시 119 (절대 원칙 3·4) */
@Composable
fun CardiacArrestScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("심정지 의심", Palette.Alarm)
        PanelCard(Modifier.weight(1f).fillMaxWidth().padding(24.dp), emergency = true) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                Text("안전한 곳에 바로 멈추세요", color = Palette.Ink, fontSize = GtType.H1, fontWeight = FontWeight.Bold)
                Text(
                    "119에 위치를 보냈어요. 전화가 연결되면 상담원 안내에 따라 가슴 압박을 시작하세요",
                    color = Palette.Ink2, fontSize = GtType.Title, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 720.dp),
                )
                BigButton(
                    "119 전화 연결", fontSize = 28,
                    modifier = Modifier.fillMaxWidth(0.5f).height(80.dp),
                ) { state.onDial119() } // ACTION_DIAL — 자동 발신 아님. SMS 자동 발송 허용 여부는 설명회 확인
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CprBeatDots()
                    Text("압박 박자 · 분당 110회", color = Palette.AlarmText, fontSize = GtType.Title, fontWeight = FontWeight.SemiBold)
                }
                // 가장 가까운 자동심장충격기 — 국립중앙의료원 AED 위치정보(키 없으면 시연값)
                Text(
                    state.nearestAed.let { aed ->
                        "가까운 AED · ${aed.org}" + (aed.place?.let { " $it" } ?: "") +
                            (aed.distanceKm?.let { " · %.1fkm".format(it) } ?: "") +
                            " · ${ai.seroai.goldentime.emergency.FixedScripts.ARREST_AED}"
                    },
                    color = Palette.Ink, fontSize = GtType.Body,
                )
                SafeStopLine(state)
            }
        }
    }
}

/** E7 사고 자동 감지 — 에어백 신호 후 10초 응답 창. 카운트다운이 화면 중심 */
@Composable
fun CrashDetectScreen(state: AppState) {
    val detState = state.crashDetector.state(state.nowMs)
    Column(Modifier.fillMaxSize()) {
        TopBar("사고 감지", Palette.Alarm, driving = false)
        PanelCard(Modifier.weight(1f).fillMaxWidth().padding(24.dp), emergency = true) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                Text("충돌이 감지되었어요. 괜찮으세요?", color = Palette.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                val remaining = (detState as? CrashDetector.State.Asking)?.remainingSeconds ?: 0
                Box(
                    Modifier.size(160.dp).border(10.dp, Palette.Alarm, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$remaining", color = Palette.Gold, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                }
                Text("응답이 없으면 응급 모드로 바꾸고 119에 사고 위치를 보냅니다", color = Palette.Ink2, fontSize = GtType.Body)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GhostButton("괜찮아요", 20, Modifier.widthIn(min = 240.dp)) {
                        state.crashDetector.onDriverOk()
                        state.resetEmergency()
                        state.go(Screen.H0)
                    }
                    BigButton("응급 모드", fontSize = 20, modifier = Modifier.widthIn(min = 240.dp)) {
                        state.crash = true
                        state.go(Screen.E2)
                    }
                }
            }
        }
    }
}

/**
 * 안전 정차 지점 — 경로 앞 비상주차대·졸음쉼터·휴게소를 거리 순으로 (갓길 제외).
 * 안내만 한다: 차량 제어 없음, 위험도 점수 없음 (SafeStopPlanner 참고)
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SafeStopLine(state: AppState) {
    val stops = ai.seroai.goldentime.core.stop.SafeStopPlanner.ahead(state.safeStops)
    if (stops.isEmpty()) return
    // 좁은 카드(E2)에서는 칩이 다음 줄로 넘어간다
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("안전 정차", color = Palette.Ink2, fontSize = GtType.Body, modifier = Modifier.align(Alignment.CenterVertically))
        stops.forEachIndexed { i, stop ->
            Box(
                Modifier.background(if (i == 0) Palette.Amber.copy(alpha = 0.16f) else Palette.Panel2, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    "${stop.displayName} · ${ai.seroai.goldentime.core.stop.SafeStopPlanner.distanceText(stop.aheadMeters)}",
                    color = if (i == 0) Palette.Amber else Palette.Ink2,
                    fontSize = GtType.Body, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, softWrap = false,
                )
            }
        }
    }
}
