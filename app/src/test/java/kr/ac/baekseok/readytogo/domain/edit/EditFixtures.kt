package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.AlternativeKind
import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import java.time.LocalDate

/**
 * 말로 고치기 테스트용 하루. 배치: 아침 a b 점심 c d 저녁.
 *
 * 숙소는 남쪽(35.0)에 있고 관광지는 북쪽으로 늘어서 있다. 저녁 추천 1등(din1)은
 * 숙소에서 멀고 2등(din2)은 숙소 바로 옆이다 — "숙소 근처로"가 무엇을 골라야 하는지 분명하다.
 */
object EditFixtures {
    val hotel = LatLng(35.0, 139.0)

    private fun attraction(id: String, lat: Double) = Attraction(
        id = id, name = id, rating = 4.0, location = LatLng(lat, 139.5),
        stayMinutes = 60, address = "",
    )

    fun restaurant(id: String, category: String, price: Long, lat: Double, priceUnknown: Boolean = false) =
        Restaurant(
            id = id, name = id, category = category, rating = 4.0, reviewCount = 100,
            location = LatLng(lat, 139.5), priceWon = price, priceUnknown = priceUnknown,
            address = "", mapUrl = "", suitableMeals = MealTime.entries.toSet(),
        )

    fun scored(restaurant: Restaurant, score: Double) =
        Recommendation(item = restaurant, score = score, overBudget = false)

    fun pair(vararg options: Recommendation<Restaurant>) = RecommendationPair(
        qualityFirst = options.first(),
        withinBudget = options.getOrNull(1),
        alternativeKind = if (options.size > 1) AlternativeKind.CHEAPER else AlternativeKind.NONE,
        alternatives = options.drop(2),
    )

    val cafe = restaurant("cafe", "카페", 8_000, 35.0)

    val ramen1 = restaurant("ramen1", "라멘", 15_000, 35.25)
    val ramen2 = restaurant("ramen2", "라멘", 12_000, 35.6)
    // b(35.2)에 ramen1보다 가깝다 — "바로 앞 근처로"의 정답.
    val sushi = restaurant("sushi", "스시", 30_000, 35.21)

    val din1 = restaurant("din1", "이자카야", 30_000, 35.5)
    val din2 = restaurant("din2", "이자카야", 20_000, 35.01)
    val din3 = restaurant("din3", "이자카야", 10_000, 35.45)

    val day = DayPlan(
        date = LocalDate.of(2026, 11, 1),
        dayNumber = 2,
        attractions = listOf(
            attraction("a", 35.1),
            attraction("b", 35.2),
            attraction("c", 35.3),
            attraction("d", 35.4),
        ),
        meals = mapOf(
            // 아침은 후보가 하나뿐이다 — "다른 것으로"가 거부되어야 한다.
            MealTime.BREAKFAST to pair(scored(cafe, 0.8)),
            MealTime.LUNCH to pair(scored(ramen1, 0.9), scored(ramen2, 0.8), scored(sushi, 0.7)),
            MealTime.DINNER to pair(scored(din1, 0.9), scored(din2, 0.8), scored(din3, 0.6)),
        ),
    )

    fun timeline(edits: TimelineEdits = TimelineEdits()) = day.toTimeline(
        deletedKeys = edits.deletedIn(day.dayNumber),
        pickedMeals = edits.pickedIn(day.dayNumber),
        customOrder = edits.orderIn(day.dayNumber),
    )
}
