package kr.ac.baekseok.readytogo.data.repository.places

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FranchiseFilterTest {

    private fun excluded(name: String) = FranchiseFilter.isExcluded(name, isEatery = true)

    @Test
    fun `한글 표기로 걸러낸다`() {
        assertTrue(excluded("맥도날드"))
        assertTrue(excluded("스타벅스 시부야점"))
        assertTrue(excluded("이치란 신주쿠 가부키초점"))
    }

    @Test
    fun `영문 표기로 걸러낸다`() {
        assertTrue(excluded("McDonald's"))
        assertTrue(excluded("Starbucks Coffee"))
        assertTrue(excluded("SUBWAY"))
        assertTrue(excluded("Burger King Menteng"))
    }

    /**
     * 실제 도쿄 추천 결과에 올라왔던 이름들. 전부 한국에도 매장이 있어 갈 이유가 없다.
     * 결과 화면에서 눈으로 발견한 것이므로, 다시 새어 나오지 않게 이름 그대로 넣어 둔다.
     */
    @Test
    fun `도쿄 결과에서 발견한 체인을 걸러낸다`() {
        assertTrue(excluded("스시로 신주쿠 히가시구치점"))
        assertTrue(excluded("쿠라스시 니시신주쿠점"))
        assertTrue(excluded("Kura Sushi Global Flagship Store Harajuku"))
        assertTrue(excluded("하브스"))
        assertTrue(excluded("하드 락 카페 도쿄"))
        assertTrue(excluded("Haidilao Shinjuku Branch"))
    }

    @Test
    fun `현지어 표기로 걸러낸다`() {
        assertTrue(excluded("マクドナルド 新宿店"))
        assertTrue(excluded("吉野家"))
    }

    @Test
    fun `대소문자와 구두점이 달라도 걸러낸다`() {
        assertTrue(excluded("mcdonalds"))
        assertTrue(excluded("McDonald’s – Shibuya"))
        assertTrue(excluded("PIZZA HUT"))
    }

    @Test
    fun `식당이 아니면 적용하지 않는다`() {
        // "Hotel Domino" 같은 무관한 이름이 걸리지 않아야 한다.
        assertFalse(FranchiseFilter.isExcluded("Hotel Domino", isEatery = false))
        assertFalse(FranchiseFilter.isExcluded("맥도날드 빌딩", isEatery = false))
    }

    @Test
    fun `다른 단어 속에 우연히 들어간 경우는 걸러내지 않는다`() {
        // subway(지하철)가 이름에 들어간 식당. 단어 경계를 보지 않으면 잘못 걸린다.
        assertFalse(excluded("Subwayo Diner"))
        assertFalse(excluded("Kingsubway Kitchen"))
    }

    @Test
    fun `프랜차이즈가 아닌 곳은 통과한다`() {
        listOf(
            "아사쿠사 소바 노포",
            "Le Relais de l'Entrecôte",
            "Brasserie des Prés",
            "Sate Taichan BANG YOYO",
            "후글렌 도쿄 시부야",
        ).forEach { name ->
            assertFalse("$name 이(가) 잘못 걸렀다", excluded(name))
        }
    }

    @Test
    fun `예외 목록은 지금 비어 있다`() {
        // 실제로 걸러진 결과를 보고 필요한 것만 넣기로 했다.
        assertTrue(FranchiseFilter.ALLOWED_EXCEPTIONS.isEmpty())
    }
}
