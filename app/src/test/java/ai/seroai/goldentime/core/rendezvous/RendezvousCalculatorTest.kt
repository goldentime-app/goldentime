package ai.seroai.goldentime.core.rendezvous

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * CLAUDE.md 테스트 값: (20,38,7,6,25,29,2) → 대기 22/51, 직접 38/38, 접선 9/34,
 * 즉시 처치 필요 시 추천 = 접선.
 */
class RendezvousCalculatorTest {

    private val input = RendezvousInput(
        ambToHere = 20, carToHospital = 38, carToPoint = 7, ambToPoint = 6,
        pointToHospital = 25, hereToHospital = 29, handover = 2,
    )

    private fun option(opts: List<RendezvousOption>, kind: RendezvousKind) =
        opts.first { it.kind == kind }

    @Test
    fun `기준 입력의 만남·병원 시간`() {
        val opts = RendezvousCalculator.compare(input, urgent = false)
        option(opts, RendezvousKind.WAIT).let { assertEquals(22, it.contact); assertEquals(51, it.hospital) }
        option(opts, RendezvousKind.DIRECT).let { assertEquals(38, it.contact); assertEquals(38, it.hospital) }
        option(opts, RendezvousKind.MEET).let { assertEquals(9, it.contact); assertEquals(34, it.hospital) }
    }

    @Test
    fun `즉시 처치 필요 시 만남 최소 = 접선 추천`() {
        val opts = RendezvousCalculator.compare(input, urgent = true)
        assertEquals(RendezvousKind.MEET, opts.first { it.recommended }.kind)
    }

    @Test
    fun `비긴급 시 만남 20분 이내 중 병원 최소`() {
        // 접선만 만남 ≤ 20 (9분) → 접선 추천
        val opts = RendezvousCalculator.compare(input, urgent = false)
        assertEquals(RendezvousKind.MEET, opts.first { it.recommended }.kind)
    }

    @Test
    fun `비긴급이고 20분 이내가 없으면 전체 중 병원 최소`() {
        val far = input.copy(carToPoint = 25, ambToPoint = 25) // 접선 만남 27분
        val opts = RendezvousCalculator.compare(far, urgent = false)
        // 대기 22/51, 직접 38/38, 접선 27/52 → 병원 최소 = 직접
        assertEquals(RendezvousKind.DIRECT, opts.first { it.recommended }.kind)
    }

    @Test
    fun `긴급 동률이면 병원 시간이 빠른 쪽`() {
        // 대기와 접선의 만남 시간을 같게 만든다
        val tie = input.copy(ambToHere = 7, carToPoint = 7, ambToPoint = 7) // 대기 9/38, 접선 9/34
        val opts = RendezvousCalculator.compare(tie, urgent = true)
        assertEquals(RendezvousKind.MEET, opts.first { it.recommended }.kind)
    }
}
