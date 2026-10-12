package kr.ac.baekseok.readytogo.data.model

import kr.ac.baekseok.readytogo.TestDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * "여행 중"과 "몇 일차"의 경계를 본다.
 *
 * 경계가 틀리면 여행 중 모드가 하루 일찍/늦게 켜지거나, 마지막 날에 없는 일차를 열어
 * 일정표가 빈 화면이 된다. 날짜 계산은 눈으로 확인하기 어려워 테스트로 고정한다.
 */
class SavedTripDayTest {

    private val trip = SavedTrip(
        id = "t",
        title = "도쿄 5일",
        destinationName = "도쿄",
        departureDate = LocalDate.of(2026, 11, 3),
        returnDate = LocalDate.of(2026, 11, 7),
        condition = TripCondition(
            origin = TestDestinations.INCHEON,
            destination = TestDestinations.TOKYO,
            departureDate = LocalDate.of(2026, 11, 3),
            returnDate = LocalDate.of(2026, 11, 7),
        ),
        savedAt = LocalDate.of(2026, 10, 12),
    )

    @Test
    fun `출발일과 귀국일 당일도 여행 중이다`() {
        assertTrue(trip.isOngoing(LocalDate.of(2026, 11, 3)))
        assertTrue(trip.isOngoing(LocalDate.of(2026, 11, 7)))
        assertFalse(trip.isOngoing(LocalDate.of(2026, 11, 2)))
        assertFalse(trip.isOngoing(LocalDate.of(2026, 11, 8)))
    }

    @Test
    fun `일차는 출발일을 1일차로 센다`() {
        assertEquals(1, trip.dayNumberOn(LocalDate.of(2026, 11, 3)))
        assertEquals(3, trip.dayNumberOn(LocalDate.of(2026, 11, 5)))
        // 마지막 일차는 dayCount와 같아야 한다. 하나라도 크면 없는 날을 열게 된다.
        assertEquals(trip.dayCount, trip.dayNumberOn(LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun `여행 기간이 아니면 일차가 없다`() {
        assertNull(trip.dayNumberOn(LocalDate.of(2026, 11, 2)))
        assertNull(trip.dayNumberOn(LocalDate.of(2026, 11, 8)))
    }
}
