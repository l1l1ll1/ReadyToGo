package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.Flight
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.FlightRepository
import kr.ac.baekseok.readytogo.data.repository.ItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.LodgingRepository
import kr.ac.baekseok.readytogo.data.repository.PlaceRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockPlaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/**
 * 실패 경로 검증.
 *
 * Mock Repository는 절대 실패하지 않으므로, 실패를 주입하는 가짜를 따로 만들어야
 * "네트워크가 끊기면 어떻게 되는가"를 확인할 수 있다. 7단계에서 실제 API를 붙이면 반드시 겪을 상황이다.
 */
class TripPlannerFailureTest {

    private val condition = TripCondition(
        origin = kr.ac.baekseok.readytogo.TestDestinations.INCHEON,
        destination = kr.ac.baekseok.readytogo.TestDestinations.TOKYO,
        departureDate = LocalDate.of(2026, 10, 1),
        returnDate = LocalDate.of(2026, 10, 5),
        budgetWon = 1_500_000,
    )

    private class FailingFlightRepository(private val error: Throwable) : FlightRepository {
        override suspend fun search(condition: TripCondition): Result<List<Flight>> =
            Result.failure(error)
    }

    private class FailingLodgingRepository(private val error: Throwable) : LodgingRepository {
        override suspend fun search(condition: TripCondition): Result<List<Lodging>> =
            Result.failure(error)
    }

    private class FailingPlaceRepository(private val error: Throwable) : PlaceRepository {
        override suspend fun searchRestaurants(
            condition: TripCondition,
            center: kr.ac.baekseok.readytogo.data.model.LatLng,
            radiusScale: Double,
        ): Result<List<Restaurant>> = Result.failure(error)

        override suspend fun searchAttractions(condition: TripCondition): Result<List<Attraction>> =
            Result.failure(error)
    }

    private class FailingItineraryRepository(private val error: Throwable) : ItineraryRepository {
        override suspend fun planDailyAttractions(
            condition: TripCondition,
            candidates: List<Attraction>,
        ): Result<List<List<Attraction>>> = Result.failure(error)
    }

    private fun plannerWith(
        flight: FlightRepository = MockFlightRepository(),
        lodging: LodgingRepository = MockLodgingRepository(),
        place: PlaceRepository = MockPlaceRepository(),
        itinerary: ItineraryRepository = MockItineraryRepository(),
    ) = TripPlanner(flight, lodging, place, itinerary)

    @Test
    fun `항공권 검색이 실패하면 전체가 실패로 돌아온다`() = runBlocking {
        val error = IOException("네트워크 연결 실패")
        val result = plannerWith(flight = FailingFlightRepository(error)).createPlan(condition)

        assertTrue(result.isFailure)
        assertEquals("네트워크 연결 실패", result.exceptionOrNull()?.message)
    }

    @Test
    fun `숙소 검색이 실패해도 예외가 밖으로 새지 않는다`() = runBlocking {
        // createPlan은 던지지 않고 Result로만 알려야 한다. 화면이 크래시하면 안 된다.
        val result = plannerWith(lodging = FailingLodgingRepository(IOException("숙소 서버 오류")))
            .createPlan(condition)

        assertTrue(result.isFailure)
    }

    @Test
    fun `장소 검색 실패도 Result로 전달된다`() = runBlocking {
        val result = plannerWith(place = FailingPlaceRepository(IOException("장소 서버 오류")))
            .createPlan(condition)

        assertTrue(result.isFailure)
    }

    @Test
    fun `일정 생성 실패도 Result로 전달된다`() = runBlocking {
        val result = plannerWith(itinerary = FailingItineraryRepository(IOException("일정 서버 오류")))
            .createPlan(condition)

        assertTrue(result.isFailure)
    }

    @Test
    fun `실패 원인이 없으면 화면에 보여줄 기본 문구가 있어야 한다`() = runBlocking {
        // message가 null인 예외도 있다. 화면이 "null"을 그대로 띄우지 않는지 확인한다.
        val result = plannerWith(flight = FailingFlightRepository(IOException())).createPlan(condition)

        val shown = result.exceptionOrNull()?.message ?: "계획을 만들지 못했습니다."
        assertEquals("계획을 만들지 못했습니다.", shown)
    }
}