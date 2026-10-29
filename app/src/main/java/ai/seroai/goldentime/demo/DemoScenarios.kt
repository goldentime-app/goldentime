package ai.seroai.goldentime.demo

import ai.seroai.goldentime.emergency.Screen

/**
 * 시연 모드 — prototype/index.html의 시나리오 A~E와 동일한 단계.
 * 에뮬레이터에 없는 신호(에어백 등)는 이 단계 재생으로 주입한다 (README 3-3).
 */
data class DemoStep(
    val screen: Screen,
    val userSays: String? = null,
    val appSays: String? = null,
    val toast: String? = null,
    /** 멀미 위험도 강제 (H 화면) */
    val riskPercent: Int? = null,
    val crash: Boolean = false,
    /** E2 진입 시 전송을 이미 보낸 상태로 (대본 "보냈어요" 단계) */
    val alreadySent: Boolean = false,
)

data class DemoScenario(val key: String, val title: String, val steps: List<DemoStep>)

object DemoScenarios {
    val A = DemoScenario(
        "A", "고속도로 열성경련 · 랑데부",
        listOf(
            DemoStep(Screen.E1, userSays = "아이가 경련해!"),
            DemoStep(Screen.E1, appSays = "응급 모드를 시작할게요. 아이가 몇 살인가요?"),
            DemoStep(Screen.E1, userSays = "네 살이요. 아침부터 열이 있었어요."),
            DemoStep(Screen.E1, appSays = "경련이 시작된 지 얼마나 됐나요?"),
            DemoStep(Screen.E1, userSays = "일이 분 된 것 같아요."),
            DemoStep(Screen.E2, appSays = "119에 위치와 상황 요약을 먼저 보냈어요. 전화가 연결되면 상담원 질문에만 답해 주세요.", alreadySent = true),
            DemoStep(Screen.E2, appSays = "아이를 옆으로 눕히고, 입에는 아무것도 넣지 마세요. 억지로 붙잡지 마세요. 경련 시간을 재고 있어요.", alreadySent = true),
            DemoStep(Screen.E3, appSays = "119 안내가 가장 우선이에요. 참고로, 8킬로미터 앞 졸음쉼터에서 구급차를 만나면 9분 안에 의료진을 만날 수 있어요. 그쪽으로 안내할까요?"),
            DemoStep(Screen.E4, userSays = "응, 거기로 가자.", appSays = "졸음쉼터까지 6킬로미터, 약 5분이에요. 정상 속도로 운전하세요. 구급차는 6분 뒤 도착 예정이에요."),
            DemoStep(Screen.E4, appSays = "곧 졸음쉼터예요. 오른쪽으로 들어가 안전하게 멈추세요.", toast = "접선 지점 반경 300m 진입 (addProximityAlert)"),
            DemoStep(Screen.E5, appSays = "문 잠금을 풀었어요. 구급대원에게 화면을 보여 주세요."),
        ),
    )

    val B = DemoScenario(
        "B", "사고 자동 감지 · 에어백",
        listOf(
            DemoStep(Screen.E7, appSays = "충돌이 감지되었어요. 괜찮으세요? 대답이 없으면 응급 모드로 바꿀게요.", toast = "registerSeatAirbagDeployed 신호 수신"),
            DemoStep(Screen.E7, toast = "10초 동안 응답 없음"),
            DemoStep(Screen.E2, crash = true, appSays = "응급 모드로 바꿨어요. 3초 뒤 119에 사고 정보를 보내요."),
            DemoStep(Screen.E2, crash = true, appSays = "119에 사고 위치와 탑승 인원을 먼저 보냈어요. 안전한 곳에 있으면 비상등을 켜세요.", alreadySent = true),
        ),
    )

    val C = DemoScenario(
        "C", "심정지 의심 · 고정 대본",
        listOf(
            DemoStep(Screen.E1, userSays = "아빠가 숨을 안 쉬어!"),
            DemoStep(Screen.E6, appSays = "안전한 곳에 바로 멈추세요. 119에 위치를 보내고 전화를 연결할게요.", toast = "심정지 키워드 감지 → 랑데부 계산 생략 · 119 문자신고 즉시 전송"),
            DemoStep(Screen.E6, appSays = "상담원 안내에 따라 가슴 한가운데를 세게, 빠르게 누르세요. 박자를 맞춰 드릴게요.", toast = "고정 대본 · 생성형 문구 사용 안 함"),
        ),
    )

    val D = DemoScenario(
        "D", "아이 멀미 · 예보와 대응",
        listOf(
            DemoStep(Screen.H1, appSays = "오늘 경로는 굽은 길이 38분이에요. 9분 더 걸리지만 편안한 경로로 가면 예상 흔들림이 절반 수준이에요. 바꿀까요?"),
            DemoStep(Screen.H0, userSays = "그래, 바꿔줘.", toast = "changeRouteOption(COMFORTABLE)", riskPercent = 22),
            DemoStep(Screen.H0, appSays = "곧 오른쪽으로 돌아요.", toast = "회전 예고 · TBT 회전 300m 전", riskPercent = 48),
            DemoStep(Screen.H2, appSays = "뒷좌석 멀미 위험이 높아지고 있어요. 화면을 끄고 앞 창밖을 보게 해 주세요. 6킬로미터 앞 휴게소에 들를까요?", riskPercent = 76),
            DemoStep(Screen.H0, userSays = "그래.", appSays = "휴게소를 경유지로 넣었어요. 아이가 괜찮은지 0부터 10까지로 물어봐 주세요.", toast = "addWaypoint(휴게소)", riskPercent = 76),
        ),
    )

    val E = DemoScenario(
        "E", "아이 발열 · 주의 모드",
        listOf(
            DemoStep(Screen.H3, userSays = "아이가 열이 나.", appSays = "숨쉬기 힘들어하거나 경련하나요?"),
            DemoStep(Screen.H3, userSays = "아니, 열만 있어.", appSays = "가까운 곳 세 군데를 찾았어요. 첫 번째, ○○아동병원 야간진료, 9분 거리예요. 들를까요?"),
            DemoStep(Screen.H0, userSays = "첫 번째.", appSays = "○○아동병원을 경유지로 넣었어요. 경련하거나 숨쉬기 힘들어하면 응급 모드라고 말해 주세요.", toast = "addWaypoint(○○아동병원)"),
        ),
    )

    val all = listOf(A, B, C, D, E)
}
