package kr.ac.baekseok.readytogo.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 예산 범위가 인원에 비례하는지 본다.
 *
 * 범위를 1인 기준으로 고정했더니 3명·100만원 도쿄 7일에서 항공료가 99만원이라
 * 남은 돈이 3천원이 되고 끼니 21칸이 전부 예산 초과로 찍혔다. 그 일을 막는 규칙이다.
 */
class BudgetRangeTest {

    @Test
    fun `범위가 인원에 비례한다`() {
        assertEquals(300_000L, BudgetRange.minFor(1))
        assertEquals(5_000_000L, BudgetRange.maxFor(1))

        assertEquals(900_000L, BudgetRange.minFor(3))
        assertEquals(15_000_000L, BudgetRange.maxFor(3))
    }

    @Test
    fun `빠른 선택 금액도 인원에 비례한다`() {
        assertEquals(
            listOf(500_000L, 1_000_000L, 1_500_000L, 2_000_000L),
            BudgetRange.quickOptionsFor(1),
        )
        assertEquals(
            listOf(1_500_000L, 3_000_000L, 4_500_000L, 6_000_000L),
            BudgetRange.quickOptionsFor(3),
        )
    }

    @Test
    fun `범위 안의 금액은 인원이 바뀌어도 그대로 둔다`() {
        // 1명 100만원 → 3명의 범위는 90만~1500만이라 100만원은 여전히 유효하다.
        // 애써 맞춰둔 예산이 인원을 한 명 늘렸다고 바뀌면 안 된다.
        assertEquals(1_000_000L, BudgetRange.coerce(1_000_000L, adults = 3))
    }

    @Test
    fun `범위를 벗어나면 가장 가까운 끝으로 옮긴다`() {
        // 1명 50만원에서 3명으로 늘리면 하한이 90만원이 된다.
        assertEquals(900_000L, BudgetRange.coerce(500_000L, adults = 3))

        // 3명 1200만원에서 1명으로 줄이면 상한이 500만원이 된다.
        assertEquals(5_000_000L, BudgetRange.coerce(12_000_000L, adults = 1))
    }
}
