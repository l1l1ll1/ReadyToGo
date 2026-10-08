package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.data.repository.FlightRepository
import kr.ac.baekseok.readytogo.data.repository.ItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.LodgingRepository
import kr.ac.baekseok.readytogo.data.repository.PlaceRepository
import kr.ac.baekseok.readytogo.domain.budget.allocateBudget
import kr.ac.baekseok.readytogo.domain.budget.applyPriceIndex
import kr.ac.baekseok.readytogo.domain.price.CityPriceIndex
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kr.ac.baekseok.readytogo.domain.recommend.medianCenter
import kr.ac.baekseok.readytogo.domain.recommend.recommendPair
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

/**
 * 네 개의 Repository에서 받은 재료로 완성된 TripPlan 하나를 만든다.
 *
 * 순서가 중요하다 — 항공권을 **먼저** 찾아야 실제 최저가를 알 수 있고, 그래야 남은 예산으로
 * 숙소·식당 상한을 정할 수 있다. 비율로 항공료를 추정하지 않는 이유가 이것이다.
 *
 * Repository 인터페이스에만 의존하므로 Mock인지 실제 API인지 알지 못한다(CLAUDE.md).
 */
class TripPlanner(
    private val flightRepository: FlightRepository,
    private val lodgingRepository: LodgingRepository,
    private val placeRepository: PlaceRepository,
    private val itineraryRepository: ItineraryRepository,
) {

    suspend fun createPlan(condition: TripCondition): Result<TripPlan> = runCatching {
        // 검색 넷은 서로를 기다릴 이유가 없다. 순차로 보내면 네 번의 네트워크 왕복이
        // 그대로 더해져 로딩이 20초 가까이 걸린다. 동시에 보내 한 번 기다린다.
        val (flights, attractions, lodgings) = coroutineScope {
            val flightsAsync = async { flightRepository.search(condition) }
            val attractionsAsync = async { placeRepository.searchAttractions(condition) }
            val lodgingsAsync = async { lodgingRepository.search(condition) }

            SearchResults(
                flights = flightsAsync.await().getOrThrow(),
                attractions = attractionsAsync.await().getOrThrow(),
                lodgings = lodgingsAsync.await().getOrThrow(),
            )
        }

        // 1. 항공권 최저가가 예산 배분의 출발점이다.
        val budget = allocateBudget(condition, flights.firstOrNull()?.priceWon)

        // 2. 일자별 배치는 관광지가 나온 뒤에야 할 수 있어 여기서 따로 부른다.
        val dailyAttractions = itineraryRepository
            .planDailyAttractions(condition, attractions)
            .getOrThrow()

        val plannedAttractions = dailyAttractions.flatten()

        // 3. 식당은 **날짜별로 따로** 찾는다.
        //    도시 중심에서 한 번만 찾으면 여행 내내 같은 후보를 나눠 써서 긴 여행에서 반복되고,
        //    그날 동선과 먼 식당도 섞인다. 일정 중심이 가까운 날끼리는 검색을 공유해 호출을 아낀다.
        val restaurantsByDay = searchRestaurantsPerDay(condition, dailyAttractions, plannedAttractions)

        // 4. 숙소. Places가 요금을 안 줘서 등급 추정인데, 등급을 고정 금액표로 환산하면
        //    물가가 다른 나라에서 크게 빗나간다. 같은 도시 식당의 실제 가격에서 배수를 구해 보정한다.
        //    날짜별 검색 결과를 합쳐 쓰되, 같은 식당이 여러 날에 중복으로 들어가지 않게 한 번만 센다.
        val allRestaurants = restaurantsByDay.flatten().distinctBy { it.id }
        val priceIndex = CityPriceIndex.of(allRestaurants.map { it.priceWon })
        val adjustedLodgings = lodgings.map { it.applyPriceIndex(priceIndex) }

        //    숙소는 여행 전체에 하나. 동선 기준은 **일정에 실제 배치된 관광지**다.
        //    검색 결과 전체를 쓰면 가지도 않을 곳까지 중심 계산에 들어간다.
        val lodging = recommendPair(
            candidates = adjustedLodgings,
            anchors = plannedAttractions.map { it.location },
            maxPriceWon = budget.lodgingPerNightWon,
        )

        // 5. 날짜별 계획 조립.
        val startDate = condition.departureDate ?: LocalDate.now()

        // 한 번 추천한 식당은 여행이 끝날 때까지 다시 쓰지 않는다.
        // 날짜 루프 바깥에 두어야 날짜를 넘어서도 중복이 막힌다.
        val usedRestaurantIds = mutableSetOf<String>()
        val dailyPlans = dailyAttractions.mapIndexed { index, dayAttractions ->
            DayPlan(
                date = startDate.plusDays(index.toLong()),
                dayNumber = index + 1,
                attractions = dayAttractions,
                meals = pickMeals(
                    restaurants = restaurantsByDay[index].orEmpty(),
                    dayAttractions = dayAttractions,
                    allAttractions = plannedAttractions,
                    maxPriceWon = budget.mealWon,
                    usedRestaurantIds = usedRestaurantIds,
                ),
            )
        }

        logSelectionSummary(dailyPlans)

        TripPlan(
            condition = condition,
            budget = budget,
            flights = flights,
            lodging = lodging,
            dailyPlans = dailyPlans,
        )
    }

    /** 동시에 보낸 검색 결과를 한 번에 받기 위한 묶음. */
    private data class SearchResults(
        val flights: List<kr.ac.baekseok.readytogo.data.model.Flight>,
        val attractions: List<Attraction>,
        val lodgings: List<kr.ac.baekseok.readytogo.data.model.Lodging>,
    )

    /**
     * 날짜별로 식당을 찾는다. 결과의 index가 곧 일차(0부터)다.
     *
     * 일정 중심이 [MERGE_DISTANCE_KM] 이내인 날끼리는 **검색을 한 번만** 하고 나눠 쓴다.
     * 같은 동네에 이틀 머무는 일정에서 호출을 두 배로 쓸 이유가 없다.
     * 대신 중복 배제는 날짜를 넘어 유지되므로 같은 식당이 두 번 나오지는 않는다.
     */
    private suspend fun searchRestaurantsPerDay(
        condition: TripCondition,
        dailyAttractions: List<List<Attraction>>,
        plannedAttractions: List<Attraction>,
    ): List<List<Restaurant>> {
        // 일정이 빈 날은 여행 전체 중심을, 그마저 없으면 도시 중심을 쓴다.
        val fallbackCenter = medianCenter(plannedAttractions.map { it.location })
            ?: condition.destination?.location
            ?: return List(dailyAttractions.size) { emptyList() }

        val dayCenters = dailyAttractions.map { day ->
            medianCenter(day.map { it.location }) ?: fallbackCenter
        }

        // 가까운 중심끼리 묶는다. groupOfDay[i]는 i일차가 속한 그룹 번호.
        val groupCenters = mutableListOf<LatLng>()
        val groupOfDay = dayCenters.map { center ->
            val existing = groupCenters.indexOfFirst {
                distanceKm(center, it) <= MERGE_DISTANCE_KM
            }
            if (existing >= 0) {
                existing
            } else {
                groupCenters += center
                groupCenters.lastIndex
            }
        }

        android.util.Log.d(
            "TripPlan",
            "식당 검색: ${dailyAttractions.size}일 → ${groupCenters.size}그룹 (그룹당 2회 호출)",
        )

        // 그룹이 며칠을 맡는지 세어 둔다. 필요한 후보 수가 여기서 나온다.
        val daysPerGroup = IntArray(groupCenters.size)
        groupOfDay.forEach { daysPerGroup[it]++ }

        // 그룹마다 한 번씩, 동시에 보낸다.
        val byGroup = coroutineScope {
            groupCenters.mapIndexed { index, center ->
                async { searchGroup(condition, center, daysPerGroup[index]) }
            }.map { it.await() }
        }

        return groupOfDay.map { byGroup[it] }
    }

    /**
     * 하루치 아침·점심·저녁을 고른다.
     *
     * @param usedRestaurantIds 여행 중 이미 추천한 식당. 이 함수가 직접 채우며, 날짜를 넘어서도
     *        유지되므로 같은 집이 두 번 나오지 않는다. 같은 여행에서 같은 식당을 또 가지는 않는다는 뜻이다.
     */
    private fun pickMeals(
        restaurants: List<Restaurant>,
        dayAttractions: List<Attraction>,
        allAttractions: List<Attraction>,
        maxPriceWon: Long?,
        usedRestaurantIds: MutableSet<String>,
    ): Map<MealTime, RecommendationPair<Restaurant>> {
        // 일정이 비는 날(도착 당일 등)은 그 도시 전체 일정을 기준으로 삼는다.
        // 기준점이 아예 없으면 동선 점수가 모두 0이 되어 평점만으로 순위가 정해져버린다.
        val anchors = dayAttractions.ifEmpty { allAttractions }.map { it.location }

        return MealTime.entries.mapNotNull { meal ->
            // 끼니에 어울리는 곳만 후보로 둔다 — 아침에 오마카세, 저녁에 카페가 뜨는 것을 막는다.
            val matching = restaurants.filter { meal in it.suitableMeals }
            // 그 끼니에 맞는 곳이 아예 없으면 끼니를 비우느니 전체 후보에서 고른다.
            val forThisMeal = matching.ifEmpty { restaurants }

            // 후보가 동나면 그 끼니는 비워 둔다.
            // 예전에는 중복을 허용해 채웠지만, 그러면 뒷날이 전부 같은 식당으로 도배됐다.
            // 날짜별 검색으로 후보를 새로 확보하므로 이 상황 자체가 드물어졌다.
            val pool = forThisMeal.filterNot { it.id in usedRestaurantIds }

            val pair = recommendPair(pool, anchors, maxPriceWon) ?: return@mapNotNull null
            usedRestaurantIds += pair.qualityFirst.item.id
            pair.withinBudget?.let { usedRestaurantIds += it.item.id }
            meal to pair
        }.toMap()
    }

    /**
     * 한 그룹이 맡은 날들에 쓸 식당을 찾는다.
     *
     * 일정이 한 동네에 몰리면 그룹이 적게 나뉘고, 그만큼 한 그룹이 여러 날을 맡아 후보가 모자란다.
     * 그러면 끼니가 빈 채로 남는다. 모자란 그룹만 반경을 넓혀 한 번 더 찾아 채운다.
     * 처음부터 넓게 찾지 않는 이유는 그러면 그날 동선과 먼 식당이 후보에 섞이기 때문이다.
     */
    private suspend fun searchGroup(
        condition: TripCondition,
        center: LatLng,
        dayCount: Int,
    ): List<Restaurant> {
        val nearby = placeRepository.searchRestaurants(condition, center).getOrThrow()
        val shortage = shortageOf(nearby, dayCount) ?: return nearby

        android.util.Log.d("TripPlan", "후보 부족($shortage) — 반경 ${WIDER_SEARCH_SCALE}배로 재검색")
        val wider = placeRepository
            .searchRestaurants(condition, center, WIDER_SEARCH_SCALE)
            .getOrThrow()

        return (nearby + wider).distinctBy { it.id }
    }

    /**
     * 후보가 모자란지 **끼니별로** 본다. 모자라면 사람이 읽을 설명을, 충분하면 null을 돌려준다.
     *
     * 그룹 전체 개수만 세면 안 된다. 후보는 [Restaurant.suitableMeals]에 따라 끼니별로 갈라져
     * 쓰이기 때문이다 — 카페가 20곳 잡혀 총 개수는 넉넉해도 저녁에 쓸 수 있는 곳이 2곳이면
     * 저녁 칸이 빈다. 실제로 도쿄 5일 일정에서 3·4일차 점심·저녁이 이렇게 비었다.
     *
     * 전체 개수도 함께 보는 이유: 한 식당이 점심과 저녁 후보에 동시에 들어가지만 중복 배제 때문에
     * 한 번만 쓸 수 있어, 끼니별로는 충분해 보여도 합치면 모자랄 수 있다.
     */
    private fun shortageOf(candidates: List<Restaurant>, dayCount: Int): String? {
        val neededPerMeal = dayCount * CHOICES_PER_MEAL
        val reasons = MealTime.entries.mapNotNull { meal ->
            val count = candidates.count { meal in it.suitableMeals }
            if (count < neededPerMeal) "${meal.label} $count/$neededPerMeal" else null
        }.toMutableList()

        val neededTotal = neededPerMeal * MealTime.entries.size
        if (candidates.size < neededTotal) {
            reasons += "전체 ${candidates.size}/$neededTotal"
        }

        return reasons.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    /**
     * 확인용 — 여행 전체에서 같은 식당이 두 번 쓰이지 않았는지, 빈 끼니가 없는지 본다.
     * 화면을 스크롤하며 세는 것으로는 같은 항목을 여러 번 집계하게 되어 판단할 수 없다.
     */
    private fun logSelectionSummary(dailyPlans: List<DayPlan>) {
        val picked = dailyPlans.flatMap { day ->
            day.meals.values.flatMap { listOfNotNull(it.qualityFirst.item, it.withinBudget?.item) }
        }
        val duplicates = picked.groupBy { it.id }.filter { it.value.size > 1 }
        val expectedMeals = dailyPlans.size * MealTime.entries.size
        val filledMeals = dailyPlans.sumOf { it.meals.size }

        android.util.Log.d(
            "TripPlan",
            "끼니 $filledMeals/$expectedMeals · 식당 ${picked.size}칸 / " +
                "서로 다른 ${picked.distinctBy { it.id }.size}곳 / 중복 ${duplicates.size}종",
        )
        duplicates.forEach { (_, same) ->
            android.util.Log.d("TripPlan", "중복: ${same.first().name} ${same.size}회")
        }

        // 숫자만으로는 "식당이 아닌 곳"이 섞인 것을 알 수 없다. 카테고리를 같이 찍어 눈으로 본다.
        dailyPlans.forEach { day ->
            day.meals.forEach { (meal, pair) ->
                val picks = listOfNotNull(pair.qualityFirst.item, pair.withinBudget?.item)
                    .joinToString(" | ") { "${it.name}(${it.category})" }
                android.util.Log.d("TripPlan", "${day.dayNumber}일차 ${meal.label}: $picks")
            }
        }

        val missing = dailyPlans.flatMap { day ->
            MealTime.entries.filterNot { it in day.meals.keys }
                .map { "${day.dayNumber}일차 ${it.label}" }
        }
        if (missing.isNotEmpty()) {
            android.util.Log.d("TripPlan", "빈 끼니: ${missing.joinToString(", ")}")
        }
    }

    private companion object {
        /**
         * 이 거리 안의 날끼리는 식당 검색을 공유한다.
         * 같은 동네에 머무는 날에 검색을 따로 보내면 호출만 두 배로 쓰고 결과는 거의 같다.
         */
        const val MERGE_DISTANCE_KM = 3.0

        /** 한 끼에 추천·대안 두 곳이 필요하다. */
        const val CHOICES_PER_MEAL = 2

        /** 후보가 모자란 그룹만 이 배수로 반경을 넓혀 다시 찾는다. */
        const val WIDER_SEARCH_SCALE = 2.0
    }
}