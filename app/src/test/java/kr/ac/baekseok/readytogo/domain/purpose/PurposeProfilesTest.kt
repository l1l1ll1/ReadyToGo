package kr.ac.baekseok.readytogo.domain.purpose

import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import org.junit.Assert.assertEquals
import org.junit.Test

class PurposeProfilesTest {

    @Test
    fun `목적을 고르지 않으면 기본값을 쓴다`() {
        assertEquals(PurposeProfiles.DEFAULT, PurposeProfiles.of(emptySet()))
    }

    @Test
    fun `휴양은 숙박 비중이 높고 일정이 느슨하다`() {
        val profile = PurposeProfiles.of(setOf(TravelPurpose.REST))

        assertEquals(0.55, profile.lodgingShare, 0.0001)
        assertEquals(0.25, profile.foodShare, 0.0001)
        assertEquals(0.20, profile.otherShare, 0.0001)
        assertEquals(2, profile.attractionsPerDay)
    }

    @Test
    fun `맛집은 식비 비중이 가장 높다`() {
        val profile = PurposeProfiles.of(setOf(TravelPurpose.FOOD))

        assertEquals(0.45, profile.foodShare, 0.0001)
        assertEquals(3, profile.attractionsPerDay)
    }

    @Test
    fun `관광은 하루에 네 곳을 돈다`() {
        val profile = PurposeProfiles.of(setOf(TravelPurpose.SIGHTSEEING))

        assertEquals(0.35, profile.otherShare, 0.0001)
        assertEquals(4, profile.attractionsPerDay)
    }

    @Test
    fun `여러 개를 고르면 평균을 낸다`() {
        // 관광(0.40/0.25/0.35/4) + 휴양(0.55/0.25/0.20/2)
        val profile = PurposeProfiles.of(setOf(TravelPurpose.SIGHTSEEING, TravelPurpose.REST))

        assertEquals(0.475, profile.lodgingShare, 0.0001)
        assertEquals(0.25, profile.foodShare, 0.0001)
        assertEquals(0.275, profile.otherShare, 0.0001)
        assertEquals("평균 3곳", 3, profile.attractionsPerDay)
    }

    @Test
    fun `관광지 수는 반올림한다`() {
        // 휴양(2) + 맛집(3) → 2.5 → 3
        val profile = PurposeProfiles.of(setOf(TravelPurpose.REST, TravelPurpose.FOOD))

        assertEquals(3, profile.attractionsPerDay)
    }

    @Test
    fun `어떤 조합이든 비율의 합은 1이다`() {
        val all = TravelPurpose.entries
        val combinations = listOf(emptySet<TravelPurpose>()) +
            all.map { setOf(it) } +
            all.flatMap { a -> all.map { b -> setOf(a, b) } } +
            listOf(all.toSet())

        combinations.forEach { purposes ->
            val p = PurposeProfiles.of(purposes)
            val sum = p.lodgingShare + p.foodShare + p.otherShare
            assertEquals("$purposes 의 합이 1이 아니다", 1.0, sum, 0.0001)
        }
    }

    @Test
    fun `모든 조합에서 하루 관광지 수가 1 이상이다`() {
        TravelPurpose.entries.forEach { purpose ->
            val count = PurposeProfiles.of(setOf(purpose)).attractionsPerDay
            assertEquals("$purpose 가 0곳이 되면 일정이 빈다", true, count >= 1)
        }
    }
}
