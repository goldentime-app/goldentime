package ai.seroai.goldentime.ui

import ai.seroai.goldentime.demo.DemoPlayer
import ai.seroai.goldentime.demo.DemoScenarios
import ai.seroai.goldentime.emergency.CrashDetector
import ai.seroai.goldentime.emergency.Screen
import ai.seroai.goldentime.ui.screens.CardiacArrestScreen
import ai.seroai.goldentime.ui.screens.CautionScreen
import ai.seroai.goldentime.ui.screens.CrashDetectScreen
import ai.seroai.goldentime.ui.screens.EmergencyListenScreen
import ai.seroai.goldentime.ui.screens.FamilyProfileScreen
import ai.seroai.goldentime.ui.screens.ForecastScreen
import ai.seroai.goldentime.ui.screens.HandoverScreen
import ai.seroai.goldentime.ui.screens.HomeScreen
import ai.seroai.goldentime.ui.screens.MeetNavScreen
import ai.seroai.goldentime.ui.screens.MotionCareScreen
import ai.seroai.goldentime.ui.screens.RendezvousScreen
import ai.seroai.goldentime.ui.screens.Summary119Screen
import ai.seroai.goldentime.ui.theme.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 루트 컴포저블 — 1920×720 가로 차량 화면 기준.
 * 하단에 시연 모드 바(시나리오 선택·단계 이동)를 둔다. 실차 빌드에서는 숨김 가능.
 */
@Composable
fun GoldenTimeApp(state: AppState = remember { AppState() }) {
    var toast by remember { mutableStateOf("") }
    var toastAt by remember { mutableStateOf(0L) }
    val showToast: (String) -> Unit = { toast = it; toastAt = state.clock() }
    val player = remember { DemoPlayer(state, showToast) }
    var demoBarVisible by remember { mutableStateOf(false) }

    // 1초 틱: 카운트다운·경과 시간 갱신 + E7 무응답 전이
    LaunchedEffect(Unit) {
        while (true) {
            state.nowMs = state.clock()
            if (state.screen == Screen.E7 &&
                state.crashDetector.state(state.nowMs) is CrashDetector.State.Unresponsive
            ) {
                state.crash = true
                state.go(Screen.E2)
            }
            if (toast.isNotEmpty() && state.nowMs - toastAt > 2600) toast = ""
            delay(250)
        }
    }

    LaunchedEffect(state.demoJump) {
        state.demoJump?.let { (key, step) -> player.jump(key, step); state.demoJump = null }
    }

    // 160dpi 차량 화면(앱 영역 ~1700px)에서 dp 레이아웃이 깨알처럼 작아지는 문제:
    // 논리 폭이 DESIGN_WIDTH가 되도록 밀도를 키운다 (해상도·dpi가 달라도 같은 비율)
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val scale = (maxWidth / DESIGN_WIDTH).coerceAtLeast(1f)
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
    Column(Modifier.fillMaxSize().background(Palette.Ground)) {
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(if (state.screen.isEmergency) Palette.AlarmGround else Palette.Dash),
        ) {
            when (state.screen) {
                Screen.H0 -> HomeScreen(state)
                Screen.H1 -> ForecastScreen(state, showToast)
                Screen.H2 -> MotionCareScreen(state, showToast)
                Screen.H3 -> CautionScreen(state, showToast)
                Screen.E1 -> EmergencyListenScreen(state)
                Screen.E2 -> Summary119Screen(state)
                Screen.E3 -> RendezvousScreen(state)
                Screen.E4 -> MeetNavScreen(state)
                Screen.E5 -> HandoverScreen(state)
                Screen.E6 -> CardiacArrestScreen(state)
                Screen.E7 -> CrashDetectScreen(state)
                Screen.S1 -> FamilyProfileScreen(state)
            }
            // 시연 바 토글: 상단 "골든타임" 제목을 길게 누르기 (기본은 숨김 — 제품 화면과 분리)
            Box(
                Modifier.align(Alignment.TopStart).size(width = 150.dp, height = 56.dp)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { demoBarVisible = !demoBarVisible }) },
            )
            // 토스트(SDK 호출 표시)는 하단 행동 영역을 가리지 않게 위쪽에
            if (toast.isNotEmpty()) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
                        .background(Palette.Ground.copy(alpha = 0.92f), RoundedCornerShape(50))
                        .border(1.dp, Palette.Rule, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(toast, color = Palette.Ink, fontSize = 16.sp)
                }
            }
        }
        if (demoBarVisible) DemoBar(state, player)
    }
    }
    }
}

/** 디자인 기준 논리 폭 — 차량 중앙 화면 앱 영역 */
private val DESIGN_WIDTH = 1000.dp

/** 시연 모드 바 — 실사용 UI와 분리감 없게 슬림·저대비 (리뷰 공통 4). 실차 빌드에서는 숨김 */
@Composable
private fun DemoBar(state: AppState, player: DemoPlayer) {
    Row(
        Modifier.fillMaxWidth().background(Palette.Ground).padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("시연", color = Palette.Rule, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        DemoScenarios.all.forEach { s ->
            val selected = player.scenario.key == s.key
            Box(
                Modifier
                    .background(if (selected) Palette.Panel2 else Palette.Ground, RoundedCornerShape(50))
                    .clickable { player.select(s) }
                    .padding(horizontal = 9.dp, vertical = 2.dp),
            ) {
                Text(s.key, color = if (selected) Palette.Ink else Palette.Rule, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("${player.scenario.title} · ${state.sdkStatus}", color = Palette.Rule, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(
            if (player.stepIndex >= 0) "${player.stepIndex + 1}/${player.scenario.steps.size}" else "-",
            color = Palette.Rule, fontSize = 11.sp,
        )
        DemoButton("◀ 이전") { player.prev() }
        DemoButton("다음 ▶") { player.next() }
        DemoButton("처음") { player.reset() }
        DemoButton("에어백(E7)") {
            state.crashDetector.onAirbagDeployed(state.clock())
            state.go(Screen.E7)
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Palette.Panel2.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(label, color = Palette.Ink2, fontSize = 11.sp)
    }
}
