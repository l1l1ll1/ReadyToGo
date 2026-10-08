package kr.ac.baekseok.readytogo.domain.recommend

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttractionScoringTest {

    private val center = LatLng(35.6896, 139.7006)

    private fun attraction(
        id: String,
        rating: Double = 4.5,
        reviewCount: Int = 5_000,
        location: LatLng = center,
        types: List<String> = emptyList(),
    ) = Attraction(
        id = id,
        name = id,
        rating = rating,
        location = location,
        stayMinutes = 90,
        address = "테스트",
        reviewCount = reviewCount,
        types = types,
    )

    @Test
    fun `목적에 맞는 장소는 가산점을 받는다`() {
        val park = attraction("park", types = listOf("park"))
        val museum = attraction("museum", types = listOf("museum"))

        val parkScore = attractionScore(park, listOf(center), setOf(TravelPurpose.REST))
        val museumScore = attractionScore(museum, listOf(center), setOf(TravelPurpose.REST))

        assertEquals(PURPOSE_MATCH_BONUS, parkScore - museumScore, 0.0001)
    }

    @Test
    fun `가산점은 가중 합산이 끝난 뒤에 더해진다`() {
        // 평점 만점 + 중심점에 위치 → 기본 1.0, 목적 일치까지면 1.15.
        val perfect = attraction("perfect", rating = 5.0, reviewCount = 1_000_000, types = listOf("park"))

        val score = attractionScore(perfect, listOf(center), setOf(TravelPurpose.REST))

        assertEquals(1.0 + PURPOSE_MATCH_BONUS, score, 0.001)
    }

    @Test
    fun `여러 목적 중 하나만 맞아도 가산점을 준다`() {
        val aquarium = attraction("aquarium", types = listOf("aquarium"))

        val score = attractionScore(
            aquarium,
            listOf(center),
            setOf(TravelPurpose.REST, TravelPurpose.ACTIVITY),
        )
        val without = attractionScore(aquarium, listOf(center), emptySet())

        assertEquals(PURPOSE_MATCH_BONUS, score - without, 0.0001)
    }

    @Test
    fun `목적을 고르지 않으면 가산점이 없다`() {
        val park = attraction("park", types = listOf("park"))
        val museum = attraction("museum", types = listOf("museum"))

        assertEquals(
            attractionScore(museum, listOf(center), emptySet()),
            attractionScore(park, listOf(center), emptySet()),
            0.0001,
        )
    }

    @Test
    fun `평점 차이가 크면 가산점으로 뒤집히지 않는다`() {
        // 가산점이 평점을 완전히 눌러버리면 목적만 맞으면 아무 곳이나 뽑힌다.
        val badButMatching = attraction("bad", rating = 3.0, types = listOf("park"))
        val greatButNot = attraction("great", rating = 4.9, types = listOf("museum"))

        val bad = attractionScore(badButMatching, listOf(center), setOf(TravelPurpose.REST))
        val great = attractionScore(greatButNot, listOf(center), setOf(TravelPurpose.REST))

        assertTrue("평점이 크게 높은 쪽이 이겨야 한다: $bad vs $great", great > bad)
    }

    @Test
    fun `동선이 나쁘면 점수가 내려간다`() {
        val near = attraction("near", location = center, types = listOf("park"))
        val far = attraction("far", location = LatLng(34.6937, 135.5023), types = listOf("park"))

        val nearScore = attractionScore(near, listOf(center), setOf(TravelPurpose.REST))
        val farScore = attractionScore(far, listOf(center), setOf(TravelPurpose.REST))

        assertTrue(nearScore > farScore)
    }
}
