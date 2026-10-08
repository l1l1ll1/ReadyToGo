package kr.ac.baekseok.readytogo.data.model

/**
 * 즐겨찾기한 장소. 식당·관광지 카드의 ♥를 누르면 여기로 들어온다.
 *
 * **Restaurant·Attraction을 그대로 저장하지 않는 이유**: 두 모델은 가격·평점처럼
 * 시간이 지나면 틀려지는 값을 들고 있다. 즐겨찾기는 "이 장소를 기억해 둔다"는 뜻이므로
 * 장소를 다시 찾을 수 있는 최소한만 저장하고, 가격·평점은 열어볼 때 새로 받아온다.
 *
 * @param placeId Google Places의 장소 id. 2단계에서 이 값으로 최신 정보를 다시 받는다.
 */
data class FavoritePlace(
    val placeId: String,
    val name: String,
    /** "일본 음식점", "공원" 같은 Places 카테고리 표시명. 목록에서 종류를 구분하는 데 쓴다. */
    val category: String,
    val kind: FavoriteKind,
    val location: LatLng,
    /** 어느 도시에서 저장했는지. 목록을 도시별로 묶어 보여주는 데 쓴다. */
    val cityName: String,
    val mapUrl: String,
)

/** 즐겨찾기 종류. 목록을 식당·관광지로 나눠 보여주려면 구분이 필요하다. */
enum class FavoriteKind(val label: String) {
    RESTAURANT("식당"),
    ATTRACTION("관광지"),
    LODGING("숙소"),
}
