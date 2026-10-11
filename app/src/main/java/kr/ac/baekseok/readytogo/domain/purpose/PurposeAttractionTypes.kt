package kr.ac.baekseok.readytogo.domain.purpose

import kr.ac.baekseok.readytogo.data.model.TravelPurpose

/**
 * 여행 목적별로 찾을 관광지 종류.
 *
 * 이 표는 두 군데서 쓴다. 한곳에 두어야 둘이 어긋나지 않는다.
 * 1. **검색**: Places에 이 타입들로 관광지를 요청한다([typesFor]).
 * 2. **일치 판정**: 찾은 관광지가 목적에 맞는지 본다. 가산점([matches])과 화면
 *    태그([matched])가 같이 쓴다. 여기서는 [GENERIC_TYPES]를 뺀 목록으로 본다.
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

    /**
     * **일치 판정에서 빼는 범용 타입.**
     *
     * Places는 관광지 대부분에 `tourist_attraction`을 같이 붙인다. 검색에서는 후보를
     * 넓히는 데 쓸모가 있지만 — 맛집 목적의 `market`만으로는 하루를 채울 곳이 모자란다 —
     * 목적이 맞는지 가리는 데 쓰면 아무 관광지나 전부 통과한다.
     *
     * 실제로 휴양+맛집 도쿄 일정에서 메이지 신궁·시부야 스크램블 교차로·시부야 스카이가
     * 모두 "맛집"으로 찍혔고, 점수에서도 저 세 곳이 맛집 가산점 0.15를 받고 있었다.
     */
    private val GENERIC_TYPES = setOf("tourist_attraction")

    /** 목적을 **가릴 수 있는** 타입만 남긴 표. 일치 판정은 이쪽을 본다. */
    private val DISTINCTIVE_BY_PURPOSE: Map<TravelPurpose, Set<String>> =
        BY_PURPOSE.mapValues { (_, types) -> types.toSet() - GENERIC_TYPES }

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
    fun matches(placeTypes: List<String>, purposes: Set<TravelPurpose>): Boolean =
        matched(placeTypes, purposes).isNotEmpty()

    /**
     * 이 장소가 **어느** 목적과 맞는지. 일정표의 목적 태그가 쓴다.
     *
     * [matches]가 점수 가산 여부(예/아니오)만 보는 것과 달리, 화면은 "왜 이게 들어왔나"를
     * 보여줘야 해서 목적 이름이 필요하다. 판정 기준은 같은 표를 쓰므로 둘이 어긋나지 않는다.
     *
     * 차례는 [TravelPurpose] 선언 순서로 고정한다. Set 순회 순서에 맡기면 같은 장소인데
     * 화면을 다시 열 때마다 태그 순서가 바뀐다.
     */
    fun matched(placeTypes: List<String>, purposes: Set<TravelPurpose>): List<TravelPurpose> {
        if (purposes.isEmpty() || placeTypes.isEmpty()) return emptyList()
        return TravelPurpose.entries.filter { purpose ->
            purpose in purposes &&
                placeTypes.any { it in DISTINCTIVE_BY_PURPOSE[purpose].orEmpty() }
        }
    }
}
