package kr.ac.baekseok.readytogo.data.model

import java.time.DayOfWeek

/**
 * 점수 기반 추천(평점 + 동선)의 대상이 갖춰야 할 최소 정보.
 *
 * 숙소와 식당은 화면에 보여줄 내용이 서로 다르지만 추천 규칙은 완전히 같다(CLAUDE.md).
 * 그래서 공통분모만 인터페이스로 뽑아, 추천 함수 하나를 둘 다에 쓴다.
 */
interface ScoredCandidate {
    /** Google 평점. 0.0 ~ 5.0 */
    val rating: Double

    /**
     * 리뷰 수. 평점을 얼마나 믿을지 정하는 데 쓴다.
     * 리뷰 3개짜리 5.0점이 리뷰 2000개짜리 4.5점을 이기지 않도록 보정하는 근거다.
     */
    val reviewCount: Int

    val location: LatLng

    /** 비교 기준이 되는 가격(원). 숙소는 1박, 식당은 1인 1끼. */
    val priceWon: Long
}

/** 숙소. priceWon은 1박 기준이다. */
data class Lodging(
    val id: String,
    val name: String,
    override val rating: Double,
    override val reviewCount: Int,
    override val location: LatLng,
    override val priceWon: Long,
    val address: String,
    val bookingUrl: String,
    val imageUrl: String? = null,
    /** Places 가격 등급(0~4). 요금을 안 주므로 이것만으로 1박가를 추정한다. */
    val priceLevel: Int? = null,
    /**
     * 가격이 추정값이면 true. Places로 찾은 숙소는 요금 정보가 없어 항상 추정이다.
     * 화면에 "예상"을 붙여 사용자가 확정 금액으로 오해하지 않게 한다.
     */
    val priceEstimated: Boolean = false,
) : ScoredCandidate

/** 식당. priceWon은 1인 1끼 평균 가격이다. */
data class Restaurant(
    val id: String,
    val name: String,
    /** 예: 스시, 라멘, 카페 */
    val category: String,
    override val rating: Double,
    override val reviewCount: Int,
    override val location: LatLng,
    /**
     * 예산 비교 기준이 되는 1인 1끼 금액. **가격대가 있으면 그 상한**을 쓴다.
     *
     * 하한을 쓰면 "예산 내"로 추천해놓고 현지에서 돈이 모자랄 수 있다.
     * 예산이 남는 쪽이 모자란 쪽보다 낫다.
     */
    override val priceWon: Long,
    /** 가격대 하한. null이면 범위 정보 없이 등급에서 추정한 값이다. */
    val minPriceWon: Long? = null,
    /** 현지 통화 표기. 예: "€17~€28". 메뉴판과 바로 대조할 수 있게 같이 보여준다. */
    val localPriceText: String? = null,
    /** 가격대 정보가 없어 등급으로 추정했으면 true. 화면에 "추정"을 붙인다. */
    val priceEstimated: Boolean = false,
    /**
     * 하한 없이 "이 금액 이하"로만 말할 수 있으면 true.
     * Google이 저가 구간을 "¥1~¥1,000"처럼 표기해서, 그대로 두면 "8원부터"가 되어버린다.
     */
    val priceIsUpperBound: Boolean = false,
    /**
     * 가격을 알 수 없는 경우.
     *
     * 즐겨찾기를 일정에 끼워 넣을 때 그 장소만 따로 받아 오는데, Places가 가격대를
     * 주지 않거나 조회가 실패할 수 있다. 0원으로 두면 "공짜"로 읽히므로 따로 표시한다.
     */
    val priceUnknown: Boolean = false,
    val address: String,
    val mapUrl: String,
    /**
     * 이 식당이 어울리는 끼니.
     *
     * 이 정보가 없으면 아침에 9만원짜리 스시 오마카세가, 저녁에 카페가 추천된다.
     * 판단은 데이터 출처(Repository)가 한다 — 7단계에서는 Places API의 카테고리와 영업시간으로
     * 채우면 되고, 추천 로직은 이 집합만 보면 된다.
     */
    val suitableMeals: Set<MealTime>,
    /**
     * 문을 여는 요일. null이면 **모른다**(Places가 안 줬거나, 영업시간을 받기 전의 캐시).
     * 일정 검토관이 "방문일에 휴무"를 찾는 데 쓴다 — 모르는 것을 휴무로 단정하지 않으려고
     * 빈 집합이 아니라 null로 둔다.
     */
    val openDays: Set<DayOfWeek>? = null,
) : ScoredCandidate

/**
 * 관광지.
 *
 * 추천 점수를 매기지 않으므로 ScoredCandidate가 아니다.
 * 대신 하루 일정의 뼈대가 되고, 그날 숙소·식당의 **동선 점수를 재는 기준점** 역할을 한다.
 */
data class Attraction(
    val id: String,
    val name: String,
    val rating: Double,
    val location: LatLng,
    /** 머무는 시간(분). 일자별 일정표에서 시간 배치에 쓴다. */
    val stayMinutes: Int,
    val address: String,
    val description: String = "",
    /** 리뷰 수. 식당·숙소와 같은 방식으로 평점을 보정하는 데 쓴다. */
    val reviewCount: Int = 0,
    /**
     * Places가 준 장소 타입(park, museum…).
     * 고른 여행 목적과 맞는지 판단해 점수에 가산점을 주는 데 쓴다.
     */
    val types: List<String> = emptyList(),
    /**
     * 지도 앱으로 열 링크. 상세 시트의 "지도에서 열기"가 쓴다.
     *
     * Mock 데이터에는 없어서 기본값을 비워 둔다 — 비어 있으면 화면이 버튼을 숨긴다.
     * 새 필드를 **맨 뒤에** 두는 이유: MockData가 Attraction을 위치 인자로 만들기 때문에
     * 중간에 끼우면 기존 호출이 전부 어긋난다. 실제로 한 번 겪었다.
     */
    val mapUrl: String = "",
    /** 문을 여는 요일. null이면 모른다([Restaurant.openDays]와 같은 규칙). */
    val openDays: Set<DayOfWeek>? = null,
)

/** 영업 요일을 알고 [day]가 빠져 있으면 휴무. 모르면 휴무가 아니다(검사하지 않는다). */
fun Set<DayOfWeek>?.isClosedOn(day: DayOfWeek): Boolean = this != null && day !in this