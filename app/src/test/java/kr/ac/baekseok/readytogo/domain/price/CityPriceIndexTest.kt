package kr.ac.baekseok.readytogo.domain.price

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CityPriceIndexTest {

    /** 중앙값이 원하는 값이 되도록 표본을 만든다. */
    private fun sample(median: Long, size: Int = 11): List<Long> =
        List(size) { index -> if (index < size / 2) median / 2 else median }

    @Test
    fun `표본이 부족하면 배수를 적용하지 않는다`() {
        val nine = List(CityPriceIndex.MIN_SAMPLE_SIZE - 1) { 5_000L }

        assertEquals(1.0, CityPriceIndex.of(nine), 0.0001)
        assertEquals("빈 목록도 마찬가지다", 1.0, CityPriceIndex.of(emptyList()), 0.0001)
    }

    @Test
    fun `기준 금액과 같으면 배수는 1이다`() {
        val prices = sample(CityPriceIndex.BASELINE_MEAL_MEDIAN_WON)

        assertEquals(1.0, CityPriceIndex.of(prices), 0.0001)
    }

    @Test
    fun `물가가 싼 도시는 1보다 작은 배수가 나온다`() {
        // 한 끼 중앙값 12,500원 → 기준의 절반
        val prices = sample(12_500L)

        assertEquals(0.5, CityPriceIndex.of(prices), 0.0001)
    }

    @Test
    fun `물가가 비싼 도시는 1보다 큰 배수가 나온다`() {
        // 한 끼 중앙값 37,500원 → 기준의 1.5배
        val prices = sample(37_500L)

        assertEquals(1.5, CityPriceIndex.of(prices), 0.0001)
    }

    @Test
    fun `아주 싼 도시도 하한에서 잘린다`() {
        // 자카르타처럼 한 끼 3,000원이어도 숙소까지 8분의 1로 떨어뜨리지는 않는다.
        val prices = sample(3_000L)

        assertEquals(CityPriceIndex.MIN_MULTIPLIER, CityPriceIndex.of(prices), 0.0001)
    }

    @Test
    fun `아주 비싼 표본도 상한에서 잘린다`() {
        // 고급 식당에 표본이 쏠리면 숙소가 전부 예산 초과로 밀려난다. 그것을 막는다.
        val prices = sample(200_000L)

        assertEquals(CityPriceIndex.MAX_MULTIPLIER, CityPriceIndex.of(prices), 0.0001)
    }

    @Test
    fun `가격이 모두 0이어도 배수가 깨지지 않는다`() {
        val prices = List(20) { 0L }

        assertEquals(1.0, CityPriceIndex.of(prices), 0.0001)
    }

    @Test
    fun `배수는 항상 정해진 범위 안에 있다`() {
        listOf(1L, 100L, 25_000L, 1_000_000L).forEach { median ->
            val index = CityPriceIndex.of(sample(median))
            assertTrue(
                "중앙값 $median 에서 범위를 벗어났다: $index",
                index >= CityPriceIndex.MIN_MULTIPLIER && index <= CityPriceIndex.MAX_MULTIPLIER,
            )
        }
    }
}
