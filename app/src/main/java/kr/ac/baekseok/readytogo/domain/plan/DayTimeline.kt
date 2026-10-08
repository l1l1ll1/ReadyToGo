package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.Restaurant

/**
 * 타임라인에 들어가는 항목 하나.
 *
 * 관광지와 식당을 한 목록으로 섞어 다루기 위해 공통 타입으로 감싼다.
 * 화면은 순서대로 그리기만 하고, 무엇이 먼저인지 판단하지 않는다.
 */
sealed interface TimelineItem {
    /** 지도·이동시간 계산에 쓰는 좌표. */
    val location: LatLng

    /** 삭제·교체를 구분하는 키. 같은 날에 같은 항목이 두 번 들어가지 않는다. */
    val key: String

    /** 머무는 시간(분). */
    val stayMinutes: Int

    data class Visit(
        val attraction: Attraction,
    ) : TimelineItem {
        override val location get() = attraction.location
        override val key get() = "visit-${attraction.id}"
        override val stayMinutes get() = attraction.stayMinutes
    }

    /**
     * 한 끼. 두 선택지를 함께 들고 있어야 시트에서 "대안으로 교체"를 할 수 있다.
     *
     * @param swapped true면 추천과 대안의 자리를 바꿔 보여준다. 계획을 다시 만들지 않고
     *        화면에서만 뒤집는다 — 다시 만들면 Places 호출이 또 나간다.
     */
    data class Meal(
        val meal: MealTime,
        val pair: RecommendationPair<Restaurant>,
        val swapped: Boolean = false,
    ) : TimelineItem {
        /** 지금 보여줄 쪽. [swapped]면 대안이 앞으로 온다. */
        val shown get() = if (swapped) pair.withinBudget ?: pair.qualityFirst else pair.qualityFirst

        /** 교체 대상. 없으면 교체 버튼을 숨긴다. */
        val other get() = if (swapped) pair.qualityFirst else pair.withinBudget

        val restaurant: Restaurant get() = shown.item

        override val location get() = restaurant.location
        override val key get() = "meal-${meal.name}"
        override val stayMinutes get() = MEAL_MINUTES
    }
}

/** 타임라인의 한 줄 — 항목과, 그 **앞**에 오는 이동. 첫 항목은 이동이 없다. */
data class TimelineEntry(
    val item: TimelineItem,
    val travelFromPrevious: TravelLeg?,
)

/**
 * 하루치 계획을 시간 순 타임라인으로 바꾼다.
 *
 * 순서는 **아침 → 관광지 절반 → 점심 → 남은 관광지 → 저녁**이다.
 * 정확한 시각(08:00 같은)은 적지 않는다 — 장소의 영업시간과 실제 교통을 모르는 상태에서
 * 시각을 적으면 틀린 정보가 된다. 순서와 소요시간만 알려주고 시각은 사용자가 정한다.
 *
 * @param deletedKeys 사용자가 지운 항목. 계획을 다시 만들지 않고 여기서 걸러낸다.
 * @param swappedMealKeys 사용자가 대안으로 교체한 끼니.
 */
fun DayPlan.toTimeline(
    deletedKeys: Set<String> = emptySet(),
    swappedMealKeys: Set<String> = emptySet(),
): List<TimelineEntry> {
    val visits = attractions.map { TimelineItem.Visit(it) }

    fun mealItem(meal: MealTime): TimelineItem.Meal? {
        val pair = meals[meal] ?: return null
        val item = TimelineItem.Meal(meal, pair)
        return item.copy(swapped = item.key in swappedMealKeys)
    }

    // 관광지를 점심 전후로 나눈다. 한 곳뿐이면 점심 뒤에 둔다 — 아침 먹고 바로 나가는 것보다
    // 점심 먹고 움직이는 쪽이 실제 여행에 가깝다.
    val splitAt = if (visits.size <= 1) 0 else (visits.size + 1) / 2

    val ordered = buildList<TimelineItem> {
        mealItem(MealTime.BREAKFAST)?.let { add(it) }
        addAll(visits.take(splitAt))
        mealItem(MealTime.LUNCH)?.let { add(it) }
        addAll(visits.drop(splitAt))
        mealItem(MealTime.DINNER)?.let { add(it) }
    }

    // 이동 시간은 **지운 항목을 걸러낸 뒤에** 계산해야 한다.
    // 먼저 계산하면 지운 항목까지 거쳐 가는 시간이 남는다.
    val remaining = ordered.filterNot { it.key in deletedKeys }

    return remaining.mapIndexed { index, item ->
        TimelineEntry(
            item = item,
            travelFromPrevious = if (index == 0) {
                null
            } else {
                estimateTravel(remaining[index - 1].location, item.location)
            },
        )
    }
}

/**
 * 한 끼에 쓰는 시간.
 *
 * 식당마다 다르지만 Places가 알려주지 않는다. 앉아서 먹는 한 끼를 한 시간으로 잡았다 —
 * 카페 아침은 더 짧고 저녁 코스는 더 길지만, 일정 전체의 느낌을 보는 데는 충분하다.
 */
private const val MEAL_MINUTES = 60
