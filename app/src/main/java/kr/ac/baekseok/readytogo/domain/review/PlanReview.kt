package kr.ac.baekseok.readytogo.domain.review

import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.data.model.isClosedOn
import kr.ac.baekseok.readytogo.domain.plan.TimelineEntry
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.domain.purpose.PurposeProfiles
import kr.ac.baekseok.readytogo.domain.purpose.IndoorOutdoor
import kr.ac.baekseok.readytogo.domain.purpose.Shelter
import kr.ac.baekseok.readytogo.domain.weather.DailyWeather
import kr.ac.baekseok.readytogo.domain.weather.RAINY_PROBABILITY
import java.time.LocalDate

/**
 * 일정 검토관(⑨-2)이 찾은 문제 하나.
 *
 * **문제는 규칙(이 파일)이 찾는다.** AI는 이 목록을 받아 설명과 고칠 명령만 낸다 —
 * 모델이 일정을 훑어 문제를 찾게 하면 없는 휴무일을 지어낼 수 있다.
 */
sealed interface ReviewIssue {
    /**
     * 편집 뒤 다시 검사해도 같은 문제면 같은 값. 화면 목록의 키이자 서버와 주고받는 이름이다.
     * AI 설명은 id가 아니라 문제 전체(data class 동등성)로 재사용한다 — 내용이 바뀌면 다시 묻는다.
     */
    val id: String
    val dayNumber: Int

    /**
     * 하루가 길다. 체류 + 이동 합계가 이번 일정의 기준([limitMinutes])을 넘었다.
     *
     * 기준은 "목적 프로필대로 관광지를 꽉 채운 날 중 가장 긴 날"이다(편집 전 계획 기준).
     * 고정 시간을 두지 않는 이유: 체류는 전부 같은 값이라 하루 길이를 가르는 것은 이동인데,
     * 이동은 도시마다 달라서 어떤 숫자를 골라도 어딘가에서는 모든 날이 걸리거나 아무 날도 안 걸린다.
     * 그래서 처음 만든 일정에서는 걸리지 않고, 편집으로 동선이 꼬이거나 먼 곳을 끼웠을 때 걸린다.
     */
    data class LongDay(
        override val dayNumber: Int,
        val minutes: Int,
        val limitMinutes: Int,
    ) : ReviewIssue {
        override val id get() = "long-$dayNumber"
    }

    /**
     * 같은 종류 식당이 이어진다. **이어진 끼니 묶음 하나가 한 건이다.**
     *
     * 라멘 → 라멘 → 라멘이면 이웃 쌍은 둘이지만 원인은 가운데 한 곳이다. 쌍마다 한 건씩 띄우면
     * 하나를 고쳤는데 두 건이 함께 사라져 "검토 2건"이 부풀려 보였다(에뮬레이터에서 봤다).
     * 그래서 묶음의 두 번째 끼니를 바꿀 자리([dayNumber]·[meal])로 잡는다 — 셋이면 가운데라
     * 한 번에 다 풀리고, 둘이면 뒤 끼니다. 넷 이상이면 남은 꼬리가 다시 검사에 걸린다.
     *
     * @param neighbors 바꿀 끼니의 앞(과 있으면 뒤) 끼니. 같은 묶음 안의 것만.
     */
    data class SameKind(
        override val dayNumber: Int,
        val meal: MealTime,
        val restaurantName: String,
        /** 화면용 종류 이름(라멘 전문점). */
        val category: String,
        val neighbors: List<MealRef>,
    ) : ReviewIssue {
        override val id get() = "kind-$dayNumber-${meal.name}"
    }

    /** 문장에 적을 끼니 하나. "1일차 점심 라멘 하루카" */
    data class MealRef(val dayNumber: Int, val meal: MealTime, val name: String)

    /**
     * 비(또는 눈) 올 확률이 높은 날인데 실외 관광지가 있다. **그날 한 건**이고 실외 곳을 모두 든다.
     * 실내·실외 판정을 미룬 곳(사찰 등)은 넣지 않는다([IndoorOutdoor]).
     */
    data class Rainy(
        override val dayNumber: Int,
        val probability: Int,
        val outdoorKeys: List<String>,
        val outdoorNames: List<String>,
    ) : ReviewIssue {
        override val id get() = "rain-$dayNumber"
    }

    /** 방문하는 날 쉬는 곳. 관광지면 [meal]이 null. */
    data class Closed(
        override val dayNumber: Int,
        val itemKey: String,
        val name: String,
        val meal: MealTime?,
    ) : ReviewIssue {
        override val id get() = "closed-$dayNumber-$itemKey"
    }
}

/**
 * 편집이 반영된 계획을 검사한다. 순수 함수라 편집할 때마다 불러도 비용이 없다(AI 호출 없음).
 *
 * 지운 항목·바꿔 끼운 곳은 [toTimeline]이 이미 반영하므로, 사용자가 이미 뺀 곳에는 경고하지 않는다.
 * 날짜 순, 같은 날 안에서는 규칙 순서(길이 → 종류 → 휴무)로 돌려준다.
 */
fun reviewPlan(
    plan: TripPlan,
    edits: TimelineEdits,
    picked: PickedPlaces = PickedPlaces(),
    /** 날짜별 예보. D-7 밖이거나 못 받았으면 비어 있고, 그때 비 규칙은 건너뛴다. */
    weather: Map<LocalDate, DailyWeather> = emptyMap(),
): List<ReviewIssue> {
    val timelines = plan.dailyPlans.associateWith { it.editedTimeline(edits, picked) }
    val longDays = findLongDays(plan, timelines)
    val sameKinds = findSameKinds(timelines)
    val closed = findClosed(timelines)
    val rainy = findRainy(timelines, weather)
    return (longDays + sameKinds + closed + rainy).sortedBy { it.dayNumber }
}

/** 강수확률이 [RAINY_PROBABILITY]% 이상인 날의 실외 관광지. */
private fun findRainy(
    timelines: Map<DayPlan, List<TimelineEntry>>,
    weather: Map<LocalDate, DailyWeather>,
): List<ReviewIssue> = timelines.mapNotNull { (day, entries) ->
    val probability = weather[day.date]?.precipitationProbability ?: return@mapNotNull null
    if (probability < RAINY_PROBABILITY) return@mapNotNull null

    val outdoor = entries.mapNotNull { it.item as? TimelineItem.Visit }
        .filter { IndoorOutdoor.of(it.attraction) == Shelter.OUTDOOR }
    if (outdoor.isEmpty()) return@mapNotNull null

    ReviewIssue.Rainy(day.dayNumber, probability, outdoor.map { it.key }, outdoor.map { it.attraction.name })
}

/** 화면에 보이는 그날 타임라인. 검사와 기본 수정이 같은 것을 봐야 한다. */
internal fun DayPlan.editedTimeline(edits: TimelineEdits, picked: PickedPlaces): List<TimelineEntry> =
    toTimeline(
        deletedKeys = edits.deletedIn(dayNumber),
        pickedMeals = edits.pickedIn(dayNumber),
        customOrder = edits.orderIn(dayNumber),
        pickedAttractions = edits.pickedAttractionsIn(dayNumber),
        picked = picked,
    )

/** 체류 + 앞 항목에서 오는 이동. 숙소·공항에서 나오는 첫 이동은 타임라인에 없어 들어가지 않는다. */
internal fun List<TimelineEntry>.totalMinutes(): Int =
    sumOf { it.item.stayMinutes + (it.travelFromPrevious?.minutes ?: 0) }

private fun findLongDays(
    plan: TripPlan,
    timelines: Map<DayPlan, List<TimelineEntry>>,
): List<ReviewIssue> {
    val perDay = PurposeProfiles.of(plan.condition.purposes).attractionsPerDay
    // 편집 전 계획에서 꽉 찬 날. 짧은 여행처럼 그런 날이 없으면 기준을 세울 수 없어 검사하지 않는다.
    val limit = plan.dailyPlans
        .filter { it.attractions.size == perDay }
        .maxOfOrNull { it.toTimeline().totalMinutes() }
        ?: return emptyList()

    return timelines.mapNotNull { (day, entries) ->
        val minutes = entries.totalMinutes()
        if (minutes > limit) ReviewIssue.LongDay(day.dayNumber, minutes, limit) else null
    }
}

/**
 * 점심·저녁을 날짜를 넘어 시간 순으로 잇고, 본업 타입이 같은 끼니가 이어진 묶음마다 한 건을 낸다
 * ([ReviewIssue.SameKind] 참고).
 *
 * - 아침은 보지 않는다. 카페·베이커리가 매일 겹치는 것은 정상이다.
 * - 범용 타입([GENERIC_MEAL_TYPES])은 겹쳐도 잡지 않는다. "음식점"끼리는 같은 음식이라는 뜻이 아니다.
 * - 추천을 못 찾은 끼니가 끼면 사슬을 끊는다 — 그 사이에 사용자가 어디서 먹을지 모른다.
 */
private fun findSameKinds(timelines: Map<DayPlan, List<TimelineEntry>>): List<ReviewIssue> {
    data class Served(val dayNumber: Int, val meal: MealTime, val restaurant: Restaurant?)

    val served = timelines.entries.sortedBy { it.key.dayNumber }.flatMap { (day, entries) ->
        entries.mapNotNull { entry ->
            when (val item = entry.item) {
                is TimelineItem.Meal ->
                    if (item.meal == MealTime.BREAKFAST) null else Served(day.dayNumber, item.meal, item.restaurant)
                is TimelineItem.NoMeal ->
                    if (item.meal == MealTime.BREAKFAST) null else Served(day.dayNumber, item.meal, null)
                is TimelineItem.Visit -> null
            }
        }
    }

    // 앞 끼니와 같은 종류면 지금 묶음에 붙이고, 아니면 새 묶음을 연다.
    val runs = mutableListOf<MutableList<Served>>()
    for (current in served) {
        val type = current.restaurant?.primaryType?.takeUnless { it in GENERIC_MEAL_TYPES }
        val run = runs.lastOrNull()
        val continues = type != null && run != null && run.last().restaurant?.primaryType == type
        if (continues) run!!.add(current) else runs += mutableListOf(current)
    }

    return runs.filter { it.size >= 2 }.map { run ->
        // 묶음에는 식당이 있는 끼니만 들어간다(타입이 있어야 이어 붙는다).
        val target = run[1]
        val chosen = target.restaurant!!
        val neighbors = listOfNotNull(run[0], run.getOrNull(2)).map {
            ReviewIssue.MealRef(it.dayNumber, it.meal, it.restaurant!!.name)
        }

        ReviewIssue.SameKind(
            dayNumber = target.dayNumber,
            meal = target.meal,
            restaurantName = chosen.name,
            category = chosen.category,
            neighbors = neighbors,
        )
    }
}

/** 방문 요일에 쉬는 관광지·식당. 영업시간을 모르면 검사하지 않는다([isClosedOn]). */
private fun findClosed(timelines: Map<DayPlan, List<TimelineEntry>>): List<ReviewIssue> =
    timelines.flatMap { (day, entries) ->
        val weekday = day.date.dayOfWeek
        entries.mapNotNull { entry ->
            when (val item = entry.item) {
                is TimelineItem.Visit ->
                    if (item.attraction.openDays.isClosedOn(weekday)) {
                        ReviewIssue.Closed(day.dayNumber, item.key, item.attraction.name, meal = null)
                    } else {
                        null
                    }
                is TimelineItem.Meal ->
                    if (item.restaurant.openDays.isClosedOn(weekday)) {
                        ReviewIssue.Closed(day.dayNumber, item.key, item.restaurant.name, item.meal)
                    } else {
                        null
                    }
                is TimelineItem.NoMeal -> null
            }
        }
    }

/**
 * 같은 종류로 치지 않는 본업 타입. Places place type 표의 이름이다.
 * 점심·저녁 검색이 `restaurant`로 나가서 본업이 그대로 `restaurant`인 곳이 많다.
 */
private val GENERIC_MEAL_TYPES = setOf(
    "restaurant", "food", "meal_takeaway", "meal_delivery", "cafe", "coffee_shop", "bakery",
)
