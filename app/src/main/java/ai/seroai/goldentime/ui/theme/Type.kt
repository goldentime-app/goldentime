package ai.seroai.goldentime.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 타입 스케일 — 차량 디스플레이(2560px, 시청거리 ~70cm) 기준 6단계로 강제 통일.
 * 디자인 리뷰 합의: 11~12sp 전면 금지, 핵심 숫자는 Display급.
 */
object GtType {
    /** 게이지 숫자, E3 "9분", E7 카운트다운 */
    val Display = 56.sp

    /** 화면 1순위 메시지 (E6 "안전한 곳에 바로 멈추세요") */
    val H1 = 40.sp

    /** 카드 핵심 값 ("연속 주행 1시간 35분", "○○ 졸음쉼터") */
    val H2 = 28.sp

    /** 카드 제목급 본문, 체크리스트 */
    val Title = 24.sp

    /** 보조 설명, 메타 정보 */
    val Body = 19.sp

    /** 카드 라벨(오버라인), 단위 */
    val Caption = 16.sp

    /**
     * 앱 전체 기본 글자 스타일 — 큰 글자일수록 촘촘해 보이도록 자간을 글자 크기 비례(-1.5%)로 줄이고,
     * 숫자는 고정폭(tnum)이라 카운트다운이 흔들리지 않는다.
     */
    val Base = TextStyle(
        letterSpacing = (-0.015).em,
        fontFeatureSettings = "tnum",
        // 한국어 어절 단위 줄바꿈 (CSS keep-all) — "마/세요"처럼 단어 중간에서 끊기지 않게
        lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
    )
}

/** 모션 — 들어올 때는 스프링 같은 감속, 상태 변화는 표준 곡선 */
object GtMotion {
    val Enter = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    const val ENTER_MS = 520
    const val STATE_MS = 240
}
