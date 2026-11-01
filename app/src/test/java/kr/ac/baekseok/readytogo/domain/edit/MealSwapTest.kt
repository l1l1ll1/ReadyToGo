package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.hotel
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.pair
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.restaurant
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.scored
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.timeline
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

/** 기준별로 "지금보다 나은 것 중 점수 최고"를 고르는지. 기준마다 고르는 경우 하나, 못 고르는 경우 하나. */
class MealSwapTest {

    private fun meal(time: MealTime) =
        timeline().map { it.item }.filterIsInstance<TimelineItem.Meal>().single { it.meal == time }

    private val lunch = meal(MealTime.LUNCH)
    private val dinner = meal(MealTime.DINNER)
    private val placeB = LatLng(35.2, 139.5)

    private fun pick(
        basis: MealBasis,
        target: TimelineItem.Meal,
        previous: LatLng? = placeB,
        lodging: LatLng? = hotel,
        taken: Set<String> = emptySet(),
    ) = pickMealFor(basis, target, previous, lodging, taken)?.item?.id

    @Test
    fun `숙소 근처 - 지금보다 숙소에 가까운 곳`() {
        assertEquals("din2", pick(MealBasis.NEAR_LODGING, dinner))
    }

    @Test
    fun `숙소 근처 - 숙소가 없으면 고르지 않는다`() {
        assertNull(pick(MealBasis.NEAR_LODGING, dinner, lodging = null))
    }

    @Test
    fun `바로 앞 근처 - 앞 일정에 더 가까운 곳`() {
        assertEquals("sushi", pick(MealBasis.NEAR_PREV, lunch))
    }

    @Test
    fun `바로 앞 근처 - 지금이 이미 가장 가까우면 없음`() {
        // 앞 일정이 라멘1 바로 옆이면 더 가까운 후보가 없다.
        assertNull(pick(MealBasis.NEAR_PREV, lunch, previous = LatLng(35.25, 139.5)))
    }

    @Test
    fun `더 싼 곳 - 싼 것 중 점수 최고`() {
        // 저녁 1등 din1(3만원)보다 싼 것은 din2(2만, 0.8)와 din3(1만, 0.6). 점수로 din2.
        assertEquals("din2", pick(MealBasis.CHEAPER, dinner))
    }

    @Test
    fun `더 싼 곳 - 지금 가격을 모르면 고르지 않는다`() {
        val unknown = restaurant("unknown", "라멘", 0, 35.25, priceUnknown = true)
        val meal = TimelineItem.Meal(MealTime.LUNCH, pair(scored(unknown, 0.9), scored(EditFixtures.ramen2, 0.8)))

        assertNull(pick(MealBasis.CHEAPER, meal))
    }

    @Test
    fun `다른 종류 - 라멘 말고`() {
        assertEquals("sushi", pick(MealBasis.EXCLUDE_SAME_KIND, lunch))
    }

    @Test
    fun `다른 종류 - 전부 같은 종류면 없음`() {
        assertNull(pick(MealBasis.EXCLUDE_SAME_KIND, dinner))
    }

    @Test
    fun `그날 다른 끼니에 나온 식당은 고르지 않는다`() {
        assertEquals("din3", pick(MealBasis.CHEAPER, dinner, taken = setOf("din2")))
    }

    @Test
    fun `방문 요일에 쉬는 식당은 고르지 않는다 - 모르는 곳은 남긴다`() {
        // 11/1(일). din2는 일요일 휴무라 빠지고, 영업시간을 모르는 din3이 남는다.
        val closedSunday = EditFixtures.din2.copy(openDays = DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY)
        val meal = TimelineItem.Meal(
            MealTime.DINNER,
            pair(scored(EditFixtures.din1, 0.9), scored(closedSunday, 0.8), scored(EditFixtures.din3, 0.6)),
        )

        val chosen = pickMealFor(MealBasis.CHEAPER, meal, placeB, hotel, emptySet(), DayOfWeek.SUNDAY)
        assertEquals("din3", chosen?.item?.id)
    }
}
