package ai.seroai.goldentime.data.location

/**
 * 위치·속도 추상화 — Pleos 구현은 PleosLocationSource(FusedLocation SDK),
 * 에뮬레이터처럼 위치가 안 들어오면 FakeLocationSource 속도로 대체.
 * 속도는 위치 변화로 추정한다 (Pleos에 속도 API 없음).
 */
interface LocationSource {
    /** 추정 속도 (km/h). 신호 없으면 null */
    fun speedKmh(): Double?
}

/** 시연·에뮬레이터용: 에뮬레이터 GPS는 정지 상태라 고정 순항 속도를 가정 */
class FakeLocationSource(var cruiseKmh: Double = 60.0) : LocationSource {
    override fun speedKmh(): Double = cruiseKmh
}
