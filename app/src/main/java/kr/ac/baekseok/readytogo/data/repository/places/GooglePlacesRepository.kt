package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.BuildConfig
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.ExchangeRateRepository
import kr.ac.baekseok.readytogo.data.repository.PlaceRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kr.ac.baekseok.readytogo.domain.purpose.PurposeAttractionTypes
import kr.ac.baekseok.readytogo.data.repository.exchange.OpenExchangeRateRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.DayOfWeek
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.maps.model.LatLng as GmsLatLng

/**
 * Google Places API로 식당·관광지를 찾는다. MockPlaceRepository를 대체한다.
 *
 * 도시 좌표를 이미 갖고 있어서(목록에서 골랐으므로) Geocoding 호출 없이 바로 주변 검색을 한다.
 * 검색 한 번에 과금되므로 호출 수를 최소로 유지한다 — 여행당 식당 1회, 관광지 1회.
 */
class GooglePlacesRepository(
    context: Context,
    private val exchangeRates: ExchangeRateRepository = OpenExchangeRateRepository(),
) : PlaceRepository {

    private val client: PlacesClient by lazy { PlacesClientFactory.create(context) }

    /** 같은 검색을 두 번 보내지 않도록 결과를 기기에 저장해 둔다. */
    private val cache by lazy { PlaceSearchCache(context) }

    override suspend fun searchRestaurants(
        condition: TripCondition,
        center: LatLng,
        radiusScale: Double,
    ): Result<List<Restaurant>> =
        runCatching {
            val radius = RESTAURANT_RADIUS_M * radiusScale

            // 같은 중심·반경으로 다시 찾으면 결과가 거의 같다. 저장해 둔 것을 쓴다.
            // 환율 변환까지 끝난 모델을 저장하므로 환율 호출도 같이 아낀다.
            val cacheKey = cache.keyOf("restaurant", center, radius, MEAL_TYPES + BREAKFAST_SEARCH_TYPES)
            cache.readRestaurants(cacheKey)?.let { return@runCatching it }
            // 검색을 두 번 나눈 이유: 한 번에 20개만 오는데 restaurant·cafe를 같이 요청하면
            // 카페·베이커리가 두어 곳밖에 안 섞여서 아침 후보가 동나고, 같은 가게가
            // "추천"과 "다른 선택"에 동시에 뜬다. 아침용을 따로 받아 후보를 확보한다.
            //
            // 두 검색은 서로를 기다릴 이유가 없으므로 동시에 보낸다.
            val (meals, breakfast) = coroutineScope {
                // 환율도 검색과 같이 미리 받는다. 안 그러면 검색이 다 끝난 뒤
                // 첫 가격을 환산하는 순간에야 네트워크를 타서 그만큼 더 기다린다.
                val ratesAsync = async { exchangeRates.prepare() }
                val mealsAsync =
                    async { searchNearby(center, MEAL_TYPES, radius, NON_EATERY_PRIMARY_TYPES) }
                val breakfastAsync = async {
                    searchNearby(center, BREAKFAST_SEARCH_TYPES, radius, NON_EATERY_PRIMARY_TYPES)
                }

                ratesAsync.await()
                mealsAsync.await() to breakfastAsync.await()
            }

            val places = (meals + breakfast)
                .distinctBy { it.id }
                // 검색 자체를 좁혔지만 그래도 식당 아닌 곳이 섞여 온다. 마지막 그물이다.
                .filter { it.isEatery() }
                // 한국에도 있는 프랜차이즈는 해외에서 갈 이유가 없다.
                .filterNot { place ->
                    FranchiseFilter.isExcluded(
                        name = place.displayName.orEmpty(),
                        isEatery = true,
                    )
                }

            // 실제 가격이 있는 곳을 먼저 환산하고, 그 값으로 이 도시의 등급별 시세를 만든다.
            // 그래야 가격 정보가 없는 곳을 **그 도시 물가 기준**으로 추정할 수 있다.
            val realPrices = places.associate { it.id to it.realPrice(exchangeRates) }
            val marketPrices = marketPriceByLevel(places, realPrices)

            val result = places.mapNotNull { place ->
                val price = realPrices[place.id] ?: place.estimatedPrice(marketPrices)
                place.toRestaurant(price)
            }
            cache.writeRestaurants(cacheKey, result)
            result
        }

    /**
     * 이 도시의 가격 등급별 시세(중앙값)를 구한다.
     *
     * 등급을 고정된 금액표로 환산하면 물가가 다른 나라에서 크게 빗나간다 —
     * 자카르타의 실제 한 끼가 3천원인데 표에 따라 10만원으로 추정되면
     * 멀쩡한 식당이 "예산 초과"로 밀려난다.
     * 같은 검색 결과 안에 가격이 공개된 식당이 있으므로, 그 값에서 시세를 끌어낸다.
     */
    private fun marketPriceByLevel(
        places: List<Place>,
        realPrices: Map<String?, ResolvedPrice?>,
    ): Map<Int, Long> =
        places.mapNotNull { place ->
            val price = realPrices[place.id] ?: return@mapNotNull null
            val level = place.priceLevel ?: return@mapNotNull null
            level to price.maxWon
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, prices) -> prices.sorted()[prices.size / 2] }

    override suspend fun searchAttractions(condition: TripCondition): Result<List<Attraction>> =
        runCatching {
            val center = condition.destination?.location
                ?: error("목적지를 먼저 골라야 관광지를 찾을 수 있습니다.")

            val types = PurposeAttractionTypes.typesFor(condition.purposes)
            // 목적을 바꾸면 찾는 타입이 달라지므로 키에 타입을 넣는다.
            // 뺀 유형도 넣는다("-hotel") — 빼는 목록이 바뀌면 그 전에 받아 둔 결과(호텔이 섞인)를
            // 7일 동안 계속 쓰게 된다.
            val excluded = LODGING_PRIMARY_TYPES
            val cacheKey = cache.keyOf("attraction", center, CITY_RADIUS_M, types + excluded.map { "-$it" })
            cache.readAttractions(cacheKey)?.let { return@runCatching it }

            val attractions = searchNearby(center, types, CITY_RADIUS_M, excluded)
                .mapNotNull { it.toAttraction() }
            cache.writeAttractions(cacheKey, attractions)
            attractions
        }

    /**
     * @param excludedPrimaryTypes 본업이 이 유형인 장소는 아예 받지 않는다.
     *        식당 검색은 [NON_EATERY_PRIMARY_TYPES]를, 관광지 검색은 숙박([LODGING_PRIMARY_TYPES])만 뺀다
     *        — 관광지 검색에서 `tourist_attraction`까지 빼면 안 되기 때문이다.
     */
    private suspend fun searchNearby(
        center: LatLng,
        types: List<String>,
        radiusMeters: Double,
        excludedPrimaryTypes: List<String> = emptyList(),
    ): List<Place> {
        val request = SearchNearbyRequest
            .builder(
                CircularBounds.newInstance(GmsLatLng(center.latitude, center.longitude), radiusMeters),
                PLACE_FIELDS,
            )
            .setIncludedTypes(types)
            .setExcludedPrimaryTypes(excludedPrimaryTypes)
            .setMaxResultCount(MAX_RESULTS)
            .build()

        return client.searchNearby(request).await().places
    }

    /** 평점이나 좌표가 없는 장소는 점수를 매길 수 없으므로 버린다. */
    private fun Place.toRestaurant(price: ResolvedPrice): Restaurant? {
        val position = location ?: return null
        val score = rating ?: return null

        return Restaurant(
            id = id ?: return null,
            name = displayName ?: return null,
            category = primaryTypeDisplayName
                ?: AppContainer.string(R.string.place_category_restaurant),
            rating = score,
            reviewCount = userRatingCount ?: 0,
            location = LatLng(position.latitude, position.longitude),
            priceWon = price.maxWon,
            minPriceWon = price.minWon,
            localPriceText = price.localText,
            priceEstimated = price.estimated,
            priceIsUpperBound = price.upperBoundOnly,
            address = shortFormattedAddress ?: "",
            mapUrl = googleMapsUri?.toString() ?: "",
            suitableMeals = suitableMeals(),
            openDays = openDays(),
            primaryType = primaryType,
        )
    }

    /**
     * 가격이 공개되지 않은 식당의 값을 추정한다.
     *
     * 같은 도시·같은 등급 식당들의 실제 가격 중앙값을 쓴다. 그 등급에 표본이 없으면
     * 가장 가까운 등급의 시세를 기준으로 잡고, 도시 전체에 표본이 하나도 없을 때만
     * 내장 기본표로 물러난다.
     */
    private fun Place.estimatedPrice(marketPrices: Map<Int, Long>): ResolvedPrice {
        val level = priceLevel ?: DEFAULT_PRICE_LEVEL
        val fromMarket = marketPrices[level]
            ?: marketPrices.minByOrNull { kotlin.math.abs(it.key - level) }
                ?.let { (sampleLevel, price) ->
                    // 표본이 다른 등급뿐이면 등급 차이만큼 배수를 적용한다.
                    // 등급이 한 칸 오를 때 대략 두 배로 본다.
                    val steps = level - sampleLevel
                    (price * Math.pow(LEVEL_STEP_MULTIPLIER, steps.toDouble())).toLong()
                }

        return ResolvedPrice(
            maxWon = fromMarket ?: PRICE_BY_LEVEL[level] ?: PRICE_BY_LEVEL.getValue(DEFAULT_PRICE_LEVEL),
            minWon = null,
            localText = null,
            estimated = true,
        )
    }

    /** 메뉴판과 바로 대조할 수 있게 현지 통화도 적어둔다. 예: "€17~€28", "¥1,000 이하" */
    private fun Place.toAttraction(): Attraction? {
        val position = location ?: return null

        return Attraction(
            id = id ?: return null,
            name = displayName ?: return null,
            rating = rating ?: DEFAULT_ATTRACTION_RATING,
            reviewCount = userRatingCount ?: 0,
            location = LatLng(position.latitude, position.longitude),
            stayMinutes = DEFAULT_STAY_MINUTES,
            address = shortFormattedAddress ?: "",
            // 목적 일치 가산점을 매기려면 이 장소가 어떤 종류인지 알아야 한다.
            types = placeTypes.orEmpty(),
            // 식당과 같은 필드다. 상세 시트에서 지도 앱으로 넘기는 데 쓴다.
            mapUrl = googleMapsUri?.toString().orEmpty(),
            openDays = openDays(),
        )
    }

    /**
     * 음식점인지 판단한다. **`primaryType`(그 장소의 본업)으로만 본다.**
     *
     * `types` 배열로 보면 안 된다 — 거기에는 그 장소에 딸려 있는 것까지 전부 들어간다.
     * 호텔은 안에 식당이 있어서 `restaurant`가, 연회장은 `food`가, 백화점은 `restaurant`가 붙는다.
     * 실제로 도쿄 일정에서 점심에 "아사쿠사 뷰 호텔", 저녁에 연회장과 잡화점이 추천됐다.
     *
     * 세부 타입을 일일이 나열하지는 않는다. Google의 음식 타입이 100개가 넘어
     * `hamburger_restaurant`·`asian_restaurant` 같은 멀쩡한 식당이 대량으로 버려졌던 적이 있다.
     * 그래서 `_restaurant`로 끝나는 것은 전부 받고, 그 외의 음식점만 [EATERY_PRIMARY_TYPES]에 둔다.
     */
    private fun Place.isEatery(): Boolean {
        val primary = primaryType
            // primaryType을 안 주는 장소가 있다. 그때만 예전처럼 상위 타입으로 본다.
            ?: return placeTypes.orEmpty().any { it in EATERY_TYPES }

        return primary.endsWith(RESTAURANT_TYPE_SUFFIX) || primary in EATERY_PRIMARY_TYPES
    }

    /**
     * 어느 끼니에 어울리는지. Mock에서 카테고리로 판단하던 것을 Places의 타입으로 대체한다.
     * 카페·베이커리는 아침, 술집은 저녁으로 본다.
     */
    private fun Place.suitableMeals(): Set<MealTime> {
        val types = placeTypes.orEmpty()
        return when {
            types.any { it in BREAKFAST_TYPES } -> setOf(MealTime.BREAKFAST)
            types.any { it in DINNER_ONLY_TYPES } -> setOf(MealTime.DINNER)
            else -> setOf(MealTime.LUNCH, MealTime.DINNER)
        }
    }

    /**
     * 문을 여는 요일. 영업시간이 없으면 null(모름).
     *
     * 요일만 본다 — 일정표에 시각이 없어서(DayTimeline 참고) "몇 시에 닫는다"는 판정할 수 없다.
     * 닫는 시각이 없는 구간은 24시간 영업이라 모든 요일로 본다(Places가 일요일 0시 하나로 준다).
     */
    private fun Place.openDays(): Set<DayOfWeek>? {
        val periods = openingHours?.periods?.takeIf { it.isNotEmpty() } ?: return null
        if (periods.any { it.close == null }) return DayOfWeek.entries.toSet()
        return periods.mapNotNull { period ->
            period.open?.day?.let { DayOfWeek.valueOf(it.name) }
        }.toSet()
    }

    private companion object {
        /** 관광지용. 도시 하나를 덮는 정도. */
        const val CITY_RADIUS_M = 15_000.0

        /**
         * 식당용. 그날 일정 중심에서 이 반경 안만 찾는다.
         * 도시 전체(15km)에서 찾으면 그날 동선과 먼 식당이 후보에 섞인다.
         */
        const val RESTAURANT_RADIUS_M = 5_000.0

        /** Places API 한 번 호출로 받을 수 있는 최대치. 호출 수를 늘리지 않으려 한 번에 최대로 받는다. */
        const val MAX_RESULTS = 20

        const val DEFAULT_PRICE_LEVEL = 2

        /**
         * 가격대 하한이 이 값 이하면 "하한 없음"으로 본다.
         * Google이 저가 구간을 1단위부터 표기하기 때문이다(¥1~¥1,000).
         */

        /** 가격 등급이 한 칸 오를 때의 대략적인 배수. 표본이 없는 등급을 메울 때만 쓴다. */
        const val LEVEL_STEP_MULTIPLIER = 2.0

        /** 가격 등급 → 1인 1끼 추정 금액(원). 숫자만 바꿔 조정할 수 있게 분리했다. */
        val PRICE_BY_LEVEL = mapOf(
            0 to 8_000L,
            1 to 12_000L,
            2 to 25_000L,
            3 to 50_000L,
            4 to 100_000L,
        )

        /**
         * 받아올 항목. Places는 **요청한 항목에 따라 요금이 달라지므로** 꼭 필요한 것만 적는다.
         * 추천에 쓰는 평점·리뷰 수·좌표·가격대와, 화면에 보여줄 이름·주소·링크가 전부다.
         */
        val PLACE_FIELDS = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            // 짧은 주소. FORMATTED_ADDRESS는 "일본 〒160-0022 Tokyo, Shinjuku City, …"처럼
            // 우편번호·국가까지 들어가 카드가 지저분해진다.
            Place.Field.SHORT_FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING,
            Place.Field.USER_RATING_COUNT,
            Place.Field.PRICE_LEVEL,
            Place.Field.PRICE_RANGE,
            Place.Field.TYPES,
            Place.Field.PRIMARY_TYPE,
            Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
            Place.Field.GOOGLE_MAPS_URI,
            // 정기 영업시간. 일정 검토관이 방문일 휴무를 찾는다. 평점·가격대와 같은
            // Enterprise 등급 필드라 이미 내는 요금에서 단가가 오르지 않는다.
            Place.Field.OPENING_HOURS,
        )

        /** 점심·저녁용. 20칸을 전부 실제 식당으로 채우기 위해 카페를 섞지 않는다. */
        val MEAL_TYPES = listOf("restaurant")

        /** 아침용. 따로 받아야 카페·베이커리 후보가 충분히 확보된다. */
        val BREAKFAST_SEARCH_TYPES = listOf("cafe", "bakery", "coffee_shop", "breakfast_restaurant")

        /**
         * 음식점으로 인정하는 상위 타입.
         * `primaryType`을 주지 않는 장소에만 쓰는 폴백이다 — 평소 판단은 primaryType으로 한다.
         */
        val EATERY_TYPES = setOf(
            "restaurant", "food", "cafe", "bakery", "bar", "meal_takeaway",
        )

        /** `primaryType`이 이것으로 끝나면 종류를 따지지 않고 식당으로 본다. */
        const val RESTAURANT_TYPE_SUFFIX = "_restaurant"

        /**
         * 숙박 시설의 본업 유형. 식당·관광지 검색 둘 다에서 뺀다.
         *
         * 관광지 검색에도 호텔이 걸려든다 — 스파·정원이 딸린 호텔은 `spa`·`garden`이 붙어
         * 휴양 검색에 매칭된다. 삿포로 2일차에 호텔 두 곳이 관광지 자리를 차지했다.
         * 숙소는 숙소 탭이 따로 고르므로 관광지로 보여줄 이유가 없다.
         *
         * 식당 검색에서 이미 실측으로 통과한 이름만 쓴다. 표에 없는 이름을 보내면 요청 전체가 거부된다.
         */
        val LODGING_PRIMARY_TYPES = listOf(
            "hotel", "lodging", "resort_hotel", "motel", "hostel", "inn", "guest_house",
        )

        /**
         * 식당 검색에서 **요청 단계에** 빼는 유형.
         *
         * Places의 `includedTypes`는 primaryType이 아니라 types 배열 전체로 매칭한다.
         * 백화점·호텔·영화관은 안에 식당이 있어 `restaurant`가 붙으므로 식당 검색에 걸려들고,
         * 유명세로 상위에 올라 한 번에 받는 20칸을 차지한다. 실측에서 35곳 중 26곳이 이것들이었다.
         *
         * [isEatery]로 걸러도 되지만, 그러면 20칸을 쓰레기로 받고 버리는 셈이라
         * 진짜 식당이 애초에 결과에 들어오지 못한다. 그래서 요청에서 뺀다.
         * 호출 수는 그대로이므로 비용은 늘지 않는다.
         */
        val NON_EATERY_PRIMARY_TYPES = listOf(
            // 숙박 — 안에 식당이 있어서 가장 많이 걸려들었다.
            *LODGING_PRIMARY_TYPES.toTypedArray(),
            // 쇼핑 — 백화점·쇼핑몰의 식당가가 통째로 걸린다.
            "department_store", "shopping_mall", "store", "book_store", "clothing_store",
            "gift_shop", "convenience_store", "supermarket", "grocery_store", "market",
            // 공연·행사장 — 연회장(banquet_hall)이 저녁 추천으로 올라왔던 적이 있다.
            "movie_theater", "banquet_hall", "event_venue", "convention_center",
            "performing_arts_theater", "amusement_center", "amusement_park",
            // 그 외 식사 목적이 아닌 큰 시설.
            "tourist_attraction", "night_club", "train_station", "subway_station",
            "airport", "gas_station", "hospital", "association_or_organization",
        )

        /**
         * `_restaurant`로 끝나지 않는 음식점 타입.
         *
         * **끼니를 해결하는 곳만** 넣는다. `banquet_hall`(연회장)·`hotel`은 음식을 팔지만 식사하러
         * 가는 곳이 아니라서 빼고, `ice_cream_shop`·`candy_store` 같은 간식 가게도 빼놨다 —
         * 넣으면 [suitableMeals]의 기본값에 걸려 점심·저녁 추천으로 올라온다.
         */
        val EATERY_PRIMARY_TYPES = setOf(
            "restaurant", "cafe", "cafeteria", "coffee_shop", "bakery", "bagel_shop",
            "sandwich_shop", "deli", "diner", "food_court", "meal_takeaway", "meal_delivery",
            "bar", "bar_and_grill", "pub", "wine_bar", "steak_house", "tea_house",
        )
        /** 현지 통화 표기용. 없는 통화는 코드를 그대로 쓴다. */

        val BREAKFAST_TYPES = setOf("cafe", "bakery", "coffee_shop", "breakfast_restaurant")
        val DINNER_ONLY_TYPES = setOf("bar", "pub", "wine_bar", "night_club")

        // 목적별 관광지 종류는 PurposeAttractionTypes에 있다.
        // 검색과 점수 가산이 같은 표를 봐야 어긋나지 않는다.
    }
}