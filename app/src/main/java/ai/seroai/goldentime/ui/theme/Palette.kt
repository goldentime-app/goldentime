package ai.seroai.goldentime.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Apple HIG 다크 시스템 팔레트 기반 — 검정 바탕 위 회색 단계로 깊이를 만들고,
 * 테두리 대신 면의 밝기 차이로 위계를 나눈다. 강조색은 화면마다 하나만.
 */
object Palette {
    val Ground = Color(0xFF000000)    // systemBackground
    val Dash = Color(0xFF000000)
    val Panel = Color(0xFF1C1C1E)     // secondarySystemBackground
    val Panel2 = Color(0xFF2C2C2E)    // tertiarySystemBackground
    val Rule = Color(0xFF3A3A3C)      // separator — 선이 꼭 필요할 때만
    val Ink = Color(0xFFF5F5F7)       // label
    val Ink2 = Color(0xFF98989F)      // secondaryLabel
    val Calm = Color(0xFF63E6BE)      // 일상 케어 (systemMint 계열)
    val Amber = Color(0xFFFF9F0A)     // 주의 (systemOrange)
    val Alarm = Color(0xFFFF453A)     // 응급 (systemRed)
    val AlarmGround = Color(0xFF0D0505)
    val AlarmPanel = Color(0xFF1F1314)
    val Route = Color(0xFF0A84FF)     // systemBlue

    /** 응급 어두운 배경 위 작은 빨간 텍스트용 — Alarm보다 한 단계 밝아 대비 4.5:1 확보 */
    val AlarmText = Color(0xFFFF6961)

    /** 시간·카운트다운 숫자 전용 골드 ("골든타임" 서사) — 전면 도입 금지, 시간 숫자 한정 */
    val Gold = Color(0xFFFFD25F)
    /** 인계 시트 기본면 — 밤에 눈부시지 않게 어두운 문서형 (밝은 Paper는 정차 중 선택) */
    val HandoverSurface = Color(0xFF1C1C1E)
    val Paper = Color(0xFFF5F5F7)     // 인계 시트
    val PaperInk = Color(0xFF1D1D1F)
}
