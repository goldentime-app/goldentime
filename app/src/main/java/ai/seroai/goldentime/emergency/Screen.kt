package ai.seroai.goldentime.emergency

/** 화면 ID — design/ 시안·prototype/index.html과 동일한 명명. */
enum class Screen(val label: String) {
    H0("홈"),
    H1("멀미 예보"),
    H2("멀미 대응"),
    H3("주의 모드"),
    E1("응급 접수"),
    E2("요약·119"),
    E3("랑데부 비교"),
    E4("접선 이동"),
    E5("인계"),
    E6("심정지 의심"),
    E7("사고 감지"),
    S1("가족 프로필"),
    ;

    val isEmergency: Boolean get() = name.startsWith("E")
}
