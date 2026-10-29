package ai.seroai.goldentime.ui.theme

import androidx.compose.ui.graphics.Color

/** 프로토타입(prototype/index.html)과 동일한 야간 계기판 팔레트 */
object Palette {
    val Ground = Color(0xFF0B0E12)
    val Dash = Color(0xFF0F1318)
    val Panel = Color(0xFF171D24)
    val Panel2 = Color(0xFF212A33)
    val Rule = Color(0xFF2B3540)
    val Ink = Color(0xFFEEF2F5)
    val Ink2 = Color(0xFF9AA7B4)
    val Calm = Color(0xFF37C3A6)      // 일상 케어
    val Amber = Color(0xFFF0A83A)     // 주의
    val Alarm = Color(0xFFE5484D)     // 응급
    val AlarmGround = Color(0xFF160C0E)
    val AlarmPanel = Color(0xFF221316)
    val Route = Color(0xFF4C8DF6)

    /** 응급 어두운 배경 위 작은 빨간 텍스트용 — Alarm보다 한 단계 밝아 대비 4.5:1 확보 */
    val AlarmText = Color(0xFFFF7A7F)

    /** 시간·카운트다운 숫자 전용 골드 ("골든타임" 서사) — 전면 도입 금지, 시간 숫자 한정 */
    val Gold = Color(0xFFE8B84B)
    /** 인계 시트 기본면 — 밤에 눈부시지 않게 어두운 문서형 (밝은 Paper는 정차 중 선택) */
    val HandoverSurface = Color(0xFF1B232B)
    val Paper = Color(0xFFF3F5F7)     // 인계 시트
    val PaperInk = Color(0xFF141820)
}
