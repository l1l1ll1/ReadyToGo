package kr.ac.baekseok.readytogo.domain.recommend

import kr.ac.baekseok.readytogo.data.model.AlternativeKind
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Restaurant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 추천 로직 검증.
 *
 * 외부 API 없이 도는 순수 함수라 Mock 데이터가 생기기 전에도 규칙을 못박아둘 수 있다.
 * 나중에 가중치를 조정할 때 이 테스트가 의도를 지켜준다.
 */
class RecommenderTest {

    // 도쿄 신주쿠역 부근을 기준점으로 잡는다.
    private val anchor = LatLng(35.6896, 139.7006)

    private fun restaurant(
        id: String,
        rating: Double,
        priceWon: Long,
        location: LatLng = anchor,
        // 기본값을 크게 둬서, 리뷰 수 보정이 끼어들지 않은 상태로 다른 규칙을 검증한다.
        reviewCount: Int = 1_000_000,
    ) = Restaurant(
        id = id,
        name = id,
        category = "테스트",
        rating = rating,
        reviewCount = reviewCount,
        location = location,
        priceWon = priceWon,
        address = "테스트 주소",
        mapUrl = "https://example.com/$id",
        // 끼니 필터는 TripPlanner의 책임이라 여기서는 모든 끼니를 허용해 둔다.
        suitableMeals = MealTime.entries.toSet(),
    )

    @Test
    fun `후보가 없으면 null을 돌려준다`() {
        val result = recommendPair(emptyList<Restaurant>(), listOf(anchor), 20_000)
        assertNull(result)
    }

    @Test
    fun `평점이 같으면 기준 장소에 가까운 쪽이 이긴다`() {
        val near = restaurant("near", rating = 4.0, priceWon = 10_000, location = anchor)
        // 기준점에서 약 5km 떨어진 지점 (위도 0.045도 ≈ 5km)
        val far = restaurant("far", rating = 4.0, priceWon = 10_000, location = LatLng(35.7346, 139.7006))

        val result = recommendPair(listOf(far, near), listOf(anchor), maxPriceWon = null)!!

        assertEquals("near", result.qualityFirst.item.id)
    }

    @Test
    fun `동선이 같으면 평점이 높은 쪽이 이긴다`() {
        val good = restaurant("good", rating = 4.8, priceWon = 30_000)
        val bad = restaurant("bad", rating = 3.0, priceWon = 30_000)

        val result = recommendPair(listOf(bad, good), listOf(anchor), maxPriceWon = null)!!

        assertEquals("good", result.qualityFirst.item.id)
    }

    @Test
    fun `품질 우선이 예산을 넘기면 예산 초과로 표시하고 예산 내 항목을 따로 준다`() {
        // 상한 20,000원의 2배(40,000원) 이내라 품질 우선 후보로 남는다.
        val expensive = restaurant("expensive", rating = 5.0, priceWon = 35_000)
        val cheap = restaurant("cheap", rating = 4.0, priceWon = 15_000)

        val result = recommendPair(listOf(expensive, cheap), listOf(anchor), maxPriceWon = 20_000)!!

        assertEquals("expensive", result.qualityFirst.item.id)
        assertTrue("예산을 넘겼으므로 표시가 붙어야 한다", result.qualityFirst.overBudget)
        assertEquals("cheap", result.withinBudget!!.item.id)
        assertTrue("예산 내 항목에는 표시가 없어야 한다", !result.withinBudget!!.overBudget)
    }

    @Test
    fun `품질 우선이 이미 예산 내면 예산 내 항목은 2등으로 채운다`() {
        val best = restaurant("best", rating = 5.0, priceWon = 10_000)
        val second = restaurant("second", rating = 4.0, priceWon = 12_000)

        val result = recommendPair(listOf(best, second), listOf(anchor), maxPriceWon = 20_000)!!

        assertEquals("best", result.qualityFirst.item.id)
        assertEquals("두 개가 같으면 안 된다", "second", result.withinBudget!!.item.id)
        assertNotEquals(result.qualityFirst.item.id, result.withinBudget!!.item.id)
    }

    @Test
    fun `품질 우선은 예산 상한의 2배를 넘는 항목을 고르지 않는다`() {
        // 상한 20,000원 → 품질 우선 후보는 40,000원 이하만.
        val luxury = restaurant("luxury", rating = 5.0, priceWon = 95_000)
        val reasonable = restaurant("reasonable", rating = 4.3, priceWon = 28_000)
        val cheap = restaurant("cheap", rating = 4.0, priceWon = 15_000)

        val result = recommendPair(
            listOf(luxury, reasonable, cheap), listOf(anchor), maxPriceWon = 20_000,
        )!!

        assertEquals("점수는 luxury가 최고지만 너무 비싸다", "reasonable", result.qualityFirst.item.id)
        assertEquals("cheap", result.withinBudget!!.item.id)
    }

    @Test
    fun `상한의 2배 안에 아무것도 없으면 제한 없이 점수 1등을 쓴다`() {
        // 한 칸을 비워두는 것보다는 비싸더라도 보여주는 편이 낫다.
        val a = restaurant("a", rating = 5.0, priceWon = 90_000)
        val b = restaurant("b", rating = 4.0, priceWon = 80_000)

        val result = recommendPair(listOf(a, b), listOf(anchor), maxPriceWon = 10_000)!!

        assertEquals("a", result.qualityFirst.item.id)
        assertTrue(result.qualityFirst.overBudget)
    }

    @Test
    fun `1등이 예산 안이면 두 번째는 더 싼 것 중 최고점이 온다`() {
        val best = restaurant("best", rating = 4.8, priceWon = 18_000)
        val cheaperGood = restaurant("cheaperGood", rating = 4.5, priceWon = 10_000)
        val cheaperBad = restaurant("cheaperBad", rating = 3.0, priceWon = 9_000)
        // 1등보다 비싼 항목은 가성비 대안이 될 수 없다.
        val pricier = restaurant("pricier", rating = 4.7, priceWon = 19_000)

        val result = recommendPair(
            listOf(best, cheaperGood, cheaperBad, pricier), listOf(anchor), maxPriceWon = 20_000,
        )!!

        assertEquals("best", result.qualityFirst.item.id)
        assertEquals("cheaperGood", result.withinBudget!!.item.id)
        assertEquals(AlternativeKind.CHEAPER, result.alternativeKind)
    }

    @Test
    fun `1등이 예산 안이고 더 싼 후보가 없으면 동선이 가장 좋은 곳을 준다`() {
        // 숙소처럼 등급이 같아 값이 똑같은 경우다. 가격으로 못 가르면 동선으로 가른다.
        val best = restaurant("best", rating = 4.8, priceWon = 10_000, location = anchor)
        val far = restaurant("far", rating = 4.7, priceWon = 10_000, location = LatLng(35.7346, 139.7006))
        val near = restaurant("near", rating = 4.0, priceWon = 10_000, location = LatLng(35.6900, 139.7010))

        val result = recommendPair(listOf(best, far, near), listOf(anchor), maxPriceWon = 20_000)!!

        assertEquals("best", result.qualityFirst.item.id)
        assertEquals("가장 가까운 곳이어야 한다", "near", result.withinBudget!!.item.id)
        assertEquals(AlternativeKind.CLOSEST, result.alternativeKind)
    }

    @Test
    fun `1등이 예산을 넘기면 두 번째는 예산 내 기준으로 뽑힌다`() {
        val expensive = restaurant("expensive", rating = 5.0, priceWon = 35_000)
        val cheap = restaurant("cheap", rating = 4.0, priceWon = 15_000)

        val result = recommendPair(listOf(expensive, cheap), listOf(anchor), maxPriceWon = 20_000)!!

        assertEquals(AlternativeKind.WITHIN_BUDGET, result.alternativeKind)
    }

    @Test
    fun `후보가 하나뿐이면 대안 없음으로 표시한다`() {
        val only = restaurant("only", rating = 4.0, priceWon = 10_000)

        val result = recommendPair(listOf(only), listOf(anchor), maxPriceWon = 20_000)!!

        assertEquals(AlternativeKind.NONE, result.alternativeKind)
    }

    @Test
    fun `일정 중심까지의 거리를 함께 돌려준다`() {
        val item = restaurant("x", rating = 4.0, priceWon = 10_000, location = anchor)

        val withAnchors = recommendPair(listOf(item), listOf(anchor), maxPriceWon = null)!!
        assertEquals(0.0, withAnchors.qualityFirst.distanceFromCenterKm!!, 0.01)

        // 기준 장소가 없으면 거리도 말할 수 없다.
        val withoutAnchors = recommendPair(listOf(item), emptyList(), maxPriceWon = null)!!
        assertNull(withoutAnchors.qualityFirst.distanceFromCenterKm)
    }

    @Test
    fun `예산 내 후보가 하나도 없으면 가장 싼 항목을 대신 보여준다`() {
        val a = restaurant("a", rating = 5.0, priceWon = 90_000)
        val b = restaurant("b", rating = 4.0, priceWon = 50_000)

        val result = recommendPair(listOf(a, b), listOf(anchor), maxPriceWon = 10_000)!!

        assertEquals("a", result.qualityFirst.item.id)
        assertEquals("b", result.withinBudget!!.item.id)
        assertTrue("둘 다 예산을 넘겼으므로 표시가 붙어야 한다", result.withinBudget!!.overBudget)
    }

    @Test
    fun `후보가 하나뿐이면 대안 칸을 비운다`() {
        // 같은 곳을 두 칸에 넣으면 선택지가 있는 것처럼 보여 오해를 준다.
        val only = restaurant("only", rating = 4.0, priceWon = 10_000)

        val result = recommendPair(listOf(only), listOf(anchor), maxPriceWon = 20_000)!!

        assertEquals("only", result.qualityFirst.item.id)
        assertNull(result.withinBudget)
        assertEquals(AlternativeKind.NONE, result.alternativeKind)
    }

    @Test
    fun `기준 장소가 없으면 동선 점수는 0이고 평점 점수만 남는다`() {
        val item = restaurant("x", rating = 5.0, priceWon = 10_000)

        val result = recommendPair(listOf(item), anchors = emptyList(), maxPriceWon = null)!!

        // 리뷰가 충분하므로 보정 없이 평점 5.0 → 평점 점수 1.0, 가중치 0.5 → 총점 0.5
        assertEquals(0.5, result.qualityFirst.score, 0.001)
    }

    @Test
    fun `리뷰가 적은 만점 식당은 리뷰 많은 식당에 밀린다`() {
        // 위치가 같으므로 동선 점수는 동일하고, 평점 점수만으로 순위가 갈린다.
        val newcomer = restaurant("newcomer", rating = 5.0, priceWon = 20_000, reviewCount = 4)
        val established = restaurant("established", rating = 4.5, priceWon = 20_000, reviewCount = 3_000)

        val result = recommendPair(listOf(newcomer, established), listOf(anchor), maxPriceWon = null)!!

        assertEquals("established", result.qualityFirst.item.id)
    }

    @Test
    fun `우회 기준이면 근거로 중심 거리 대신 우회 거리를 내보낸다`() {
        val next = LatLng(35.6812, 139.7671) // 도쿄역
        val item = restaurant("a", 4.0, 10_000)

        val result = recommendPair(listOf(item), RouteBasis.Detour(anchor, next), maxPriceWon = null)!!

        // 출발점과 같은 자리라 들러도 더 가는 거리가 없다.
        assertEquals(0.0, result.qualityFirst.detourKm!!, 0.01)
        assertNull(result.qualityFirst.distanceFromCenterKm)
    }

    @Test
    fun `평점이 같으면 다음 일정 방향에 있는 식당이 추천된다`() {
        val next = LatLng(35.6812, 139.7671) // 도쿄역
        val onTheWay = restaurant("onTheWay", 4.2, 10_000, location = LatLng(35.6868, 139.7336))
        val opposite = restaurant("opposite", 4.2, 10_000, location = LatLng(35.6924, 139.6676))

        val result = recommendPair(
            listOf(opposite, onTheWay),
            RouteBasis.Detour(anchor, next),
            maxPriceWon = null,
        )!!

        assertEquals("onTheWay", result.qualityFirst.item.id)
    }
}