package kr.ac.baekseok.readytogo.domain.purpose

import kr.ac.baekseok.readytogo.data.model.TravelPurpose

/**
 * 여행 목적별로 찾을 관광지 종류.
 *
 * 이 표는 두 군데서 쓴다. 한곳에 두어야 둘이 어긋나지 않는다.
 * 1. **검색**: Places에 이 타입들로 관광지를 요청한다.
 * 2. **점수**: 찾은 관광지의 타입이 이 목록과 겹치면 목적 일치 가산점을 준다.
 *
 * 타입 이름은 Places의 place type 표를 따른다. 없는 이름을 보내면 요청 자체가 거부되므로
 * 문서에 있는 것만 적는다. (온천 `hot_spring`은 존재하지 않아 `spa`로 대신한다.)
 */
object PurposeAttractionTypes {

    /** 목적을 고르지 않았을 때 찾을 종류. */
    val DEFAULT: List<String> = listOf("tourist_attraction", "museum", "park")

    private val BY_PURPOSE: Map<TravelPurpose, List<String>> = mapOf(
        TravelPurpose.REST to listOf("park", "spa", "beach", "garden", "buddhist_temple"),
        TravelPurpose.SIGHTSEEING to listOf("tourist_attraction", "museum", "historical_landmark"),
        TravelPurpose.FOOD to listOf("market", "tourist_attraction"),
        TravelPurpose.ACTIVITY to listOf(
            "amusement_park", "aquarium", "zoo", "water_park",
            "hiking_area", "ski_resort", "tourist_attraction",
        ),
    )

    /** 고른 목적들의 종류를 합친다. 고르지 않았으면 기본 종류. */
    fun typesFor(purposes: Set<TravelPurpose>): List<String> {
        if (purposes.isEmpty()) return DEFAULT
        return purposes.flatMap { BY_PURPOSE[it].orEmpty() }
            .distinct()
            .ifEmpty { DEFAULT }
    }

    /**
     * 이 장소가 고른 목적에 맞는지.
     *
     * 여러 목적을 골랐다면 그중 **어느 하나와만 맞아도** 일치로 본다.
     * 목적을 고르지 않았으면 비교할 기준이 없으므로 일치로 치지 않는다.
     */
    fun matches(placeTypes: List<String>, purposes: Set<TravelPurpose>): Boolean {
        if (purposes.isEmpty() || placeTypes.isEmpty()) return false
        val wanted = purposes.flatMap { BY_PURPOSE[it].orEmpty() }.toSet()
        return placeTypes.any { it in wanted }
    }
}
