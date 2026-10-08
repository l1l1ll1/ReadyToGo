package kr.ac.baekseok.readytogo.data.repository.mock

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.PlaceRepository
import kotlinx.coroutines.delay

/**
 * 장소(식당 · 관광지) Mock.
 *
 * 관광지는 여행 목적에 따라 후보를 좁힌다. 실제 Places API도 카테고리로 거르므로
 * 지금부터 같은 모양으로 만들어 둔다. 목적을 고르지 않았으면 전부 후보가 된다.
 */
class MockPlaceRepository : PlaceRepository {

    /** Mock은 도쿄 데이터 하나뿐이라 중심점을 쓰지 않는다. 실제 구현체는 이 값으로 검색 범위를 정한다. */
    override suspend fun searchRestaurants(
        condition: TripCondition,
        center: LatLng,
        radiusScale: Double,
    ): Result<List<Restaurant>> {
        delay(NETWORK_DELAY_MS)
        return Result.success(MockData.restaurants)
    }

    override suspend fun searchAttractions(condition: TripCondition): Result<List<Attraction>> {
        delay(NETWORK_DELAY_MS)
        if (condition.purposes.isEmpty()) {
            return Result.success(MockData.attractions)
        }

        val wantedIds = condition.purposes.flatMap { ATTRACTION_IDS_BY_PURPOSE[it].orEmpty() }.toSet()
        val filtered = MockData.attractions.filter { it.id in wantedIds }

        // 목적에 맞는 곳이 너무 적으면 일정이 비어버린다. 그럴 땐 전체 후보로 되돌린다.
        return Result.success(filtered.ifEmpty { MockData.attractions })
    }

    private companion object {
        const val NETWORK_DELAY_MS = 500L

        /** 목적별로 어울리는 관광지. 7단계에서는 Places API의 카테고리가 이 역할을 한다. */
        val ATTRACTION_IDS_BY_PURPOSE: Map<TravelPurpose, List<String>> = mapOf(
            TravelPurpose.REST to listOf("a3", "a5", "a8", "a12"),
            TravelPurpose.SIGHTSEEING to listOf("a1", "a2", "a3", "a5", "a6", "a9", "a13", "a15"),
            TravelPurpose.FOOD to listOf("a11", "a10", "a7", "a1"),
            TravelPurpose.ACTIVITY to listOf("a2", "a4", "a12", "a14", "a7"),
        )
    }
}