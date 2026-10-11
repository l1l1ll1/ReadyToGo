package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.Restaurant

/**
 * 타임라인에 들어가는 항목 하나.
 *
 * 관광지와 식당을 한 목록으로 섞어 다루기 위해 공통 타입으로 감싼다.
 * 화면은 순서대로 그리기만 하고, 무엇이 먼저인지 판단하지 않는다.
 */
sealed interface TimelineItem {
    /**
     * 지도·이동시간 계산에 쓰는 좌표.
     *
     * null이면 **장소가 아닌 항목**이다([NoMeal]). 이동시간 계산에서 건너뛴다 —
     * 좌표 없는 항목을 사슬에 끼우면 앞뒤 거리가 0으로 잡혀 이동시간이 사라진다.
     */
    val location: LatLng?

    /** 삭제·교체를 구분하는 키. 같은 날에 같은 항목이 두 번 들어가지 않는다. */
    val key: String

    /** 머무는 시간(분). */
    val stayMinutes: Int

    data class Visit(
        val attraction: Attraction,
        /**
         * 원래 이 자리에 있던 관광지의 id.
         *
         * **키를 여기에 묶는 이유**: 즐겨찾기로 바꿔 끼우면 attraction이 통째로 달라지는데,
         * 키까지 바뀌면 지운 기록과 순서 기록이 그 자리를 못 찾는다. 자리는 그대로 두고
         * 안에 든 장소만 바꾼다.
         */
        val slotId: String = attraction.id,
    ) : TimelineItem {
        override val location get() = attraction.location
        override val key get() = "visit-$slotId"
        override val stayMinutes get() = attraction.stayMinutes
    }

    /**
     * 한 끼. 후보를 전부 들고 있어야 시트에서 다른 곳으로 바꿀 수 있다.
     *
     * **계획을 다시 만들지 않고 보여줄 것만 바꾼다.** 다시 만들면 Places 호출이 또 나간다.
     *
     * @param pickedPlaceId 사용자가 고른 곳의 id. null이면 추천(점수 1등)을 보여준다.
     *        고른 곳이 후보에 없으면(저장된 편집을 읽었는데 계획이 바뀐 경우) 추천으로 돌아간다.
     */
    data class Meal(
        val meal: MealTime,
        val pair: RecommendationPair<Restaurant>,
        val pickedPlaceId: String? = null,
        /**
         * 즐겨찾기에서 골라 끼운 식당. 후보 목록에 없는 곳이라 따로 들고 온다.
         *
         * 아직 받아 오는 중이면 null이고, 그동안은 추천이 그대로 보인다.
         */
        val pickedRestaurant: Restaurant? = null,
    ) : TimelineItem {
        /**
         * 고를 수 있는 전부. 추천 → 대안 → 나머지 순이고, 같은 곳이 두 번 들어가지 않는다.
         *
         * 시트의 목록이 이 순서를 그대로 쓴다. 점수 순이라 위에 있을수록 더 추천하는 곳이다.
         */
        val options: List<Recommendation<Restaurant>>
            get() = (listOf(pair.qualityFirst) + listOfNotNull(pair.withinBudget) + pair.alternatives)
                .distinctBy { it.item.id }

        /**
         * 지금 보여줄 쪽.
         *
         * 즐겨찾기에서 고른 곳은 후보에 없으므로 점수 없이 감싼다  14 사용자가 직접 고른
         * 자리라 추천 점수를 매길 근거가 없다.
         */
        val shown: Recommendation<Restaurant>
            get() = options.firstOrNull { it.item.id == pickedPlaceId }
                ?: pickedRestaurant?.let { Recommendation(item = it, score = 0.0, overBudget = false) }
                ?: pair.qualityFirst

        val restaurant: Restaurant get() = shown.item

        override val location get() = restaurant.location
        override val key get() = "meal-${meal.name}"
        override val stayMinutes get() = MEAL_MINUTES
    }

    /**
     * 추천할 식당을 찾지 못한 끼니.
     *
     * 그냥 빼버리면 "아침 다음 저녁"이 되어 점심을 잊은 것처럼 보인다. 자리를 남기고
     * 왜 비었는지 적어야 사용자가 직접 찾아볼 수 있다.
     */
    data class NoMeal(val meal: MealTime) : TimelineItem {
        override val location: LatLng? get() = null
        override val key get() = "nomeal-${meal.name}"
        override val stayMinutes get() = 0
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
 * @param pickedMeals 사용자가 직접 고른 끼니. 끼니 키 → 고른 장소 id.
 * @param customOrder 끌어서 바꾼 순서. 비어 있거나 키가 맞지 않으면 기본 순서를 쓴다.
 * @param pickedAttractions 즐겨찾기로 바꿔 끼운 관광지. 자리 키 → 고른 장소 id.
 * @param picked 위 두 가지가 가리키는 실제 장소. 아직 못 받았으면 원래 것을 그대로 둔다.
 */
fun DayPlan.toTimeline(
    deletedKeys: Set<String> = emptySet(),
    pickedMeals: Map<String, String> = emptyMap(),
    customOrder: List<String> = emptyList(),
    pickedAttractions: Map<String, String> = emptyMap(),
    picked: PickedPlaces = PickedPlaces(),
): List<TimelineEntry> {
    val visits = attractions.map { attraction ->
        val slot = TimelineItem.Visit(attraction)
        // 바꿔 끼운 곳이 있으면 자리는 두고 안에 든 장소만 갈아 끼운다.
        val replacement = pickedAttractions[slot.key]?.let { picked.attractions[it] }
        if (replacement == null) slot else slot.copy(attraction = replacement)
    }

    // 추천을 못 찾은 끼니도 자리를 남긴다. 없으면 점심을 잊은 것처럼 보인다.
    fun mealItem(meal: MealTime): TimelineItem {
        val pair = meals[meal] ?: return TimelineItem.NoMeal(meal)
        val item = TimelineItem.Meal(meal, pair)
        return item.copy(
            pickedPlaceId = pickedMeals[item.key],
            pickedRestaurant = pickedMeals[item.key]?.let { picked.restaurants[it] },
        )
    }

    // 관광지를 점심 전후로 나눈다. 한 곳뿐이면 점심 뒤에 둔다 — 아침 먹고 바로 나가는 것보다
    // 점심 먹고 움직이는 쪽이 실제 여행에 가깝다.
    val splitAt = if (visits.size <= 1) 0 else (visits.size + 1) / 2

    val ordered = buildList {
        add(mealItem(MealTime.BREAKFAST))
        addAll(visits.take(splitAt))
        add(mealItem(MealTime.LUNCH))
        addAll(visits.drop(splitAt))
        add(mealItem(MealTime.DINNER))
    }

    // 사용자가 끌어서 바꾼 순서가 있으면 그것을 따른다.
    //
    // **키가 맞지 않으면 기본 순서로 돌아간다.** 저장해 둔 순서를 읽었는데 그 사이 계획이
    // 바뀌었을 수 있다(관광지가 달라지면 키도 달라진다). 어긋난 순서를 억지로 적용하면
    // 항목이 사라지거나 중복된다.
    val sorted = if (customOrder.isEmpty() || customOrder.toSet() != ordered.map { it.key }.toSet()) {
        ordered
    } else {
        ordered.sortedBy { customOrder.indexOf(it.key) }
    }

    // 이동 시간은 **순서를 정하고 지운 항목을 걸러낸 뒤에** 계산해야 한다.
    // 먼저 계산하면 지운 항목까지 거쳐 가는 시간이 남고, 순서를 바꿔도 "도보 17분"이 그대로다.
    val remaining = sorted.filterNot { it.key in deletedKeys }

    // 좌표가 없는 항목(추천 못 찾은 끼니)은 건너뛰고 **마지막 좌표**에서 거리를 잰다.
    // 그러지 않으면 빈 끼니가 끼는 순간 앞뒤 이동시간이 사라진다.
    var lastLocation: LatLng? = null

    return remaining.map { item ->
        val from = lastLocation
        val to = item.location
        val travel = if (from != null && to != null) estimateTravel(from, to) else null
        if (to != null) lastLocation = to

        TimelineEntry(item = item, travelFromPrevious = travel)
    }
}

/**
 * 한 끼에 쓰는 시간.
 *
 * 식당마다 다르지만 Places가 알려주지 않는다. 앉아서 먹는 한 끼를 한 시간으로 잡았다 —
 * 카페 아침은 더 짧고 저녁 코스는 더 길지만, 일정 전체의 느낌을 보는 데는 충분하다.
 */
private const val MEAL_MINUTES = 60
