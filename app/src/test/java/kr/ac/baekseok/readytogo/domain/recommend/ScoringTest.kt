package kr.ac.baekseok.readytogo.domain.recommend

import kr.ac.baekseok.readytogo.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    private val shinjuku = LatLng(35.6896, 139.7006)
    private val tokyoStation = LatLng(35.6812, 139.7671)

    @Test
    fun `리뷰가 충분히 많으면 평점 점수는 만점 대비 비율에 수렴한다`() {
        // 리뷰 100만 개면 보정이 거의 사라진다.
        assertEquals(1.0, ratingScore(5.0, 1_000_000), 0.001)
        assertEquals(0.9, ratingScore(4.5, 1_000_000), 0.001)
    }

    @Test
    fun `리뷰가 적으면 평점이 전체 평균 쪽으로 끌려간다`() {
        // 리뷰 4개 / 5.0점 → (4/54)×5.0 + (50/54)×4.0 ≈ 4.074
        assertEquals(4.074, adjustedRating(5.0, 4), 0.001)
        // 리뷰가 0개면 온전히 전체 평균이다.
        assertEquals(PRIOR_RATING, adjustedRating(5.0, 0), 0.0001)
    }

    @Test
    fun `리뷰 많은 노포가 리뷰 적은 만점 가게를 이긴다`() {
        val newcomer = adjustedRating(5.0, 4)      // 리뷰 4개 만점
        val establishedShop = adjustedRating(4.8, 1_900) // 리뷰 1900개

        assertTrue(
            "보정 후에는 노포가 위여야 한다: $newcomer vs $establishedShop",
            establishedShop > newcomer,
        )
    }

    @Test
    fun `평점이 범위를 벗어나도 0과 1 사이로 잘린다`() {
        // 외부 API가 이상한 값을 주더라도 총점이 깨지지 않아야 한다.
        assertEquals(1.0, ratingScore(7.0, 1_000_000), 0.001)
        assertEquals(0.0, ratingScore(-1.0, 1_000_000), 0.001)
    }

    @Test
    fun `같은 지점의 거리는 0이다`() {
        assertEquals(0.0, distanceKm(shinjuku, shinjuku), 0.0001)
    }

    @Test
    fun `신주쿠에서 도쿄역까지는 대략 6km다`() {
        // 실제 직선거리 약 6.1km. 하버사인 근사가 크게 빗나가지 않는지 확인한다.
        val km = distanceKm(shinjuku, tokyoStation)
        assertTrue("예상 범위를 벗어남: $km km", km in 5.0..7.5)
    }

    @Test
    fun `기준점과 같은 자리면 동선 점수는 만점이다`() {
        assertEquals(1.0, routeScore(shinjuku, listOf(shinjuku)), 0.0001)
    }

    @Test
    fun `기준점에서 멀어질수록 동선 점수가 떨어지고 한계 거리에서 0이 된다`() {
        val near = routeScore(shinjuku, listOf(tokyoStation))
        assertTrue("가까우면 0보다 커야 한다: $near", near > 0.0)

        // 한참 떨어진 오사카를 기준으로 삼으면 0이어야 한다.
        val osaka = LatLng(34.6937, 135.5023)
        assertEquals(0.0, routeScore(shinjuku, listOf(osaka)), 0.0001)
    }

    @Test
    fun `중심점은 위도 경도의 중앙값이다`() {
        val center = medianCenter(
            listOf(
                LatLng(35.0, 139.0),
                LatLng(36.0, 140.0),
                LatLng(37.0, 141.0),
            )
        )!!

        assertEquals(36.0, center.latitude, 0.0001)
        assertEquals(140.0, center.longitude, 0.0001)
    }

    @Test
    fun `멀리 떨어진 한 곳이 중심을 끌고 가지 않는다`() {
        // 도쿄 일정에 오사카가 하나 끼어도 중심은 도쿄에 남아야 한다.
        // 평균을 쓰면 중심이 남서쪽으로 밀려 정작 도심 숙소가 손해를 본다.
        val tokyoPlan = listOf(shinjuku, LatLng(35.6580, 139.7016), LatLng(34.6937, 135.5023))
        val center = medianCenter(tokyoPlan)!!

        assertTrue("중심이 오사카 쪽으로 밀렸다: $center", center.longitude > 139.0)

        // 도쿄 후보가 오사카 후보를 확실히 이겨야 한다.
        val tokyoCandidate = routeScore(shinjuku, tokyoPlan)
        val osakaCandidate = routeScore(LatLng(34.6937, 135.5023), tokyoPlan)
        assertTrue("도쿄 후보 점수가 너무 낮다: $tokyoCandidate", tokyoCandidate > 0.5)
        assertEquals("오사카 후보는 동선 밖이다", 0.0, osakaCandidate, 0.0001)
    }

    @Test
    fun `기준 장소가 하나뿐이면 그 장소가 곧 중심이다`() {
        val center = medianCenter(listOf(tokyoStation))!!

        assertEquals(tokyoStation.latitude, center.latitude, 0.0001)
        assertEquals(tokyoStation.longitude, center.longitude, 0.0001)
    }
}