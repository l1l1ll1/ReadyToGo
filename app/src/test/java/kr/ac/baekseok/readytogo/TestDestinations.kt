package kr.ac.baekseok.readytogo

import kr.ac.baekseok.readytogo.data.model.Destination
import kr.ac.baekseok.readytogo.data.model.LatLng

/**
 * 테스트에서 쓰는 목적지.
 *
 * 실제 목록은 assets에서 읽는데 단위 테스트에는 Android Context가 없다.
 * 테스트가 필요로 하는 건 좌표와 공항 코드뿐이라 여기에 고정값으로 둔다.
 */
object TestDestinations {
    val INCHEON = Destination("대한민국", "인천", LatLng(37.4602, 126.4407), listOf("ICN"))
    val TOKYO = Destination("일본", "도쿄", LatLng(35.6896, 139.7006), listOf("HND", "NRT"))
    val OSAKA = Destination("일본", "오사카", LatLng(34.6937, 135.5023), listOf("KIX", "ITM"))
}
