package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.day
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.hotel
import kr.ac.baekseok.readytogo.domain.edit.EditFixtures.timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 모델이 돌려준 명령을 앱이 검증·실행하는 규칙. Claude를 부르지 않는다 —
 * 명령 목록을 직접 만들어 넣는다.
 *
 * 별칭은 실제 화면과 같은 경로(summarizeDay)로 만든다. a1~a4 = a b c d.
 */
class ApplyCommandsTest {

    private val aliases = summarizeDay(timeline(), hotel).keyByAlias
    private val dayNumber = day.dayNumber

    private fun run(vararg commands: EditCommand, edits: TimelineEdits = TimelineEdits()) =
        applyCommands(day, edits, commands.toList(), aliases, hotel)

    private fun keys(edits: TimelineEdits) = timeline(edits).map { it.item.key }

    @Test
    fun `별칭은 화면 순서대로 붙고 지운 항목은 빠진다`() {
        assertEquals(
            listOf("m_breakfast", "a1", "a2", "m_lunch", "a3", "a4", "m_dinner"),
            summarizeDay(timeline(), hotel).items.map { it.id },
        )
        assertEquals("visit-c", aliases["a3"])
    }

    @Test
    fun `오후 관광지 하나 빼기`() {
        val result = run(EditCommand.Remove("a3"))

        assertEquals(1, result.appliedCount)
        assertEquals(setOf("visit-c"), result.edits.deletedIn(dayNumber))
    }

    @Test
    fun `없는 id는 거부하고 편집을 건드리지 않는다`() {
        val result = run(EditCommand.Remove("a9"))

        assertEquals(0, result.appliedCount)
        assertEquals(RejectReason.UNKNOWN_ITEM, result.rejections.single().reason)
        assertEquals(TimelineEdits(), result.edits)
    }

    @Test
    fun `끼니는 뺄 수도 옮길 수도 없다`() {
        val result = run(
            EditCommand.Remove("m_lunch"),
            EditCommand.Move("m_dinner", "a1"),
        )

        assertEquals(0, result.appliedCount)
        assertEquals(
            listOf(RejectReason.MEAL_FIXED, RejectReason.MEAL_FIXED),
            result.rejections.map { it.reason },
        )
    }

    @Test
    fun `관광지를 아침 바로 뒤로 옮긴다`() {
        val result = run(EditCommand.Move("a4", "m_breakfast"))

        assertEquals(
            listOf("meal-BREAKFAST", "visit-d", "visit-a", "visit-b", "meal-LUNCH", "visit-c", "meal-DINNER"),
            keys(result.edits),
        )
    }

    @Test
    fun `자기 뒤로 옮기기와 없는 기준점은 거부`() {
        val result = run(
            EditCommand.Move("a2", "a2"),
            EditCommand.Move("a2", "a9"),
        )

        assertEquals(
            listOf(RejectReason.BAD_POSITION, RejectReason.UNKNOWN_ITEM),
            result.rejections.map { it.reason },
        )
    }

    @Test
    fun `뒤 명령은 앞 명령이 바꾼 화면을 기준으로 본다 - 이미 뺀 것은 다시 못 뺀다`() {
        val result = run(
            EditCommand.Remove("a1"),
            EditCommand.Move("a2", "m_lunch"),
            EditCommand.Remove("a1"),
        )

        assertEquals(2, result.appliedCount)
        assertEquals(RejectReason.UNKNOWN_ITEM, result.rejections.single().reason)
        assertEquals(
            listOf("meal-BREAKFAST", "meal-LUNCH", "visit-b", "visit-c", "visit-d", "meal-DINNER"),
            keys(result.edits),
        )
    }

    @Test
    fun `후보가 없으면 그 명령만 거부하고 나머지는 실행한다`() {
        val result = run(
            EditCommand.Remove("a3"),
            EditCommand.SwapMeal(MealTime.BREAKFAST, MealBasis.EXCLUDE_SAME_KIND),
        )

        assertEquals(1, result.appliedCount)
        assertEquals(RejectReason.NO_CANDIDATE, result.rejections.single().reason)
        assertEquals(setOf("visit-c"), result.edits.deletedIn(dayNumber))
    }

    @Test
    fun `저녁을 숙소 근처로 바꾸면 고른 식당이 편집에 남는다`() {
        val result = run(EditCommand.SwapMeal(MealTime.DINNER, MealBasis.NEAR_LODGING))

        assertEquals(mapOf("meal-DINNER" to "din2"), result.edits.pickedIn(dayNumber))
    }

    @Test
    fun `추천 1등으로 돌아가면 고른 기록을 지운다 - pickMeal과 같은 규칙`() {
        // 점심을 스시(3만원)로 바꿔 둔 상태에서 "더 싼 걸로" → 라멘1(1.5만원, 점수 1등).
        val picked = TimelineEdits(pickedMeals = mapOf(dayNumber to mapOf("meal-LUNCH" to "sushi")))
        val result = run(EditCommand.SwapMeal(MealTime.LUNCH, MealBasis.CHEAPER), edits = picked)

        assertTrue(result.edits.pickedIn(dayNumber).isEmpty())
    }

    @Test
    fun `명령이 0개면 아무것도 바뀌지 않는다`() {
        val result = run()

        assertEquals(0, result.appliedCount)
        assertTrue(result.rejections.isEmpty())
        assertEquals(TimelineEdits(), result.edits)
    }
}
