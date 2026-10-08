package kr.ac.baekseok.readytogo.ui.map

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 겹치는 마커 펼치기 검증.
 *
 * 눈으로만 확인하면 "지금 화면에서는 괜찮아 보인다"로 끝난다. 실제로 우에노에서 마커 하나가
 * 완전히 가려졌던 것을 숫자로 못 박아 둔다.
 */
class MarkerSpreadTest {

    /** 도쿄역 근처. 위도 35도에서 경도 보정이 제대로 되는지도 같이 본다. */
    private val tokyo = LatLng(35.6812, 139.7671)

    private fun spot(name: String, position: LatLng) = MapSpot(
        position = position,
        title = name,
        snippet = "",
        kind = SpotKind.RESTAURANT,
    )

    /** 북쪽으로 km만큼 떨어진 좌표. 위도 1도는 어디서나 약 111.32km다. */
    private fun LatLng.north(km: Double) = LatLng(latitude + km / 111.32, longitude)

    @Test
    fun `멀리 떨어진 마커는 움직이지 않는다`() {
        val spots = listOf(
            spot("A", tokyo),
            spot("B", tokyo.north(5.0)),
            spot("C", tokyo.north(10.0)),
        )

        spots.spreadOverlapping().forEach { moved ->
            val original = spots.first { it.title == moved.title }
            assertEquals(
                "${moved.title}이(가) 움직였다",
                original.position,
                moved.displayPosition,
            )
        }
    }

    @Test
    fun `같은 자리에 겹친 마커는 서로 떨어진다`() {
        // 세 곳이 사실상 한 지점에 있고, 멀리 하나를 둬서 일정 폭을 만든다.
        val spots = listOf(
            spot("겹침1", tokyo),
            spot("겹침2", tokyo),
            spot("겹침3", tokyo),
            spot("먼곳", tokyo.north(8.0)),
        )

        val spread = spots.spreadOverlapping()
        val overlapping = spread.filter { it.title.startsWith("겹침") }

        // 셋이 서로 다른 자리에 놓여야 한다. 하나라도 겹치면 가려진다.
        val positions = overlapping.map { it.displayPosition }
        assertEquals("펼친 뒤에도 같은 좌표가 남았다", 3, positions.distinct().size)

        // 눈으로 구분되려면 최소한의 간격이 있어야 한다.
        overlapping.forEachIndexed { i, a ->
            overlapping.drop(i + 1).forEach { b ->
                val gap = distanceKm(a.displayPosition, b.displayPosition)
                assertTrue("${a.title}와 ${b.title}가 ${gap}km로 너무 가깝다", gap > 0.02)
            }
        }
    }

    @Test
    fun `펼쳐도 원래 좌표는 그대로 남는다`() {
        // 동선 선과 카메라 범위가 원래 좌표를 써야 하므로 position은 바뀌면 안 된다.
        val spots = listOf(spot("A", tokyo), spot("B", tokyo), spot("멀리", tokyo.north(8.0)))

        spots.spreadOverlapping().forEach { moved ->
            val original = spots.first { it.title == moved.title }
            assertEquals(original.position, moved.position)
        }
    }

    @Test
    fun `마커가 하나뿐이면 그대로 둔다`() {
        val only = listOf(spot("혼자", tokyo))
        assertEquals(only, only.spreadOverlapping())
    }

    @Test
    fun `펼친 거리는 일정 폭에 비해 작다`() {
        // 마커를 실제 위치에서 옮기는 것이므로 오차가 눈에 띄면 안 된다.
        val spanKm = 8.0
        val spots = listOf(spot("A", tokyo), spot("B", tokyo), spot("멀리", tokyo.north(spanKm)))

        spots.spreadOverlapping().forEach { moved ->
            val shift = distanceKm(moved.position, moved.displayPosition)
            assertTrue("${moved.title}이(가) ${shift}km나 밀렸다", shift < spanKm * 0.05)
        }
    }
}
