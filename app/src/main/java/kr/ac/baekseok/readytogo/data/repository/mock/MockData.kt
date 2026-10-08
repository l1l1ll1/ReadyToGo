package kr.ac.baekseok.readytogo.data.repository.mock

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm

/**
 * 도쿄 기준 가짜 데이터.
 *
 * 좌표는 실제 위치를 넣었다. 동선 점수가 거리 계산에서 나오기 때문에, 좌표가 엉터리면
 * 추천 결과가 뒤죽박죽이 되어 로직이 맞는지 눈으로 확인할 수 없다.
 * 가격·평점·리뷰 수는 그럴듯한 범위로 지어냈다. 7단계에서 이 파일만 실제 API로 교체된다.
 */
internal object MockData {

    // 주요 지역 좌표. 아래 데이터가 이 근처에 흩어지도록 배치했다.
    private val SHINJUKU = LatLng(35.6896, 139.7006)
    private val SHIBUYA = LatLng(35.6580, 139.7016)
    private val TOKYO_STATION = LatLng(35.6812, 139.7671)
    private val ASAKUSA = LatLng(35.7148, 139.7967)
    private val UENO = LatLng(35.7138, 139.7770)
    private val GINZA = LatLng(35.6721, 139.7636)

    /**
     * 주소를 만들 때 쓰는 지역 목록.
     *
     * 가게마다 주소를 일일이 적는 대신 좌표에서 가장 가까운 지역 이름을 붙인다.
     * 이러지 않으면 주소가 "도쿄 긴자 부티크 호텔"처럼 가게 이름과 겹쳐 보인다.
     */
    private val AREAS: List<Pair<String, LatLng>> = listOf(
        "신주쿠" to SHINJUKU,
        "시부야" to SHIBUYA,
        "도쿄역" to TOKYO_STATION,
        "아사쿠사" to ASAKUSA,
        "우에노" to UENO,
        "긴자" to GINZA,
        "하라주쿠" to LatLng(35.6702, 139.7027),
        "아키하바라" to LatLng(35.6984, 139.7731),
        "롯폰기" to LatLng(35.6627, 139.7311),
        "오다이바" to LatLng(35.6297, 139.7756),
        "이케부쿠로" to LatLng(35.7295, 139.7109),
        "스카이트리" to LatLng(35.7101, 139.8107),
        "도쿄타워" to LatLng(35.6586, 139.7454),
        "츠키지" to LatLng(35.6654, 139.7707),
    )

    private fun addressOf(location: LatLng): String =
        "도쿄 " + AREAS.minBy { distanceKm(location, it.second) }.first

    /**
     * 카테고리별로 어울리는 끼니.
     *
     * 실제 Places API를 붙이면 카테고리(cafe/bakery/restaurant)와 영업시간이 이 역할을 대신한다.
     * 해산물을 아침에도 넣은 이유는 츠키지 같은 시장 식당이 아침 장사를 하기 때문이다.
     *
     * 주의: 아래 restaurants 목록이 초기화될 때 이 값을 읽으므로 반드시 그보다 먼저 선언해야 한다.
     * (object의 프로퍼티는 위에서 아래로 초기화된다)
     */
    private val MEALS_BY_CATEGORY: Map<String, Set<MealTime>> = mapOf(
        // 카페·베이커리는 아침 전용으로 뒀다. 점심·저녁 후보와 겹치지 않아야
        // 한 끼가 다른 끼니의 후보를 먹어치워 뒷날 아침이 비는 일이 없다.
        "카페" to setOf(MealTime.BREAKFAST),
        "베이커리" to setOf(MealTime.BREAKFAST),
        // 츠키지 같은 시장 식당은 아침 장사를 한다.
        "해산물" to setOf(MealTime.BREAKFAST, MealTime.LUNCH, MealTime.DINNER),
        "소바" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "우동" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "라멘" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "카레" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "돈카츠" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "규카츠" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "텐동" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "양식" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "중식" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "스시" to setOf(MealTime.LUNCH, MealTime.DINNER),
        "뷔페" to setOf(MealTime.LUNCH, MealTime.DINNER),
        // 술을 곁들이는 곳은 저녁만.
        "야키니쿠" to setOf(MealTime.DINNER),
        "야키토리" to setOf(MealTime.DINNER),
        "이자카야" to setOf(MealTime.DINNER),
    )

    val attractions: List<Attraction> = listOf(
        Attraction("a1", "센소지", 4.5, ASAKUSA, 90, "도쿄 아사쿠사 2-3-1", "도쿄에서 가장 오래된 절"),
        Attraction("a2", "도쿄 스카이트리", 4.4, LatLng(35.7101, 139.8107), 120, "스미다구 오시아게", "634m 전망대"),
        Attraction("a3", "우에노 공원", 4.3, UENO, 90, "다이토구 우에노 공원", "박물관과 벚꽃 명소"),
        Attraction("a4", "아키하바라 전자상가", 4.2, LatLng(35.6984, 139.7731), 120, "지요다구 소토칸다", "애니·전자제품 거리"),
        Attraction("a5", "메이지 신궁", 4.6, LatLng(35.6764, 139.6993), 80, "시부야구 요요기카미조노초", "도심 속 숲과 신사"),
        Attraction("a6", "시부야 스크램블 교차로", 4.4, SHIBUYA, 40, "시부야구 도겐자카", "세계에서 가장 붐비는 교차로"),
        Attraction("a7", "하라주쿠 다케시타 거리", 4.1, LatLng(35.6702, 139.7027), 90, "시부야구 진구마에", "젊은 층 패션 거리"),
        Attraction("a8", "신주쿠 교엔", 4.5, LatLng(35.6852, 139.7100), 100, "신주쿠구 나이토마치", "넓은 정원"),
        Attraction("a9", "도쿄 타워", 4.3, LatLng(35.6586, 139.7454), 80, "미나토구 시바코엔", "333m 전망대"),
        Attraction("a10", "긴자 거리", 4.2, GINZA, 100, "주오구 긴자", "백화점과 명품 거리"),
        Attraction("a11", "츠키지 장외시장", 4.4, LatLng(35.6654, 139.7707), 80, "주오구 츠키지", "해산물 먹거리 시장"),
        Attraction("a12", "오다이바 해변공원", 4.2, LatLng(35.6297, 139.7756), 110, "미나토구 다이바", "레인보우브리지 야경"),
        Attraction("a13", "롯폰기 힐스 전망대", 4.3, LatLng(35.6627, 139.7311), 70, "미나토구 롯폰기", "도쿄 타워가 보이는 전망"),
        Attraction("a14", "이케부쿠로 선샤인시티", 4.0, LatLng(35.7295, 139.7109), 120, "도시마구 히가시이케부쿠로", "수족관과 쇼핑몰"),
        Attraction("a15", "도쿄역 마루노우치 역사", 4.3, TOKYO_STATION, 40, "지요다구 마루노우치", "붉은 벽돌 역사 건물"),
    )

    val lodgings: List<Lodging> = listOf(
        // 리뷰가 거의 없는 만점 숙소(l9)를 일부러 섞어뒀다.
        // 리뷰 수 보정(adjustedRating)이 이런 항목을 걸러내는지 확인하는 용도다.
        lodging("l1", "신주쿠 그랜드 호텔", 4.5, 1_240, SHINJUKU, 150_000),
        lodging("l2", "시부야 스테이 호텔", 4.3, 890, SHIBUYA, 120_000),
        lodging("l3", "도쿄역 프리미어", 4.7, 2_100, TOKYO_STATION, 260_000),
        lodging("l4", "아사쿠사 리버사이드", 4.2, 640, ASAKUSA, 85_000),
        lodging("l5", "우에노 비즈니스 인", 3.9, 1_450, UENO, 62_000),
        lodging("l6", "긴자 부티크 호텔", 4.6, 980, GINZA, 210_000),
        lodging("l7", "신주쿠 캡슐 스테이", 3.7, 3_200, LatLng(35.6938, 139.7036), 38_000),
        lodging("l8", "롯폰기 레지던스", 4.4, 520, LatLng(35.6627, 139.7311), 175_000),
        lodging("l9", "오다이바 베이 호텔", 5.0, 6, LatLng(35.6297, 139.7756), 98_000),
        lodging("l10", "이케부쿠로 시티 호텔", 4.0, 1_100, LatLng(35.7295, 139.7109), 72_000),
    )

    val restaurants: List<Restaurant> = listOf(
        restaurant("r1", "스시 잔마이 츠키지", "스시", 4.3, 5_600, LatLng(35.6654, 139.7707), 28_000),
        restaurant("r2", "이치란 신주쿠", "라멘", 4.2, 8_900, LatLng(35.6917, 139.7005), 13_000),
        restaurant("r3", "규카츠 모토무라 시부야", "규카츠", 4.4, 3_200, SHIBUYA, 19_000),
        restaurant("r4", "아사쿠사 텐동 다이코쿠야", "텐동", 4.1, 2_400, ASAKUSA, 17_000),
        restaurant("r5", "우에노 야키니쿠 정", "야키니쿠", 4.0, 1_150, UENO, 35_000),
        restaurant("r6", "긴자 큐베이", "스시", 4.8, 1_900, GINZA, 95_000),
        restaurant("r7", "신주쿠 오모이데요코초 꼬치집", "야키토리", 4.2, 2_700, SHINJUKU, 22_000),
        restaurant("r8", "도쿄역 라멘 스트리트", "라멘", 4.1, 4_300, TOKYO_STATION, 14_000),
        restaurant("r9", "하라주쿠 팬케이크 하우스", "카페", 4.0, 3_800, LatLng(35.6702, 139.7027), 16_000),
        restaurant("r10", "아키하바라 카레 본점", "카레", 3.9, 2_100, LatLng(35.6984, 139.7731), 11_000),
        restaurant("r11", "롯폰기 이자카야 하나", "이자카야", 4.3, 1_600, LatLng(35.6627, 139.7311), 31_000),
        restaurant("r12", "오다이바 시푸드 뷔페", "뷔페", 3.8, 2_900, LatLng(35.6297, 139.7756), 42_000),
        restaurant("r13", "시부야 소바 삼대", "소바", 4.5, 980, LatLng(35.6595, 139.6987), 15_000),
        restaurant("r14", "신주쿠 모닝 카페", "카페", 4.1, 1_400, LatLng(35.6880, 139.7040), 9_000),
        restaurant("r15", "츠키지 해산물 덮밥", "해산물", 4.6, 3_100, LatLng(35.6660, 139.7695), 24_000),
        restaurant("r16", "우에노 모닝 베이커리", "베이커리", 4.0, 760, LatLng(35.7120, 139.7760), 8_000),
        // 리뷰 4개짜리 만점 식당. 보정이 없으면 긴자 큐베이(4.8, 리뷰 1900)를 이겨버린다.
        restaurant("r17", "숨은 맛집 코지 다이닝", "이자카야", 5.0, 4, LatLng(35.6890, 139.7010), 26_000),
        restaurant("r18", "긴자 함박스테이크", "양식", 4.2, 2_200, GINZA, 23_000),
        restaurant("r19", "아사쿠사 모던 카페", "카페", 4.2, 1_300, LatLng(35.7130, 139.7950), 10_000),
        restaurant("r20", "이케부쿠로 돈카츠 마루", "돈카츠", 4.3, 1_800, LatLng(35.7295, 139.7109), 18_000),

        // 여행 전체에서 식당이 겹치지 않으려면 5일 기준 30곳이 필요하다(3끼 × 2선택지 × 5일).
        // 특히 아침은 지역별로 골고루 있어야 엉뚱하게 먼 카페가 추천되지 않는다.
        restaurant("r21", "도쿄역 커피 스탠드", "카페", 4.1, 2_050, TOKYO_STATION, 7_000),
        restaurant("r22", "긴자 모닝 토스트", "베이커리", 4.3, 1_600, GINZA, 12_000),
        restaurant("r23", "시부야 브런치 카페", "카페", 4.2, 2_400, LatLng(35.6605, 139.7000), 14_000),
        restaurant("r24", "아키하바라 모닝 베이커리", "베이커리", 4.0, 890, LatLng(35.6990, 139.7720), 6_500),
        restaurant("r25", "롯폰기 에스프레소 바", "카페", 4.4, 1_750, LatLng(35.6640, 139.7320), 11_000),
        restaurant("r26", "이케부쿠로 아침식당", "카페", 3.9, 1_120, LatLng(35.7280, 139.7120), 8_500),
        restaurant("r27", "스카이트리 뷰 카페", "카페", 4.3, 3_400, LatLng(35.7095, 139.8095), 13_000),
        restaurant("r28", "하라주쿠 크루아상", "베이커리", 4.5, 2_700, LatLng(35.6710, 139.7040), 9_500),
        restaurant("r29", "긴자 커피관", "카페", 4.6, 4_100, LatLng(35.6730, 139.7650), 12_000),
        restaurant("r30", "우에노 아침 카페", "카페", 4.0, 1_050, LatLng(35.7125, 139.7755), 7_500),

        restaurant("r31", "신주쿠 우동 사누키", "우동", 4.2, 2_300, LatLng(35.6905, 139.7020), 12_000),
        restaurant("r32", "시부야 중화요리 용", "중식", 4.1, 1_900, LatLng(35.6590, 139.7025), 21_000),
        restaurant("r33", "아사쿠사 소바 노포", "소바", 4.6, 3_600, LatLng(35.7140, 139.7955), 16_000),
        restaurant("r34", "우에노 라멘 아지", "라멘", 4.2, 2_800, LatLng(35.7130, 139.7765), 12_000),
        restaurant("r35", "긴자 스테이크 하우스", "양식", 4.5, 1_450, LatLng(35.6715, 139.7640), 46_000),
        restaurant("r36", "도쿄역 돈카츠 이치방", "돈카츠", 4.3, 3_050, LatLng(35.6805, 139.7665), 19_000),
        restaurant("r37", "오다이바 이탈리안 마레", "양식", 4.0, 1_250, LatLng(35.6300, 139.7750), 33_000),
        restaurant("r38", "스카이트리 회전초밥", "스시", 4.1, 4_600, LatLng(35.7105, 139.8100), 22_000),
        restaurant("r39", "아키하바라 이자카야 토리", "이자카야", 4.2, 1_700, LatLng(35.6980, 139.7735), 27_000),
        restaurant("r40", "롯폰기 야키니쿠 화", "야키니쿠", 4.4, 2_100, LatLng(35.6620, 139.7305), 44_000),
    )

    private fun lodging(
        id: String,
        name: String,
        rating: Double,
        reviewCount: Int,
        location: LatLng,
        priceWon: Long,
    ) = Lodging(
        id = id,
        name = name,
        rating = rating,
        reviewCount = reviewCount,
        location = location,
        priceWon = priceWon,
        address = addressOf(location),
        bookingUrl = "https://example.com/lodging/$id",
    )

    private fun restaurant(
        id: String,
        name: String,
        category: String,
        rating: Double,
        reviewCount: Int,
        location: LatLng,
        priceWon: Long,
    ) = Restaurant(
        id = id,
        name = name,
        category = category,
        rating = rating,
        reviewCount = reviewCount,
        location = location,
        priceWon = priceWon,
        address = addressOf(location),
        mapUrl = "https://example.com/place/$id",
        // 분류에 없는 카테고리가 생기면 점심·저녁으로 둔다. 아침에 엉뚱한 곳이 뜨는 쪽이 더 이상하다.
        suitableMeals = MEALS_BY_CATEGORY[category] ?: setOf(MealTime.LUNCH, MealTime.DINNER),
    )
}