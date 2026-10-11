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
    /**
     * 후보가 동났을 때 같은 식당을 다시 쓸지.
     *
     * **Mock에서만 켠다.** Mock 식당은 15곳뿐이라 5일 일정(30칸)에서 반드시 소진되는데,
     * 테스트 중 화면이 비어 보이면 레이아웃을 확인할 수 없다.
     * 실제 API 경로에서는 끈다 — 같은 여행에서 같은 집을 두 번 가라고 추천할 이유가 없다.
     */
    private val allowRestaurantReuse: Boolean = false,
) {

    /** 진단 로그에서 "예산 내 후보 수"를 세는 데만 쓴다. 추천 로직은 이 값을 보지 않는다. */
    private var lastMealBudgetWon: Long? = null

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

        // 진단 로그가 끼니 예산을 알아야 "예산 내 후보가 몇 곳"을 셀 수 있다.
        // 검색 함수까지 인자로 들고 다니면 서명이 지저분해져서 필드로 둔다.
        lastMealBudgetWon = budget.mealWon

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

        // 그날 중심. 4단계(반경 넓혀 재검색)에서 어디를 중심으로 다시 찾을지 알아야 한다.
        // null이면 검색할 위치가 없다는 뜻이고, 그 경우 4단계는 그냥 건너뛴다.
        val dayCenters = dayCentersOf(condition, dailyAttractions, plannedAttractions).orEmpty()

        val dailyPlans = dailyAttractions.mapIndexed { index, dayAttractions ->
            DayPlan(
                date = startDate.plusDays(index.toLong()),
                dayNumber = index + 1,
                attractions = dayAttractions,
                meals = pickMeals(
                    dayNumber = index + 1,
                    restaurants = restaurantsByDay[index].orEmpty(),
                    dayAttractions = dayAttractions,
                    allAttractions = plannedAttractions,
                    maxPriceWon = budget.mealWon,
                    usedRestaurantIds = usedRestaurantIds,
                    widenSearch = {
                        val center = dayCenters.getOrNull(index)
                        if (center == null) {
                            emptyList()
                        } else {
                            placeRepository
                                .searchRestaurants(condition, center, WIDEST_SEARCH_SCALE)
                                .getOrDefault(emptyList())
                        }
                    },
                ),
            )
        }

        // 결과 한 줄 요약. 끼니가 비거나 예산 초과가 쏟아지는 것은 추천 품질이 무너진 신호라
        // 매 실행에서 보고 있어야 한다. 항목 목록까지 찍던 예전 진단 로그와 달리 한 줄이다.
        logPlanSummary(dailyPlans)

        TripPlan(
            condition = condition,
            budget = budget,
            flights = flights,
            lodging = lodging,
            dailyPlans = dailyPlans,
        )
    }

    /** 끼니를 몇 칸 채웠고 그중 몇이 예산을 넘었는지. */
    private fun logPlanSummary(dailyPlans: List<DayPlan>) {
        val expected = dailyPlans.size * MealTime.entries.size
        val filled = dailyPlans.sumOf { it.meals.size }
        val picks = dailyPlans.flatMap { day ->
            day.meals.values.flatMap { listOfNotNull(it.qualityFirst, it.withinBudget) }
        }
        val overBudget = picks.count { it.overBudget }
        val noAlternative = dailyPlans.sumOf { day -> day.meals.values.count { it.withinBudget == null } }
        val distinct = picks.map { it.item.id }.distinct().size

        android.util.Log.d(
            "TripPlan",
            "결과: 끼니 $filled/$expected · 식당 ${picks.size}칸(서로 다른 ${distinct}곳) · " +
                "예산 초과 $overBudget · 대안 없음 $noAlternative",
        )
    }

    /** 동시에 보낸 검색 결과를 한 번에 받기 위한 묶음. */
    private data class SearchResults(
        val flights: List<kr.ac.baekseok.readytogo.data.model.Flight>,
        val attractions: List<Attraction>,
        val lodgings: List<kr.ac.baekseok.readytogo.data.model.Lodging>,
    )

    /**
     * 날짜별 일정 중심. 식당 검색과 재검색이 같은 기준을 써야 한다.
     *
     * 일정이 빈 날은 여행 전체 중심을, 그마저 없으면 도시 중심을 쓴다.
     * 셋 다 없으면 검색할 위치가 없다는 뜻이라 null이다.
     */
    private fun dayCentersOf(
        condition: TripCondition,
        dailyAttractions: List<List<Attraction>>,
        plannedAttractions: List<Attraction>,
    ): List<LatLng>? {
        val fallbackCenter = medianCenter(plannedAttractions.map { it.location })
            ?: condition.destination?.location
            ?: return null

        return dailyAttractions.map { day ->
            medianCenter(day.map { it.location }) ?: fallbackCenter
        }
    }

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
        val dayCenters = dayCentersOf(condition, dailyAttractions, plannedAttractions)
            ?: return List(dailyAttractions.size) { emptyList() }

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

        // 그룹이 며칠을 맡는지 세어 둔다. 필요한 후보 수가 여기서 나온다.
        val daysPerGroup = IntArray(groupCenters.size)
        groupOfDay.forEach { daysPerGroup[it]++ }

        // 진단용 — 어느 그룹이 몇 일을 맡았는지. 한 그룹이 여러 날을 맡으면 후보가 모자란다.
        android.util.Log.d(
            "TripPlan",
            "식당 검색: ${dailyAttractions.size}일 → ${groupCenters.size}그룹 " +
                "(그룹별 담당 일수 ${daysPerGroup.joinToString(",")})",
        )
        groupOfDay.forEachIndexed { dayIndex, group ->
            android.util.Log.d("TripPlan", "  ${dayIndex + 1}일차 → 그룹 $group")
        }

        // 그룹마다 한 번씩, 동시에 보낸다.
        val byGroup = coroutineScope {
            groupCenters.mapIndexed { index, center ->
                async { searchGroup(condition, center, daysPerGroup[index]) }
            }.map { it.await() }
        }

        // 넓혀 찾아도 모자란 그룹은 **날짜별로 분리해 다시 찾는다.**
        //
        // 3km 안에 있다는 이유로 묶었지만 그 동네에 식당이 적으면 묶은 날들이 후보를 나눠 쓰다
        // 뒷날이 빈다. 그럴 때는 호출을 아끼는 것보다 각 날의 중심에서 따로 찾는 편이 낫다 —
        // 중심이 3km 안이라도 날마다 조금씩 달라서 새 후보가 걸린다.
        return coroutineScope {
            dailyAttractions.indices.map { dayIndex ->
                val group = groupOfDay[dayIndex]
                val shared = byGroup[group]

                if (daysPerGroup[group] == 1 || shortageOf(shared, daysPerGroup[group]) == null) {
                    async { shared }
                } else {
                    android.util.Log.d(
                        "TripPlan",
                        "그룹 $group 후보 부족 — ${dayIndex + 1}일차를 따로 검색",
                    )
                    async {
                        val own = placeRepository
                            .searchRestaurants(condition, dayCenters[dayIndex], WIDER_SEARCH_SCALE)
                            .getOrThrow()
                        (shared + own).distinctBy { it.id }
                    }
                }
            }.map { it.await() }
        }
    }

    /**
     * 하루치 아침·점심·저녁을 고른다.
     *
     * @param usedRestaurantIds 여행 중 이미 추천한 식당. 이 함수가 직접 채우며, 날짜를 넘어서도
     *        유지되므로 같은 집이 두 번 나오지 않는다. 같은 여행에서 같은 식당을 또 가지는 않는다는 뜻이다.
     */
    private suspend fun pickMeals(
        dayNumber: Int,
        restaurants: List<Restaurant>,
        dayAttractions: List<Attraction>,
        allAttractions: List<Attraction>,
        maxPriceWon: Long?,
        usedRestaurantIds: MutableSet<String>,
        /** 4단계에서 쓰는 추가 검색. 하루에 한 번만 불린다. */
        widenSearch: suspend () -> List<Restaurant>,
    ): Map<MealTime, RecommendationPair<Restaurant>> {
        // 일정이 비는 날(도착 당일 등)은 그 도시 전체 일정을 기준으로 삼는다.
        // 기준점이 아예 없으면 동선 점수가 모두 0이 되어 평점만으로 순위가 정해져버린다.
        val anchors = dayAttractions.ifEmpty { allAttractions }.map { it.location }

        var available = restaurants
        var widened = false
        val picked = mutableMapOf<MealTime, RecommendationPair<Restaurant>>()

        for (meal in MealTime.entries) {
            var pair = selectForMeal(meal, available, anchors, maxPriceWon, usedRestaurantIds)

            // 4단계 — 여기까지 못 채웠으면 반경을 넓혀 한 번 더 찾는다.
            // 하루에 한 번으로 제한한다. 끼니마다 넓히면 호출이 세 배가 된다.
            if (pair == null && !widened) {
                widened = true
                available = (available + widenSearch()).distinctBy { it.id }
                android.util.Log.d("TripPlan", "  ${dayNumber}일차 ${meal.name}: 반경 넓혀 재검색")
                pair = selectForMeal(meal, available, anchors, maxPriceWon, usedRestaurantIds)
            }

            if (pair == null) {
                // 5단계 — 여기까지 오면 정말 없다. 화면이 "근처 추천 식당 없음"을 보여준다.
                android.util.Log.d("TripPlan", "  ${dayNumber}일차 ${meal.name}: 후보 없음")
                continue
            }

            usedRestaurantIds += pair.qualityFirst.item.id
            pair.withinBudget?.let { usedRestaurantIds += it.item.id }
            picked[meal] = pair
        }

        return picked
    }

    /**
     * 한 끼니를 **단계적으로 완화**하며 고른다.
     *
     * 왜 단계를 두는가: 예전에는 중복 제외만 하고 한 번에 골랐다. 그러면 앞날이 좋은 후보를
     * 다 써버려 뒷날은 남은 것 중 아무거나 집게 되고, 예산 초과가 줄줄이 생겼다
     * (실측: 2일차 저녁에 중복 제외 후 2곳만 남아 그중 예산 내가 1곳).
     *
     * 좋은 것부터 차례로 양보하면 "예산은 넘지만 평점이 좋은 곳"과 "예산은 맞지만 평점이 낮은 곳"
     * 중 무엇을 먼저 포기할지가 분명해진다 — 예산을 먼저 지키고 평점을 양보한다.
     *
     * 1. 예산 내 + 평점 [MIN_RATING_PREFERRED] 이상
     * 2. 예산 내 + 평점 [MIN_RATING_ACCEPTABLE] 이상
     * 3. 예산 초과 허용 (화면에 "예산 초과" 빨간 표시가 붙는다)
     *
     * 중복은 모든 단계에서 금지한다 — 완화는 품질을 양보하는 것이고, 같은 집을 두 번 가는 것은
     * 품질 문제가 아니라 계획이 잘못된 것이다. 단, Mock은 후보가 15곳뿐이라 예외다
     * ([allowRestaurantReuse]).
     */
    private fun selectForMeal(
        meal: MealTime,
        restaurants: List<Restaurant>,
        anchors: List<LatLng>,
        maxPriceWon: Long?,
        usedRestaurantIds: Set<String>,
    ): RecommendationPair<Restaurant>? {
        // 끼니에 어울리는 곳만 후보로 둔다 — 아침에 오마카세, 저녁에 카페가 뜨는 것을 막는다.
        val matching = restaurants.filter { meal in it.suitableMeals }
        // 그 끼니에 맞는 곳이 아예 없으면 끼니를 비우느니 전체 후보에서 고른다.
        val forThisMeal = matching.ifEmpty { restaurants }

        val fresh = forThisMeal.filterNot { it.id in usedRestaurantIds }
        // Mock은 후보가 적어 긴 여행에서 금방 소진된다. 테스트 중 화면이 비는 것을 막기 위해서만
        // 재사용을 허용한다. 실제 API 경로에서는 fresh가 비면 그 끼니를 비운다.
        val pool = fresh.ifEmpty { if (allowRestaurantReuse) forThisMeal else emptyList() }
        if (pool.isEmpty()) return null

        val withinBudget = if (maxPriceWon == null) {
            pool
        } else {
            pool.filter { it.priceWon <= maxPriceWon }
        }

        // 단계별 후보 목록. 앞일수록 엄격하다.
        val stages = listOf(
            withinBudget.filter { it.rating >= MIN_RATING_PREFERRED },
            withinBudget.filter { it.rating >= MIN_RATING_ACCEPTABLE },
            // 3단계: 예산을 넘겨도 채운다. 빈칸보다는 "조금 비싸다"는 정보가 낫다.
            pool,
        )

        // **두 선택지가 다 채워지는 단계를 먼저 찾는다.**
        //
        // 단계를 순서대로 보며 처음 성공한 것을 쓰면, 엄격한 단계에 후보가 1곳뿐일 때
        // "대안 없음"이 되어 선택지 한 칸이 빈다. CLAUDE.md는 항상 두 가지를 주라고 하고,
        // 그건 후보가 정말 하나일 때만 예외다 — 기준을 조여서 하나로 만든 것은 예외가 아니다.
        //
        // 평점 4.0짜리 한 곳보다 3.5 이상 두 곳이 낫다고 본다. 비교할 대상이 있어야 선택이 된다.
        stages.forEach { stage ->
            val pair = recommendPair(stage, anchors, maxPriceWon)
            if (pair?.withinBudget != null) return pair
        }

        // 어느 단계에서도 둘을 못 채웠다. 후보가 정말 하나인 경우다 — 하나라도 보여준다.
        return stages.firstNotNullOfOrNull { recommendPair(it, anchors, maxPriceWon) }
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
        val shortage = shortageOf(nearby, dayCount)
        if (shortage == null) {
            logCandidates("그룹(${dayCount}일) 1차", nearby)
            return nearby
        }

        android.util.Log.d("TripPlan", "후보 부족($shortage) — 반경 ${WIDER_SEARCH_SCALE}배로 재검색")
        val wider = placeRepository
            .searchRestaurants(condition, center, WIDER_SEARCH_SCALE)
            .getOrThrow()

        val merged = (nearby + wider).distinctBy { it.id }
        logCandidates("그룹(${dayCount}일) 재검색 후", merged)
        return merged
    }

    /**
     * 진단용 — 후보를 끼니별·예산별로 센다.
     *
     * "후보가 N곳"만 알면 왜 예산 초과가 쏟아지는지 알 수 없다. 끼니별로 쓸 수 있는 곳이
     * 몇이고 그중 예산 안이 몇인지를 봐야 원인이 후보 수인지 가격대인지 갈린다.
     */
    private fun logCandidates(label: String, candidates: List<Restaurant>) {
        val mealBudget = lastMealBudgetWon
        val breakdown = MealTime.entries.joinToString(" / ") { meal ->
            val forMeal = candidates.filter { meal in it.suitableMeals }
            val within = if (mealBudget == null) {
                "-"
            } else {
                forMeal.count { it.priceWon <= mealBudget }.toString()
            }
            "${meal.name} ${forMeal.size}곳(예산내 $within)"
        }
        android.util.Log.d("TripPlan", "$label: 총 ${candidates.size}곳 · $breakdown")
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
            if (count < neededPerMeal) "${meal.name} $count/$neededPerMeal" else null
        }.toMutableList()

        // 점심과 저녁은 거의 같은 후보를 쓴다(일반 식당은 둘 다 가능). 중복 배제 때문에 한 곳은
        // 한 번만 쓸 수 있으니 **두 끼니를 합쳐서** 세어야 한다.
        // 끼니별로만 보면 "점심 8곳 / 저녁 8곳"이 충분해 보이는데 실제로는 같은 8곳이라
        // 2일치 네 칸을 채우고 나면 바닥난다. 실측에서 2일차 저녁에 2곳만 남았다.
        val lunchOrDinner = candidates.count {
            MealTime.LUNCH in it.suitableMeals || MealTime.DINNER in it.suitableMeals
        }
        val neededLunchDinner = neededPerMeal * 2
        if (lunchOrDinner < neededLunchDinner) {
            reasons += "점심·저녁 합계 $lunchOrDinner/$neededLunchDinner"
        }

        // 묶인 날 수 × 6(3끼 × 2선택지)을 전체 하한으로 둔다.
        val neededTotal = dayCount * MealTime.entries.size * CHOICES_PER_MEAL
        if (candidates.size < neededTotal) {
            reasons += "전체 ${candidates.size}/$neededTotal"
        }

        return reasons.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    // 추천 결과를 전부 찍어 보던 진단 로그(끼니 수·중복·빈 끼니·끼니별 추천 목록)는 지웠다.
    // 빈 끼니와 "식당 아닌 곳이 섞이는" 문제를 잡는 데 썼고 둘 다 해결됐다.
    // 남긴 로그는 호출 수와 캐시 적중뿐이다 — 그건 비용에 직접 연결되므로 계속 봐야 한다.

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

        /**
         * 끼니 선택 4단계에서 쓰는 마지막 반경 배수.
         * 여기까지 왔으면 "동선이 조금 멀어도 갈 곳이 있는 게 낫다"는 상황이다.
         */
        const val WIDEST_SEARCH_SCALE = 3.0

        /**
         * 끼니 선택 완화 단계의 평점 기준.
         *
         * 4.0은 Google 평점에서 "가 볼 만하다"의 통상 경계다. 3.5까지 내리면 선택지가 크게
         * 늘어나지만 더 내리면 추천이라 부르기 어려워져서, 그 아래는 평점 대신 예산을 양보한다.
         */
        const val MIN_RATING_PREFERRED = 4.0
        const val MIN_RATING_ACCEPTABLE = 3.5
    }
}