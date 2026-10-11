package kr.ac.baekseok.readytogo.data.model

/**
 * 사용자가 즐겨찾기에서 끼워 넣은 장소들. placeId로 찾는다.
 *
 * **편집(TimelineEdits)과 따로 둔다.** 편집에는 "어느 자리에 어느 id"만 적고, 그 id가
 * 가리키는 실제 장소는 여기 있다. 편집은 Firestore에 남지만 이건 앱이 떠 있는 동안만
 * 산다 — 가격·평점은 시간이 지나면 틀려지므로 다시 켤 때 새로 받는다.
 */
data class PickedPlaces(
    val restaurants: Map<String, Restaurant> = emptyMap(),
    val attractions: Map<String, Attraction> = emptyMap(),
)
