package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.repository.ExchangeRateRepository
import kr.ac.baekseok.readytogo.data.repository.PlaceDetailsRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kr.ac.baekseok.readytogo.data.repository.exchange.OpenExchangeRateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 장소 하나를 Places에서 받아 온다.
 *
 * **메모리에만 기억한다.** 가격과 평점은 시간이 지나면 틀려지는 값이라 기기에 남기지
 * 않는다 — 앱을 다시 켜면 새로 받는다. 대신 앱이 떠 있는 동안 같은 곳을 또 고르면
 * 호출하지 않는다.
 *
 * 조회는 과금된다. 그래서 [PlaceDetailsRepository] 주석대로 "실제로 끼워 넣을 때"만
 * 불러야 하고, 이 클래스는 그 약속을 지키는 쪽(화면)을 믿는다.
 */
class GooglePlaceDetailsRepository(
    context: Context,
    private val client: PlacesClient = PlacesClientFactory.create(context),
    private val exchangeRates: ExchangeRateRepository = OpenExchangeRateRepository(),
) : PlaceDetailsRepository {

    private val restaurants = mutableMapOf<String, Restaurant>()
    private val attractions = mutableMapOf<String, Attraction>()

    /** 같은 곳을 동시에 두 번 묻지 않게 막는다. 그대로 두면 과금이 두 배가 된다. */
    private val lock = Mutex()

    override suspend fun restaurantFor(favorite: FavoritePlace): Restaurant = lock.withLock {
        restaurants[favorite.placeId]?.let { return it }

        val place = fetch(favorite.placeId)
        val price = place?.realPrice(exchangeRates)

        val restaurant = Restaurant(
            id = favorite.placeId,
            // 받아 온 이름보다 저장해 둔 이름을 앞세운다. 사용자가 담을 때 본 이름이다.
            name = favorite.name,
            category = place?.primaryTypeDisplayName
                ?: favorite.category.ifBlank {
                    AppContainer.string(R.string.place_category_restaurant)
                },
            rating = place?.rating ?: 0.0,
            reviewCount = place?.userRatingCount ?: 0,
            location = favorite.location,
            // 가격을 모르면 0으로 두고 priceUnknown으로 알린다. 예산 판정에서는
            // 0원이라 "예산 내"로 잡히는데, 화면이 "가격 정보 없음"을 적으므로
            // 사용자가 0원으로 오해하지는 않는다.
            priceWon = price?.maxWon ?: 0L,
            minPriceWon = price?.minWon,
            localPriceText = price?.localText,
            priceEstimated = price?.estimated ?: false,
            priceIsUpperBound = price?.upperBoundOnly ?: false,
            priceUnknown = price == null,
            address = place?.shortFormattedAddress ?: "",
            mapUrl = favorite.mapUrl,
            // 어느 끼니에나 넣을 수 있게 둔다. 사용자가 직접 고른 곳이라
            // 앱이 "이 집은 아침에 안 맞는다"고 막을 이유가 없다.
            suitableMeals = MealTime.entries.toSet(),
        )

        restaurants[favorite.placeId] = restaurant
        return restaurant
    }

    override suspend fun attractionFor(favorite: FavoritePlace): Attraction = lock.withLock {
        attractions[favorite.placeId]?.let { return it }

        val place = fetch(favorite.placeId)

        val attraction = Attraction(
            id = favorite.placeId,
            name = favorite.name,
            rating = place?.rating ?: DEFAULT_ATTRACTION_RATING,
            location = favorite.location,
            stayMinutes = DEFAULT_STAY_MINUTES,
            address = place?.shortFormattedAddress ?: "",
            reviewCount = place?.userRatingCount ?: 0,
            types = place?.placeTypes.orEmpty(),
            mapUrl = favorite.mapUrl,
        )

        attractions[favorite.placeId] = attraction
        return attraction
    }

    /** 실패하면 null. 호출부가 즐겨찾기에 있는 값만으로 만들어 넣는다. */
    private suspend fun fetch(placeId: String): Place? = withContext(Dispatchers.IO) {
        runCatching {
            val request = FetchPlaceRequest.builder(placeId, DETAIL_FIELDS).build()
            client.fetchPlace(request).await().place
        }
            .onFailure { Log.w(TAG, "장소 상세를 받지 못했다: $placeId", it) }
            .getOrNull()
    }

    private companion object {
        const val TAG = "PlaceDetails"

        /**
         * 받아 올 항목.
         *
         * 사진은 빼 둔다 — 사진은 설정 토글과 월 상한이 따로 관리하므로
         * (GooglePlacesPhotoRepository) 여기서 같이 받으면 그 장치를 우회하게 된다.
         */
        val DETAIL_FIELDS = listOf(
            Place.Field.RATING,
            Place.Field.USER_RATING_COUNT,
            Place.Field.PRICE_RANGE,
            Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
            Place.Field.SHORT_FORMATTED_ADDRESS,
            Place.Field.TYPES,
        )
    }
}
