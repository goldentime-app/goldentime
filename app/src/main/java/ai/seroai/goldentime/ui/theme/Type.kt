package ai.seroai.goldentime.ui.theme

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
}
