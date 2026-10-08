package kr.ac.baekseok.readytogo.domain.budget

import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetAllocatorTest {

    /** 4박 5일(끼니 15번) 도쿄 여행. */
    private fun condition(budgetWon: Long?) = TripCondition(
        destination = kr.ac.baekseok.readytogo.TestDestinations.TOKYO,
        departureDate = LocalDate.of(2026, 9, 24),
        returnDate = LocalDate.of(2026, 9, 28),
        budgetWon = budgetWon,
    )

    /**
     * 인원이 늘면 **1인·1실 몫이 줄어든다.** 총예산은 그대로이기 때문이다.
     *
     * 이 계산이 틀리면 4명 여행에서 1인 10만원짜리 식당이 "예산 내"로 잡힌다.
     * 식당 가격은 1인분, 숙소 가격은 1실 기준으로 들어오므로 상한도 같은 기준이어야 한다.
     */
    @Test
    fun `인원이 늘면 1인 식사 상한이 인원수만큼 내려간다`() {
        val solo = allocateBudget(condition(1_500_000), cheapestFlightWon = 500_000)
        val four = allocateBudget(
            condition(1_500_000).copy(adults = 4),
            cheapestFlightWon = 500_000,
        )

        assertEquals(20_000L, solo.mealWon)
        // 같은 남은 예산을 네 배의 끼니가 나눠 쓴다.
        assertEquals(5_000L, four.mealWon)
    }

    @Test
    fun `숙소 상한은 방 수로 나눈다`() {
        // 4명이면 2인 1실로 2실. 1실 상한은 혼자일 때의 절반이다.
        val four = allocateBudget(
            condition(1_500_000).copy(adults = 4),
            cheapestFlightWon = 500_000,
        )

        assertEquals(2, four.roomCount)
        assertEquals(56_250L, four.lodgingPerNightWon)
    }

    @Test
    fun `홀수 인원은 방을 올림한다`() {
        // 3명이 2인 1실이면 한 명이 남는다. 2실이 필요하다.
        assertEquals(2, TripCondition(adults = 3).roomCount)
        assertEquals(1, TripCondition(adults = 1).roomCount)
        assertEquals(1, TripCondition(adults = 2).roomCount)
        assertEquals(3, TripCondition(adults = 5).roomCount)
        assertEquals(3, TripCondition(adults = 6).roomCount)
    }

    @Test
    fun `인원을 정하지 않으면 1명으로 본다`() {
        val result = allocateBudget(condition(1_500_000), cheapestFlightWon = 500_000)
        assertEquals(1, result.adults)
        assertEquals(1, result.roomCount)
    }

    @Test
    fun `항공료를 뺀 나머지를 45대30대25로 나눈다`() {
        val result = allocateBudget(condition(1_500_000), cheapestFlightWon = 500_000)

        assertEquals(1_000_000L, result.remainingWon)
        // 숙박 45% = 45만원 ÷ 4박
        assertEquals(112_500L, result.lodgingPerNightWon)
        // 식비 30% = 30만원 ÷ (5일 × 3끼)
        assertEquals(20_000L, result.mealWon)
        // 기타 25%
        assertEquals(250_000L, result.otherWon)
        assertFalse(result.flightAloneExceedsBudget)
    }

    @Test
    fun `항공권이 비싸면 숙소와 식사 상한이 자동으로 내려간다`() {
        val cheap = allocateBudget(condition(1_500_000), cheapestFlightWon = 300_000)
        val expensive = allocateBudget(condition(1_500_000), cheapestFlightWon = 900_000)

        assertTrue(
            "항공료가 오르면 숙소 상한은 내려가야 한다",
            expensive.lodgingPerNightWon!! < cheap.lodgingPerNightWon!!
        )
        assertTrue(expensive.mealWon!! < cheap.mealWon!!)
    }

    @Test
    fun `항공료만으로 예산을 넘기면 남은 예산은 0이고 경고 표시가 켜진다`() {
        val result = allocateBudget(condition(500_000), cheapestFlightWon = 800_000)

        assertEquals(0L, result.remainingWon)
        assertEquals(0L, result.lodgingPerNightWon)
        assertEquals(0L, result.mealWon)
        assertTrue(result.flightAloneExceedsBudget)
    }

    @Test
    fun `예산을 입력하지 않으면 상한이 없다`() {
        val result = allocateBudget(condition(budgetWon = null), cheapestFlightWon = 500_000)

        assertNull("null은 상한 없음이라는 뜻이다", result.lodgingPerNightWon)
        assertNull(result.mealWon)
        assertNull(result.remainingWon)
        assertFalse(result.flightAloneExceedsBudget)
    }

    @Test
    fun `항공권 검색 결과가 없으면 항공료를 0으로 보고 나눈다`() {
        val result = allocateBudget(condition(1_000_000), cheapestFlightWon = null)

        assertEquals(1_000_000L, result.remainingWon)
        assertNull(result.flightWon)
        assertFalse(result.flightAloneExceedsBudget)
    }

    @Test
    fun `당일치기여도 0으로 나누지 않는다`() {
        val sameDay = TripCondition(
            destination = kr.ac.baekseok.readytogo.TestDestinations.OSAKA,
            departureDate = LocalDate.of(2026, 9, 24),
            returnDate = LocalDate.of(2026, 9, 24),
            budgetWon = 600_000,
        )

        val result = allocateBudget(sameDay, cheapestFlightWon = 200_000)

        // 0박이지만 계산은 1박 기준으로 떨어진다. 숙소 검색은 호출부가 건너뛴다.
        assertEquals(180_000L, result.lodgingPerNightWon)
        assertTrue(result.mealWon!! > 0L)
    }

    @Test
    fun `여행 목적에 따라 배분이 달라진다`() {
        // 맛집을 고르면 식비 비중이 오르고 숙박이 내려간다.
        val foodTrip = allocateBudget(
            condition(1_500_000).copy(purposes = setOf(TravelPurpose.FOOD)),
            cheapestFlightWon = 500_000,
        )
        val restTrip = allocateBudget(
            condition(1_500_000).copy(purposes = setOf(TravelPurpose.REST)),
            cheapestFlightWon = 500_000,
        )

        assertTrue("맛집이 휴양보다 1끼 상한이 높아야 한다", foodTrip.mealWon!! > restTrip.mealWon!!)
        assertTrue(
            "휴양이 맛집보다 1박 상한이 높아야 한다",
            restTrip.lodgingPerNightWon!! > foodTrip.lodgingPerNightWon!!,
        )
    }
}