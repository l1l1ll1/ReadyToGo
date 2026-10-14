package kr.ac.baekseok.readytogo.data.model

import kr.ac.baekseok.readytogo.TestDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 저장된 일정의 기간 변경.
 *
 * 편집을 가져갈지 버릴지, 문서 id가 새 날짜를 따라가는지를 고정한다. id가 어긋나면
 * 결과 화면이 일정을 못 찾아 "저장" 버튼이 다시 켜지고 편집이 사라진다.
 */
class SavedTripDatesTest {

    private val departure = LocalDate.of(2026, 11, 3)
    private val returning = LocalDate.of(2026, 11, 7)

    private val edits = TimelineEdits(
        deleted = mapOf(2 to setOf("visit-a")),
        done = mapOf(1 to setOf("meal-LUNCH")),
    )

    private val condition = TripCondition(
        origin = TestDestinations.INCHEON,
        destination = TestDestinations.TOKYO,
        departureDate = departure,
        returnDate = returning,
        budgetWon = 1_500_000,
    )

    private val trip = SavedTrip(
        id = SavedTrip.idFor(condition)!!,
        title = "도쿄 5일",
        destinationName = "도쿄",
        departureDate = departure,
        returnDate = returning,
        condition = condition,
        savedAt = LocalDate.of(2026, 10, 12),
        edits = edits,
        coverPlaceId = "cover",
    )

    private val autoTitle: (Int) -> String = { days -> "도쿄 ${days}일" }

    @Test
    fun `일수가 같으면 편집을 그대로 가져가고 id와 조건이 새 날짜를 따른다`() {
        val moved = trip.withDates(departure.plusDays(7), returning.plusDays(7), autoTitle)

        assertEquals(edits, moved.edits)
        assertEquals(departure.plusDays(7), moved.condition.departureDate)
        assertEquals(returning.plusDays(7), moved.condition.returnDate)
        assertEquals(SavedTrip.idFor(moved.condition), moved.id)
        assertNotEquals(trip.id, moved.id)
        assertEquals("도쿄 5일", moved.title)
        assertEquals("cover", moved.coverPlaceId)
        assertEquals(trip.savedAt, moved.savedAt)
    }

    @Test
    fun `일수가 바뀌면 편집을 비우고 자동 이름을 고친다`() {
        val moved = trip.withDates(departure, returning.plusDays(2), autoTitle)

        assertTrue(moved.edits.isEmpty)
        assertEquals(7, moved.dayCount)
        assertEquals("도쿄 7일", moved.title)
    }

    @Test
    fun `사용자가 바꾼 이름은 일수가 바뀌어도 둔다`() {
        val renamed = trip.copy(title = "신혼여행")
        val moved = renamed.withDates(departure, returning.plusDays(2), autoTitle)

        assertEquals("신혼여행", moved.title)
    }

    @Test
    fun `출발일 전날까지만 기간을 바꿀 수 있다`() {
        assertTrue(trip.canChangeDates(departure.minusDays(1)))
        assertFalse(trip.canChangeDates(departure))
        assertFalse(trip.canChangeDates(returning.plusDays(1)))
    }
}
