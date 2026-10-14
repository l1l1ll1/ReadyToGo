package kr.ac.baekseok.readytogo.ui.map

import kr.ac.baekseok.readytogo.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 지도 동선 선 검증.
 *
 * 실기기에서 선이 아예 없거나 일부만 이어진 날이 있었다. 관광지만 잇고, 두 곳 미만이면
 * 그리지 않았기 때문이다. 숙소에서 나가 숙소로 돌아오는 한 줄이 되는지 숫자로 못 박는다.
 */
class DayRouteTest {

    private val hotel = LatLng(35.68, 139.76)
    private val a = LatLng(35.71, 139.79)
    private val b = LatLng(35.66, 139.70)
    private val c = LatLng(35.63, 139.88)

    @Test
    fun `숙소에서 출발해 일정표 순서대로 돌고 숙소로 돌아온다`() {
        assertEquals(listOf(hotel, a, b, c, hotel), dayRoute(hotel, listOf(a, b, c)))
    }

    @Test
    fun `좌표 없는 항목은 건너뛰고 다음 항목으로 잇는다`() {
        assertEquals(listOf(hotel, a, b, hotel), dayRoute(hotel, listOf(a, null, b)))
    }

    @Test
    fun `관광지 한 곳뿐인 날도 숙소 왕복 선이 생긴다`() {
        assertEquals(listOf(hotel, a, hotel), dayRoute(hotel, listOf(a)))
    }

    @Test
    fun `숙소가 없으면 장소끼리만 잇는다`() {
        assertEquals(listOf(a, b), dayRoute(null, listOf(a, b)))
    }

    @Test
    fun `장소가 하나도 없으면 선을 만들지 않는다`() {
        assertEquals(emptyList<LatLng>(), dayRoute(hotel, listOf(null)))
    }
}
