package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.plan.TimelineEntry
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.moveAfter
import kr.ac.baekseok.readytogo.domain.plan.toTimeline

/** 명령을 실행하지 않은 이유. 스낵바에 그대로 옮겨 적는다. */
enum class RejectReason {
    /** 별칭이 요약에 없거나, 이미 지워 화면에 없는 항목. */
    UNKNOWN_ITEM,

    /** 끼니를 빼거나 옮기려 했다. 끼니는 하루의 뼈대라 자리가 고정이다(끌기와 같은 규칙). */
    MEAL_FIXED,

    /** 자기 뒤로 옮기기, 또는 첫 끼니보다 앞으로 가는 순서. */
    BAD_POSITION,

    /** 기준에 맞는 다른 식당·관광지가 없다. */
    NO_CANDIDATE,
}

data class Rejection(val command: EditCommand, val reason: RejectReason)

data class ApplyResult(
    val edits: TimelineEdits,
    val appliedCount: Int,
    val rejections: List<Rejection>,
)

/**
 * 명령을 하루 편집에 차례로 반영한다. **유효한 것만 실행하고 나머지는 이유와 함께 돌려준다.**
 *
 * 하나씩 적용할 때마다 타임라인을 다시 만든다 — "오후 하나 빼고 그다음 것을 점심 뒤로"처럼
 * 뒤 명령은 앞 명령이 바뀐 화면을 기준으로 말하기 때문이다.
 *
 * 편집 방식은 손으로 할 때와 같다(TripPlanViewModel의 deleteTimelineItem, pickMeal, reorderDay).
 * 여기서는 [TimelineEdits]만 바꾸고 저장·되돌리기는 ViewModel이 한 번에 한다.
 *
 * @param otherDaysAttractionIds 다른 날 화면에 있는 관광지. 관광지 교체가 같은 곳을 또 넣지 않게 뺀다
 *        ([attractionIdsOutside]로 만든다). 그날 것은 여기서 직접 센다.
 */
fun applyCommands(
    day: DayPlan,
    edits: TimelineEdits,
    commands: List<EditCommand>,
    keyByAlias: Map<String, String>,
    lodging: LatLng?,
    picked: PickedPlaces = PickedPlaces(),
    otherDaysAttractionIds: Set<String> = emptySet(),
): ApplyResult {
    var current = edits
    var applied = 0
    val rejections = mutableListOf<Rejection>()

    for (command in commands) {
        val entries = day.toTimeline(
            deletedKeys = current.deletedIn(day.dayNumber),
            pickedMeals = current.pickedIn(day.dayNumber),
            customOrder = current.orderIn(day.dayNumber),
            pickedAttractions = current.pickedAttractionsIn(day.dayNumber),
            picked = picked,
        )
        val step = Step(day, current, entries, keyByAlias, lodging, otherDaysAttractionIds)
        when (val outcome = step.apply(command)) {
            is Outcome.Applied -> {
                current = outcome.edits
                applied += 1
            }
            is Outcome.Rejected -> rejections += Rejection(command, outcome.reason)
        }
    }
    return ApplyResult(current, applied, rejections)
}

private sealed interface Outcome {
    data class Applied(val edits: TimelineEdits) : Outcome
    data class Rejected(val reason: RejectReason) : Outcome
}

/** 명령 하나를 지금 화면([entries]) 기준으로 검사하고 반영한다. */
private class Step(
    val day: DayPlan,
    val edits: TimelineEdits,
    val entries: List<TimelineEntry>,
    val keyByAlias: Map<String, String>,
    val lodging: LatLng?,
    val otherDaysAttractionIds: Set<String>,
) {
    private val dayNumber get() = day.dayNumber

    fun apply(command: EditCommand): Outcome = when (command) {
        is EditCommand.Remove -> remove(command)
        is EditCommand.Move -> move(command)
        is EditCommand.SwapMeal -> swapMeal(command)
        is EditCommand.SwapPlace -> swapPlace(command)
    }

    private fun swapPlace(command: EditCommand.SwapPlace): Outcome {
        val item = visible(command.id) ?: return Outcome.Rejected(RejectReason.UNKNOWN_ITEM)
        // 끼니는 swap_meal로 바꾼다. 관광지 후보로 끼니 자리를 채울 수는 없다.
        if (item !is TimelineItem.Visit) return Outcome.Rejected(RejectReason.MEAL_FIXED)

        val index = entries.indexOfFirst { it.item.key == item.key }
        val previous = entries.take(index).lastOrNull { it.item.location != null }?.item?.location ?: lodging
        val taken = otherDaysAttractionIds +
            entries.mapNotNull { (it.item as? TimelineItem.Visit)?.attraction?.id }

        val chosen = pickPlaceFor(
            basis = command.basis,
            current = item.attraction,
            alternatives = day.alternatives,
            previous = previous,
            takenIds = taken,
            visitDay = day.date.dayOfWeek,
        ) ?: return Outcome.Rejected(RejectReason.NO_CANDIDATE)

        // 즐겨찾기 끼우기와 같은 기록(자리 키 → 장소 id)이라 저장 형식이 늘지 않는다.
        val next = edits.pickedAttractionsIn(dayNumber) + (item.key to chosen.id)
        return Outcome.Applied(edits.copy(pickedAttractions = edits.pickedAttractions + (dayNumber to next)))
    }

    private fun remove(command: EditCommand.Remove): Outcome {
        val item = visible(command.id) ?: return Outcome.Rejected(RejectReason.UNKNOWN_ITEM)
        if (item !is TimelineItem.Visit) return Outcome.Rejected(RejectReason.MEAL_FIXED)

        val deleted = edits.deletedIn(dayNumber) + item.key
        return Outcome.Applied(edits.copy(deleted = edits.deleted + (dayNumber to deleted)))
    }

    private fun move(command: EditCommand.Move): Outcome {
        val item = visible(command.id) ?: return Outcome.Rejected(RejectReason.UNKNOWN_ITEM)
        val after = visible(command.afterId) ?: return Outcome.Rejected(RejectReason.UNKNOWN_ITEM)
        if (item !is TimelineItem.Visit) return Outcome.Rejected(RejectReason.MEAL_FIXED)

        // "무엇 뒤로"만 받으므로 맨 앞(첫 끼니 위)으로는 갈 수 없지만, 하루가 관광지로
        // 시작하는 순서가 저장되지 않게 결과를 한 번 더 본다 — toTimeline이 아침을
        // 맨 앞으로 되돌리면 사용자가 말한 자리와 다른 곳에 놓인다.
        val keys = moveAfter(entries.map { it.item.key }, item.key, after.key)
            ?: return Outcome.Rejected(RejectReason.BAD_POSITION)
        if (keys.first() == item.key) return Outcome.Rejected(RejectReason.BAD_POSITION)

        return Outcome.Applied(edits.copy(order = edits.order + (dayNumber to keys)))
    }

    private fun swapMeal(command: EditCommand.SwapMeal): Outcome {
        val index = entries.indexOfFirst { entry ->
            val item = entry.item
            (item is TimelineItem.Meal && item.meal == command.meal) ||
                (item is TimelineItem.NoMeal && item.meal == command.meal)
        }
        if (index < 0) return Outcome.Rejected(RejectReason.UNKNOWN_ITEM)
        val meal = entries[index].item as? TimelineItem.Meal
            ?: return Outcome.Rejected(RejectReason.NO_CANDIDATE)

        // 하루 첫 항목이면 앞이 없다. 하루는 숙소에서 시작하므로 숙소를 앞으로 본다.
        val previous = entries.take(index).lastOrNull { it.item.location != null }?.item?.location
            ?: lodging
        val takenIds = entries
            .mapNotNull { it.item as? TimelineItem.Meal }
            .filter { it.meal != command.meal }
            .map { it.restaurant.id }
            .toSet()

        val chosen = pickMealFor(command.basis, meal, previous, lodging, takenIds, day.date.dayOfWeek)
            ?: return Outcome.Rejected(RejectReason.NO_CANDIDATE)

        // 추천(점수 1등)으로 돌아가면 기록을 지운다 — pickMeal과 같은 규칙.
        val current = edits.pickedIn(dayNumber)
        val next = if (chosen.item.id == meal.pair.qualityFirst.item.id) {
            current - meal.key
        } else {
            current + (meal.key to chosen.item.id)
        }
        return Outcome.Applied(edits.copy(pickedMeals = edits.pickedMeals + (dayNumber to next)))
    }

    /** 별칭이 가리키는 항목이 지금 화면에 있으면 그것. */
    private fun visible(alias: String): TimelineItem? {
        val key = keyByAlias[alias] ?: return null
        return entries.firstOrNull { it.item.key == key }?.item
    }
}

/**
 * [dayNumber]가 아닌 날들의 화면에 있는 관광지 id. 관광지 교체가 다른 날에 이미 있는 곳을 고르지 않게 한다.
 * 바꿔 끼운 곳까지 반영해야 하므로 원래 배치가 아니라 편집된 타임라인을 본다.
 */
fun TripPlan.attractionIdsOutside(dayNumber: Int, edits: TimelineEdits, picked: PickedPlaces): Set<String> =
    dailyPlans
        .filter { it.dayNumber != dayNumber }
        .flatMap { day ->
            day.toTimeline(
                deletedKeys = edits.deletedIn(day.dayNumber),
                pickedMeals = edits.pickedIn(day.dayNumber),
                customOrder = edits.orderIn(day.dayNumber),
                pickedAttractions = edits.pickedAttractionsIn(day.dayNumber),
                picked = picked,
            )
        }
        .mapNotNull { (it.item as? TimelineItem.Visit)?.attraction?.id }
        .toSet()
