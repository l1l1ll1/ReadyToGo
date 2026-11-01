package kr.ac.baekseok.readytogo.domain.review

import kr.ac.baekseok.readytogo.data.model.AlternativeKind
import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.BudgetAllocation
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.edit.EditCommand
import kr.ac.baekseok.readytogo.domain.edit.summarizeDay
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 규칙 3개(하루 길이·같은 종류 연속·휴무)와 기본 수정.
 *
 * 4일 일정, 목적 없음(하루 3곳). 1일차(일요일)와 4일차는 1곳, 2·3일차는 3곳이다.
 * 관광지는 서로 1km 남짓 떨어져 있어 이동이 짧다.
 */
class PlanReviewTest {

    private val start = LocalDate.of(2026, 11, 1) // 일요일
    private val everyDay = DayOfWeek.entries.toSet()

    private fun attraction(id: String, lat: Double, openDays: Set<DayOfWeek>? = null) = Attraction(
        id = id, name = id, rating = 4.0, location = LatLng(lat, 139.7),
        stayMinutes = 90, address = "", openDays = openDays,
    )

    private fun restaurant(id: String, type: String?, openDays: Set<DayOfWeek>? = null, lat: Double = 35.6) =
        Restaurant(
            id = id, name = id, category = type ?: "음식점", rating = 4.0, reviewCount = 100,
            location = LatLng(lat, 139.7), priceWon = 10_000, address = "", mapUrl = "",
            suitableMeals = MealTime.entries.toSet(), openDays = openDays, primaryType = type,
        )

    private fun pair(vararg options: Restaurant) = RecommendationPair(
        qualityFirst = Recommendation(options.first(), 0.9, false),
        withinBudget = options.getOrNull(1)?.let { Recommendation(it, 0.8, false) },
        alternativeKind = if (options.size > 1) AlternativeKind.CHEAPER else AlternativeKind.NONE,
        alternatives = options.drop(2).map { Recommendation(it, 0.7, false) },
    )

    private fun day(number: Int, attractions: List<Attraction>, meals: Map<MealTime, RecommendationPair<Restaurant>>) =
        DayPlan(start.plusDays(number - 1L), number, attractions, meals)

    private fun meals(day: Int, lunch: String? = "restaurant", dinner: String? = "restaurant") = mapOf(
        MealTime.BREAKFAST to pair(restaurant("b$day", "cafe")),
        MealTime.LUNCH to pair(restaurant("l$day", lunch)),
        MealTime.DINNER to pair(restaurant("d$day", dinner)),
    )

    private fun plan(vararg days: DayPlan) = TripPlan(
        condition = TripCondition(departureDate = start),
        budget = BudgetAllocation(null, null, null, null, null, null, false),
        flights = emptyList(),
        lodging = null,
        dailyPlans = days.toList(),
    )

    private val day1 = day(1, listOf(attraction("a1", 35.60)), meals(1))
    private val day2 = day(2, listOf(attraction("b1", 35.61), attraction("b2", 35.62), attraction("b3", 35.63)), meals(2))
    private val day3 = day(3, listOf(attraction("c1", 35.61), attraction("c2", 35.62), attraction("c3", 35.63)), meals(3))
    private val day4 = day(4, listOf(attraction("e1", 35.60)), meals(4))
    private val basePlan = plan(day1, day2, day3, day4)

    // ---- 문제 없음 ----

    @Test
    fun `처음 만든 일정은 아무것도 걸리지 않는다`() {
        assertEquals(emptyList<ReviewIssue>(), reviewPlan(basePlan, TimelineEdits()))
    }

    // ---- (a) 하루 길이 ----

    @Test
    fun `먼 곳을 끼우면 그날이 꽉 찬 날의 최대치를 넘는다`() {
        // 2일차 b2 자리에 100km 넘게 떨어진 곳을 끼운다.
        val far = attraction("far", 36.7)
        val edits = TimelineEdits(pickedAttractions = mapOf(2 to mapOf("visit-b2" to "far")))
        val picked = PickedPlaces(attractions = mapOf("far" to far))

        val issues = reviewPlan(basePlan, edits, picked)

        val long = issues.single() as ReviewIssue.LongDay
        assertEquals(2, long.dayNumber)
        assertTrue(long.minutes > long.limitMinutes)
    }

    @Test
    fun `꽉 찬 날이 하나도 없으면 길이를 검사하지 않는다`() {
        // 이틀짜리 여행은 두 날 모두 1곳이라 기준이 없다.
        val far = attraction("far", 36.7)
        val shortPlan = plan(day1, day4)
        val edits = TimelineEdits(pickedAttractions = mapOf(1 to mapOf("visit-a1" to "far")))

        val issues = reviewPlan(shortPlan, edits, PickedPlaces(attractions = mapOf("far" to far)))

        assertTrue(issues.none { it is ReviewIssue.LongDay })
    }

    @Test
    fun `긴 날의 기본 수정은 빼면 가장 짧아지는 관광지부터`() {
        val far = attraction("far", 36.7)
        val edits = TimelineEdits(pickedAttractions = mapOf(2 to mapOf("visit-b2" to "far")))
        val picked = PickedPlaces(attractions = mapOf("far" to far))
        val issue = reviewPlan(basePlan, edits, picked).single()
        val summary = summarizeDay(day2.editedTimeline(edits, picked), null)

        val first = defaultFixes(issue, day2, edits, picked, summary).first().single() as EditCommand.Remove

        assertEquals("visit-b2", summary.keyByAlias[first.id])
        // 실행하면 문제가 사라진다.
        val result = applyFirst(day2, edits, listOf(listOf(first)), summary.keyByAlias, null, picked)!!
        assertTrue(reviewPlan(basePlan, result.edits, picked).isEmpty())
    }

    // ---- (b) 같은 종류 연속 ----

    @Test
    fun `전날 저녁과 다음 날 점심이 같은 종류면 다음 날 점심을 잡는다`() {
        val p = plan(day1, day(2, day2.attractions, meals(2, dinner = "ramen_restaurant")),
            day(3, day3.attractions, meals(3, lunch = "ramen_restaurant")), day4)

        val issue = reviewPlan(p, TimelineEdits()).single() as ReviewIssue.SameKind

        assertEquals(3, issue.dayNumber)
        assertEquals(MealTime.LUNCH, issue.meal)
        assertEquals(listOf(ReviewIssue.MealRef(2, MealTime.DINNER, "d2")), issue.neighbors)
    }

    @Test
    fun `셋이 이어지면 가운데 한 건으로 묶고, 고치면 전부 풀린다`() {
        // 1일차 저녁 → 2일차 점심 → 2일차 저녁이 전부 라멘. 원인은 가운데 2일차 점심 하나다.
        val lunch2 = pair(restaurant("l2", "ramen_restaurant"), restaurant("l2b", "sushi_restaurant"))
        val d2 = day(2, day2.attractions, meals(2, dinner = "ramen_restaurant") + (MealTime.LUNCH to lunch2))
        val p = plan(day(1, day1.attractions, meals(1, dinner = "ramen_restaurant")), d2, day3, day4)

        val issue = reviewPlan(p, TimelineEdits()).single() as ReviewIssue.SameKind

        assertEquals(2, issue.dayNumber)
        assertEquals(MealTime.LUNCH, issue.meal)
        assertEquals(
            listOf(ReviewIssue.MealRef(1, MealTime.DINNER, "d1"), ReviewIssue.MealRef(2, MealTime.DINNER, "d2")),
            issue.neighbors,
        )

        val summary = summarizeDay(d2.editedTimeline(TimelineEdits(), PickedPlaces()), null)
        val fixes = defaultFixes(issue, d2, TimelineEdits(), PickedPlaces(), summary)
        val result = applyFirst(d2, TimelineEdits(), fixes, summary.keyByAlias, null, PickedPlaces())!!
        assertTrue(reviewPlan(p, result.edits).isEmpty())
    }

    @Test
    fun `넷이 이어져도 묶음 하나라 한 건이다`() {
        val p = plan(
            day(1, day1.attractions, meals(1, dinner = "ramen_restaurant")),
            day(2, day2.attractions, meals(2, "ramen_restaurant", "ramen_restaurant")),
            day(3, day3.attractions, meals(3, lunch = "ramen_restaurant")),
            day4,
        )

        assertEquals(1, reviewPlan(p, TimelineEdits()).size)
    }

    @Test
    fun `같은 날 점심과 저녁이 같은 종류면 저녁을 잡는다`() {
        val p = plan(day1, day(2, day2.attractions, meals(2, "sushi_restaurant", "sushi_restaurant")), day3, day4)

        val issue = reviewPlan(p, TimelineEdits()).single() as ReviewIssue.SameKind

        assertEquals(MealTime.DINNER, issue.meal)
    }

    @Test
    fun `범용 타입끼리, 아침끼리, 모르는 타입은 잡지 않는다`() {
        // meals()의 기본값: 아침은 매일 cafe, 점심·저녁은 restaurant.
        val unknown = day(2, day2.attractions, meals(2, null, null))
        assertTrue(reviewPlan(plan(day1, unknown, day3, day4), TimelineEdits()).isEmpty())
    }

    @Test
    fun `추천을 못 찾은 끼니가 끼면 사슬이 끊긴다`() {
        val noLunch = meals(3, lunch = "ramen_restaurant", dinner = "ramen_restaurant") - MealTime.LUNCH
        val p = plan(day1, day(2, day2.attractions, meals(2, dinner = "ramen_restaurant")),
            day(3, day3.attractions, noLunch), day4)

        // 2일차 저녁 라멘 → 3일차 점심 없음 → 3일차 저녁 라멘: 이웃이 아니다.
        assertTrue(reviewPlan(p, TimelineEdits()).isEmpty())
    }

    @Test
    fun `같은 종류의 기본 수정은 다른 종류로 바꾸기`() {
        val lunch3 = pair(restaurant("l3", "ramen_restaurant"), restaurant("l3b", "sushi_restaurant"))
        val p = plan(day1, day(2, day2.attractions, meals(2, dinner = "ramen_restaurant")),
            day(3, day3.attractions, meals(3) + (MealTime.LUNCH to lunch3)), day4)
        val issue = reviewPlan(p, TimelineEdits()).single()
        val target = p.dailyPlans[2]
        val summary = summarizeDay(target.editedTimeline(TimelineEdits(), PickedPlaces()), null)

        val fixes = defaultFixes(issue, target, TimelineEdits(), PickedPlaces(), summary)
        val result = applyFirst(target, TimelineEdits(), fixes, summary.keyByAlias, null, PickedPlaces())!!

        assertEquals(mapOf("meal-LUNCH" to "l3b"), result.edits.pickedIn(3))
        assertTrue(reviewPlan(p, result.edits).isEmpty())
    }

    // ---- (c) 휴무 ----

    @Test
    fun `방문 요일에 쉬는 관광지를 잡는다 - 모르면 잡지 않는다`() {
        // 2일차는 월요일. b1은 월요일 휴무, b2는 영업시간 모름.
        val closedMonday = everyDay - DayOfWeek.MONDAY
        val d2 = day(2, listOf(attraction("b1", 35.61, closedMonday), attraction("b2", 35.62), attraction("b3", 35.63, everyDay)), meals(2))

        val issue = reviewPlan(plan(day1, d2, day3, day4), TimelineEdits()).single() as ReviewIssue.Closed

        assertEquals("visit-b1", issue.itemKey)
        assertEquals(null, issue.meal)
    }

    @Test
    fun `이미 뺀 곳에는 경고하지 않는다`() {
        val d2 = day(2, listOf(attraction("b1", 35.61, everyDay - DayOfWeek.MONDAY), attraction("b2", 35.62), attraction("b3", 35.63)), meals(2))
        val edits = TimelineEdits(deleted = mapOf(2 to setOf("visit-b1")))

        assertTrue(reviewPlan(plan(day1, d2, day3, day4), edits).isEmpty())
    }

    @Test
    fun `쉬는 식당의 기본 수정은 연 곳으로 바꾼다`() {
        // 1일차(일요일) 점심 1등이 일요일 휴무, 2등은 연다.
        val lunch = pair(restaurant("closed", "restaurant", everyDay - DayOfWeek.SUNDAY), restaurant("open", "restaurant", everyDay, lat = 35.55))
        val d1 = day(1, day1.attractions, meals(1) + (MealTime.LUNCH to lunch))
        val p = plan(d1, day2, day3, day4)
        val issue = reviewPlan(p, TimelineEdits()).single() as ReviewIssue.Closed
        assertEquals(MealTime.LUNCH, issue.meal)

        val summary = summarizeDay(d1.editedTimeline(TimelineEdits(), PickedPlaces()), LatLng(35.5, 139.7))
        val fixes = defaultFixes(issue, d1, TimelineEdits(), PickedPlaces(), summary)
        val result = applyFirst(d1, TimelineEdits(), fixes, summary.keyByAlias, LatLng(35.5, 139.7), PickedPlaces())!!

        assertEquals("open", result.edits.pickedIn(1)["meal-LUNCH"])
        assertTrue(reviewPlan(p, result.edits).isEmpty())
    }

    @Test
    fun `여러 문제는 날짜 순`() {
        val d2 = day(2, listOf(attraction("b1", 35.61, everyDay - DayOfWeek.MONDAY), attraction("b2", 35.62), attraction("b3", 35.63)), meals(2))
        val p = plan(day1, d2, day(3, day3.attractions, meals(3, "sushi_restaurant", "sushi_restaurant")), day4)

        assertEquals(listOf(2, 3), reviewPlan(p, TimelineEdits()).map { it.dayNumber })
    }

    @Test
    fun `NoMeal은 휴무 검사 대상이 아니다`() {
        val d1 = day(1, day1.attractions, meals(1) - MealTime.LUNCH)
        val entries = d1.editedTimeline(TimelineEdits(), PickedPlaces())
        assertTrue(entries.any { it.item is TimelineItem.NoMeal })
        assertTrue(reviewPlan(plan(d1, day2, day3, day4), TimelineEdits()).isEmpty())
    }
}
