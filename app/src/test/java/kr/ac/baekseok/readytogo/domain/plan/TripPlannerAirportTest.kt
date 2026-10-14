package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Flight
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.AirportRepository
import kr.ac.baekseok.readytogo.data.repository.FlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockPlaceRepository
import kr.ac.baekseok.readytogo.domain.recommend.detourKm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 도착일은 공항에서 시작하고 출국일은 공항에서 끝나는지, 공항을 모르면 숙소로 돌아가는지.
 */
class TripPlannerAirportTest {

    private val condition = TripCondition(
        origin = kr.ac.baekseok.readytogo.TestDestinations.INCHEON,
        destination = kr.ac.baekseok.readytogo.TestDestinations.TOKYO,
        departureDate = LocalDate.of(2026, 10, 1),
        returnDate = LocalDate.of(2026, 10, 5),
        budgetWon = 1_500_000,
    )

    private val tokyoAirports = object : AirportRepository {
        private val table = mapOf("NRT" to LatLng(35.7686, 140.3887), "HND" to LatLng(35.5497, 139.787))
        override suspend fun locationOf(code: String) = table[code]
    }

    private val noAirports = object : AirportRepository {
        override suspend fun locationOf(code: String): LatLng? = null
    }

    /** 실제 검색이 실패해 예시 데이터로 대체된 상황. */
    private val sampleFlights = object : FlightRepository {
        override suspend fun search(condition: TripCondition): Result<List<Flight>> =
            MockFlightRepository().search(condition).map { list -> list.map { it.copy(isSample = true) } }
    }

    private fun planner(
        airports: AirportRepository?,
        flights: FlightRepository = MockFlightRepository(),
    ) = TripPlanner(
        flightRepository = flights,
        lodgingRepository = MockLodgingRepository(),
        placeRepository = MockPlaceRepository(),
        itineraryRepository = MockItineraryRepository(),
        allowRestaurantReuse = true,
        airportRepository = airports,
    )

    @Test
    fun `최저가 항공편의 도착 공항이 계획에 담긴다`() = runBlocking {
        val plan = planner(tokyoAirports).createPlan(condition).getOrThrow()

        assertNotNull(plan.airport)
        assertEquals(plan.flights.first().arrivalAirport, plan.airport!!.code)
    }

    @Test
    fun `도착일 아침은 공항에서 첫 관광지로 가는 길 기준으로 고른다`() = runBlocking {
        val plan = planner(tokyoAirports).createPlan(condition).getOrThrow()
        val airport = plan.airport!!.location
        val hotel = plan.lodging!!.qualityFirst.item.location
        val day1 = plan.dailyPlans.first()
        val breakfast = day1.meals.getValue(MealTime.BREAKFAST).qualityFirst

        val (from, to) = mealNeighbours(MealTime.BREAKFAST, day1.attractions, emptyMap(), airport, hotel)
        assertEquals(airport, from)
        assertEquals(detourKm(from, breakfast.item.location, to), breakfast.detourKm!!, 0.0001)
    }

    @Test
    fun `출국일 저녁은 마지막 관광지에서 공항으로 가는 길 기준으로 고른다`() = runBlocking {
        val plan = planner(tokyoAirports).createPlan(condition).getOrThrow()
        val airport = plan.airport!!.location
        val last = plan.dailyPlans.last()
        val dinner = last.meals.getValue(MealTime.DINNER).qualityFirst

        val from = last.attractions.last().location
        assertEquals(detourKm(from, dinner.item.location, airport), dinner.detourKm!!, 0.0001)
    }

    @Test
    fun `예시 항공편이면 공항을 쓰지 않는다`() = runBlocking {
        val plan = planner(tokyoAirports, sampleFlights).createPlan(condition).getOrThrow()
        assertNull(plan.airport)
    }

    @Test
    fun `공항 좌표를 모르면 공항을 쓰지 않는다`() = runBlocking {
        assertNull(planner(noAirports).createPlan(condition).getOrThrow().airport)
        assertNull(planner(null).createPlan(condition).getOrThrow().airport)
    }
}
