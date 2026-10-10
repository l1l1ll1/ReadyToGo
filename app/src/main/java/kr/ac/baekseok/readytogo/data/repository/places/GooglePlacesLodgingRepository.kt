package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.LodgingRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kr.ac.baekseok.readytogo.domain.budget.LodgingPriceTable
import com.google.android.gms.maps.model.LatLng as GmsLatLng

/**
 * Google Places로 숙소를 찾는다. MockLodgingRepository를 대체한다.
 *
 * Places는 **숙소 요금을 주지 않는다.** 가격 등급(0~4)만 있어서 1박가는 전부 추정이다.
 * 여기서는 기본표 값(배수 1.0 기준)으로 채우고, 도시 물가 배수는 TripPlanner가 나중에 곱한다.
 * 배수를 여기서 받으려면 식당 검색이 끝나기를 기다려야 해서 검색 병렬 실행이 깨지기 때문이다.
 */
class GooglePlacesLodgingRepository(
    context: Context,
) : LodgingRepository {

    private val client: PlacesClient by lazy { PlacesClientFactory.create(context) }

    /** 같은 도시를 다시 검색하지 않도록 결과를 기기에 저장해 둔다. */
    private val cache by lazy { PlaceSearchCache(context) }

    override suspend fun search(condition: TripCondition): Result<List<Lodging>> = runCatching {
        val center = condition.destination?.location
            ?: error("목적지를 먼저 골라야 숙소를 찾을 수 있습니다.")

        val cacheKey = cache.keyOf("lodging", center, SEARCH_RADIUS_M, LODGING_TYPES)
        cache.readLodgings(cacheKey)?.let { return@runCatching it }

        val request = SearchNearbyRequest
            .builder(
                CircularBounds.newInstance(
                    GmsLatLng(center.latitude, center.longitude),
                    SEARCH_RADIUS_M,
                ),
                PLACE_FIELDS,
            )
            .setIncludedTypes(LODGING_TYPES)
            .setMaxResultCount(MAX_RESULTS)
            .build()

        val lodgings = client.searchNearby(request).await().places.mapNotNull { it.toLodging() }
        cache.writeLodgings(cacheKey, lodgings)
        lodgings
    }

    /** 평점이나 좌표가 없으면 추천 점수를 매길 수 없으므로 버린다. */
    private fun Place.toLodging(): Lodging? {
        val position = location ?: return null
        val score = rating ?: return null

        return Lodging(
            id = id ?: return null,
            name = displayName ?: return null,
            rating = score,
            reviewCount = userRatingCount ?: 0,
            location = LatLng(position.latitude, position.longitude),
            priceWon = LodgingPriceTable.basePriceWon(priceLevel),
            address = shortFormattedAddress ?: "",
            // 예약 기능은 넣지 않는다(CLAUDE.md 범위). 지도 링크만 걸어 준다.
            bookingUrl = googleMapsUri?.toString() ?: "",
            priceLevel = priceLevel,
            priceEstimated = true,
        )
    }

    private companion object {
        /** 도시 중심에서 이 반경 안. 식당보다 넓게 잡을 이유가 없다. */
        const val SEARCH_RADIUS_M = 15_000.0
        const val MAX_RESULTS = 20
        const val TAG = "LodgingSearch"

        val LODGING_TYPES = listOf("hotel", "motel", "guest_house", "hostel", "bed_and_breakfast")

        /** 요청 항목마다 요금이 달라지므로 추천·표시에 꼭 필요한 것만 받는다. */
        val PLACE_FIELDS = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.SHORT_FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING,
            Place.Field.USER_RATING_COUNT,
            Place.Field.PRICE_LEVEL,
            Place.Field.GOOGLE_MAPS_URI,
        )
        // SERVES_BREAKFAST는 넣지 않는다. 호텔에는 값이 채워지지 않고(자카르타 13곳 전부 UNKNOWN),
        // 넣기만 해도 Nearby Search 과금이 Enterprise → Enterprise + Atmosphere로 올라간다.
        // "호텔 조식" 선택지는 Places로 구현할 수 없다.
    }
}
