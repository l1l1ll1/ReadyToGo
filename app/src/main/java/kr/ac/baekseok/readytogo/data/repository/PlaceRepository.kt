package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TripCondition

/**
 * 장소(식당 · 관광지) 검색.
 *
 * 식당과 관광지를 한 인터페이스로 묶은 이유: 7단계에서 둘 다 Google Places API 하나로 가져올 예정이라
 * 구현체를 나눠봐야 같은 클라이언트를 두 번 들고 있게 된다.
 */
interface PlaceRepository {
    /**
     * 식당을 찾는다.
     *
     * @param center 검색 중심. **그날 일정의 중심점**을 넘긴다.
     *        도시 중심에서 한 번만 찾으면 여행 내내 같은 후보를 나눠 쓰게 되어,
     *        긴 여행에서 같은 식당이 반복되고 그날 동선과도 멀어진다.
     */
    suspend fun searchRestaurants(
        condition: TripCondition,
        center: LatLng,
        /**
         * 기본 반경에 곱할 배수. 후보가 모자란 곳만 넓혀 다시 찾을 때 쓴다.
         * 구체적인 반경(미터)은 구현체가 정한다 — 호출부가 알 필요 없다.
         */
        radiusScale: Double = 1.0,
    ): Result<List<Restaurant>>

    /**
     * 여행 목적(휴양/관광/맛집/액티비티)에 맞는 관광지를 찾는다.
     * 목적은 condition.purposes에 들어 있다.
     */
    suspend fun searchAttractions(condition: TripCondition): Result<List<Attraction>>
}