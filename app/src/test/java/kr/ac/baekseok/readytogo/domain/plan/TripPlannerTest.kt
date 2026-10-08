package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.mock.MockFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockPlaceRepository
import kr.ac.baekseok.readytogo.domain.recommend.QUALITY_PRICE_MULTIPLIER
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Mock 네 개를 실제로 물려서 TripPlan이 끝까지 만들어지는지 확인한다.
 * 4단계 결과 화면을 만들기 전에 데이터가 제대로 채워지는지 여기서 먼저 잡는다.
 */
class TripPlannerTest {

    private val planner = TripPlanner(
        flightRepository = MockFlightRepository(),
        lodgingRepository = MockLodgingRepository(),
        placeRepository = MockPlaceRepository(),
        itineraryRepository = MockItineraryRepository(),
    )

    /** 4박 5일 도쿄, 예산 150만원. */
    private val condition = TripCondition(
        origin = kr.ac.baekseok.readytogo.TestDestinations.INCHEON,
        destination = kr.ac.baekseok.readytogo.TestDestinations.TOKYO,
        departureDate = LocalDate.of(2026, 10, 1),
        returnDate = LocalDate.of(2026, 10, 5),
        budgetWon = 1_500_000,
    )

    @Test
    fun `여행 일수만큼 일자별 계획이 만들어진다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        assertEquals(5, plan.dailyPlans.size)
        assertEquals(LocalDate.of(2026, 10, 1), plan.dailyPlans.first().date)
        assertEquals(LocalDate.of(2026, 10, 5), plan.dailyPlans.last().date)
        assertEquals(1, plan.dailyPlans.first().dayNumber)
    }

    @Test
    fun `항공권은 최저가 오름차순이고 그 최저가로 예산을 나눈다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        val prices = plan.flights.map { it.priceWon }
        assertEquals("오름차순이어야 한다", prices.sorted(), prices)

        val cheapest = prices.first()
        assertEquals(cheapest, plan.budget.flightWon)
        assertEquals(1_500_000L - cheapest, plan.budget.remainingWon)
    }

    @Test
    fun `모든 날에 아침 점심 저녁이 다 채워진다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        plan.dailyPlans.forEach { day ->
            assertEquals(
                "${day.dayNumber}일차 끼니가 비었다",
                setOf(MealTime.BREAKFAST, MealTime.LUNCH, MealTime.DINNER),
                day.meals.keys,
            )
        }
    }

    @Test
    fun `여행 전체에서 같은 식당이 두 번 나오지 않는다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        val all = plan.dailyPlans.flatMap { day ->
            day.meals.values.flatMap { listOf(it.qualityFirst.item, it.withinBudget!!.item) }
        }
        val duplicated = all.groupBy { it.id }.filter { it.value.size > 1 }.keys

        assertEquals("중복 추천된 식당: $duplicated", emptySet<String>(), duplicated)
        // 5일 × 3끼 × 2선택지 = 30곳이 전부 서로 달라야 한다.
        assertEquals(30, all.size)
    }

    @Test
    fun `추천된 식당은 그 끼니에 어울리는 곳이다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        plan.dailyPlans.forEach { day ->
            day.meals.forEach { (meal, pair) ->
                listOf(pair.qualityFirst.item, pair.withinBudget!!.item).forEach { restaurant ->
                    assertTrue(
                        "${day.dayNumber}일차 ${meal.label}에 ${restaurant.category}인 " +
                            "${restaurant.name}이(가) 추천됐다",
                        meal in restaurant.suitableMeals,
                    )
                }
            }
        }
    }

    @Test
    fun `아침에는 술집이나 고깃집이 추천되지 않는다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        val dinnerOnly = setOf("이자카야", "야키니쿠", "야키토리")
        plan.dailyPlans.forEach { day ->
            val breakfast = day.meals[MealTime.BREAKFAST] ?: return@forEach
            listOfNotNull(breakfast.qualityFirst.item, breakfast.withinBudget?.item).forEach {
                assertTrue("${day.dayNumber}일차 아침에 ${it.category}", it.category !in dinnerOnly)
            }
        }
    }

    @Test
    fun `품질 우선 추천도 예산 상한의 2배 안에 들어온다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        val mealCap = (plan.budget.mealWon!! * QUALITY_PRICE_MULTIPLIER).toLong()
        plan.dailyPlans.forEach { day ->
            day.meals.forEach { (meal, pair) ->
                val item = pair.qualityFirst.item
                assertTrue(
                    "${day.dayNumber}일차 ${meal.label}: ${item.name} ${item.priceWon}원 > 상한 ${mealCap}원",
                    item.priceWon <= mealCap,
                )
            }
        }

        val lodgingCap = (plan.budget.lodgingPerNightWon!! * QUALITY_PRICE_MULTIPLIER).toLong()
        assertTrue(plan.lodging!!.qualityFirst.item.priceWon <= lodgingCap)
    }

    @Test
    fun `숙소는 두 가지 선택지가 모두 채워진다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        val lodging = plan.lodging
        assertNotNull(lodging)
        assertTrue(lodging!!.qualityFirst.score > 0.0)
        assertTrue(lodging.withinBudget!!.score > 0.0)
    }

    @Test
    fun `하루 일정은 지리적으로 뭉쳐 있다`() = runBlocking {
        val plan = planner.createPlan(condition).getOrThrow()

        // 최근접 이웃으로 묶었으므로, 같은 날 장소끼리는 서로 멀지 않아야 한다.
        plan.dailyPlans.forEach { day ->
            day.attractions.zipWithNext { a, b ->
                val km = distanceKm(a.location, b.location)
                assertTrue("${day.dayNumber}일차: ${a.name} → ${b.name} 가 ${km}km 떨어져 있다", km < 12.0)
            }
        }
    }

    @Test
    fun `예산을 입력하지 않아도 계획이 만들어진다`() = runBlocking {
        val plan = planner.createPlan(condition.copy(budgetWon = null)).getOrThrow()

        assertEquals(5, plan.dailyPlans.size)
        assertNotNull(plan.lodging)
        // 상한이 없으므로 예산 초과 표시가 붙으면 안 된다.
        assertTrue(plan.dailyPlans.all { day -> day.meals.values.none { it.qualityFirst.overBudget } })
    }
}