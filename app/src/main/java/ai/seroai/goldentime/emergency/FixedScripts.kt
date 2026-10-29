package ai.seroai.goldentime.emergency

/**
 * 생명 관련 고정 대본 (절대 원칙 3) — LLM이 생성하지 않는다. 검수된 상수 문자열만 쓴다.
 *
 * 출처: prototype/index.html 시나리오 대본.
 * TODO: docs/화면흐름_음성대본_원문.md 4절 원문 확보 후 문구 일치 재검수 (docs/ 폴더가 handoff에 없음).
 */
object FixedScripts {

    // --- 심정지 의심 (E6) — 카운트다운 없이 즉시 ---
    const val ARREST_STOP = "안전한 곳에 바로 멈추세요. 119에 위치를 보내고 전화를 연결할게요."
    const val ARREST_CPR = "상담원 안내에 따라 가슴 한가운데를 세게, 빠르게 누르세요. 박자를 맞춰 드릴게요."
    const val ARREST_BEAT = "압박 박자 ● ● ● ● · 분당 110회"

    /** 가까운 AED 안내 — 동승자가 있을 때만, 운전자는 압박을 멈추지 않는다 */
    const val ARREST_AED = "다른 탑승자가 가져와 주세요"

    // --- 사고 감지 (E7) ---
    const val CRASH_ASK = "충돌이 감지되었어요. 괜찮으세요? 대답이 없으면 응급 모드로 바꿀게요."
    const val CRASH_TO_EMERGENCY = "응급 모드로 바꿨어요. 3초 뒤 119에 사고 정보를 보내요."
    const val CRASH_SENT = "119에 사고 위치와 탑승 인원을 먼저 보냈어요. 안전한 곳에 있으면 비상등을 켜세요."

    // --- 119 전송 (E2) ---
    const val SEND_NOTICE = "119에 위치와 상황 요약을 먼저 보냈어요. 전화가 연결되면 상담원 질문에만 답해 주세요."
    const val SEND_COUNTDOWN = "초 뒤 119에 요약을 보내요"
    const val SEND_CANCEL_LABEL = "보내지 않기"
    const val SEIZURE_CARE = "아이를 옆으로 눕히고, 입에는 아무것도 넣지 마세요. 억지로 붙잡지 마세요. 경련 시간을 재고 있어요."

    // --- 과속 유도 금지 (절대 원칙 5) ---
    const val NORMAL_SPEED = "정상 속도로 운전하세요"

    /** 랑데부 안내 — 119 우선 원칙을 항상 앞에 둔다 (절대 원칙 2) */
    const val RDV_PREFIX = "119 안내가 가장 우선이에요."
}
