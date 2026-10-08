package kr.ac.baekseok.readytogo.data.repository.mock

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MockItineraryRepositoryTest {

    private val repository = MockItineraryRepository()
    private val places = MockPlaceRepository()

    private fun condition(days: Long, purposes: Set<TravelPurpose> = emptySet()): TripCondition {
        val start = LocalDate.of(2026, 10, 1)
        return TripCondition(
            destination = kr.ac.baekseok.readytogo.TestDestinations.TOKYO,
            departureDate = start,
            returnDate = start.plusDays(days - 1),
            budgetWon = 1_500_000,
            purposes = purposes,
        )
    }

    private suspend fun plan(days: Long, purposes: Set<TravelPurpose> = emptySet()): List<List<Attraction>> {
        val c = condition(days, purposes)
        val candidates = places.searchAttractions(c).getOrThrow()
        return repository.planDailyAttractions(c, candidates).getOrThrow()
    }

    @Test
    fun `날짜 수만큼 하루치가 만들어진다`() = runBlocking {
        assertEquals(5, plan(days = 5).size)
        assertEquals(10, plan(days = 10).size)
    }

    @Test
    fun `장소보다 날짜가 많아도 뒷날이 통째로 비지 않는다`() = runBlocking {
        // 관광지 15곳 / 10일. 앞에서부터 채우면 뒤 5일이 빈다.
        val days = plan(days = 10)

        days.forEachIndexed { index, day ->
            assertTrue("${index + 1}일차가 비었다", day.isNotEmpty())
        }
        assertEquals(15, days.sumOf { it.size })
    }

    @Test
    fun `여행 목적을 하나만 골라 후보가 적어도 고르게 나뉜다`() = runBlocking {
        // 휴양 후보는 4곳뿐이다. 5일이면 최소 4일은 채워져야 한다.
        val days = plan(days = 5, purposes = setOf(TravelPurpose.REST))

        val filledDays = days.count { it.isNotEmpty() }
        assertEquals(4, days.sumOf { it.size })
        assertTrue("채워진 날이 $filledDays 일뿐이다", filledDays >= 4)
    }

    @Test
    fun `도착일과 출국일은 한 곳만 배정한다`() = runBlocking {
        val days = plan(days = 5)

        assertEquals(1, days.first().size)
        assertEquals(1, days.last().size)
    }

    @Test
    fun `하루 안의 장소들은 서로 가깝다`() = runBlocking {
        val days = plan(days = 5)

        days.forEach { day ->
            day.zipWithNext { a, b ->
                val km = kr.ac.baekseok.readytogo.domain.recommend.distanceKm(a.location, b.location)
                assertTrue("${a.name} → ${b.name} 가 ${km}km", km < 12.0)
            }
        }
    }

    @Test
    fun `후보가 없어도 날짜 수는 유지된다`() = runBlocking {
        val result = repository.planDailyAttractions(condition(days = 3), emptyList()).getOrThrow()

        assertEquals(3, result.size)
        assertTrue(result.all { it.isEmpty() })
    }

    @Test
    fun `장소가 날짜보다 적으면 있는 만큼만 앞에서부터 채운다`() = runBlocking {
        val two = listOf(
            Attraction("x1", "장소1", 4.0, LatLng(35.6896, 139.7006), 60, "주소"),
            Attraction("x2", "장소2", 4.0, LatLng(35.6900, 139.7010), 60, "주소"),
        )

        val result = repository.planDailyAttractions(condition(days = 5), two).getOrThrow()

        assertEquals(5, result.size)
        assertEquals(2, result.sumOf { it.size })
    }
}