package kr.ac.baekseok.readytogo.domain.review

import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.domain.edit.ApplyResult
import kr.ac.baekseok.readytogo.domain.edit.DaySummary
import kr.ac.baekseok.readytogo.domain.edit.EditCommand
import kr.ac.baekseok.readytogo.domain.edit.MealBasis
import kr.ac.baekseok.readytogo.domain.edit.applyCommands
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem

/**
 * 규칙이 정한 기본 수정. 둘러보기(AI 호출 불가)·호출 실패 때 쓰고, AI가 낸 명령이
 * 하나도 먹히지 않았을 때도 쓴다.
 *
 * **후보를 여러 벌** 돌려준다. 실행 쪽([applyFirst])이 앞에서부터 시도해 처음 먹힌 것을 쓴다 —
 * 어떤 기준이 통할지는 실행해 봐야 안다(더 가까운 식당이 없을 수도 있다).
 *
 * 명령의 id는 [summary]의 별칭이다. AI 명령과 똑같은 경로([applyCommands])로 실행하려고
 * 실제 키를 별칭으로 바꿔 둔다.
 */
fun defaultFixes(
    issue: ReviewIssue,
    day: DayPlan,
    edits: TimelineEdits,
    picked: PickedPlaces,
    summary: DaySummary,
): List<List<EditCommand>> {
    val aliasByKey = summary.keyByAlias.entries.associate { (alias, key) -> key to alias }

    return when (issue) {
        // 빼면 하루가 가장 많이 짧아지는 관광지부터. 보통 동선에서 가장 튀어나온 곳이다.
        is ReviewIssue.LongDay -> {
            val visits = day.editedTimeline(edits, picked).map { it.item }.filterIsInstance<TimelineItem.Visit>()
            visits
                .sortedBy { visit ->
                    val without = edits.copy(deleted = edits.deleted + (day.dayNumber to (edits.deletedIn(day.dayNumber) + visit.key)))
                    day.editedTimeline(without, picked).totalMinutes()
                }
                .mapNotNull { aliasByKey[it.key] }
                .map { listOf(EditCommand.Remove(it)) }
        }

        is ReviewIssue.SameKind ->
            listOf(listOf(EditCommand.SwapMeal(issue.meal, MealBasis.EXCLUDE_SAME_KIND)))

        is ReviewIssue.Closed -> when (val meal = issue.meal) {
            null -> listOfNotNull(aliasByKey[issue.itemKey]?.let { listOf(EditCommand.Remove(it)) })
            // 쉬는 집은 교체 후보에서 빠지므로(pickMealFor의 visitDay) 어느 기준이든 연 곳으로 바뀐다.
            // 동선을 덜 흔드는 기준부터 시도한다.
            else -> CLOSED_MEAL_BASES.map { listOf(EditCommand.SwapMeal(meal, it)) }
        }
    }
}

/** 후보를 차례로 실행해 하나라도 먹힌 첫 결과. 전부 실패하면 null. */
fun applyFirst(
    day: DayPlan,
    edits: TimelineEdits,
    alternatives: List<List<EditCommand>>,
    keyByAlias: Map<String, String>,
    lodging: LatLng?,
    picked: PickedPlaces,
): ApplyResult? =
    alternatives.asSequence()
        .map { applyCommands(day, edits, it, keyByAlias, lodging, picked) }
        .firstOrNull { it.appliedCount > 0 }

private val CLOSED_MEAL_BASES = listOf(
    MealBasis.NEAR_PREV,
    MealBasis.NEAR_LODGING,
    MealBasis.EXCLUDE_SAME_KIND,
    MealBasis.CHEAPER,
)
