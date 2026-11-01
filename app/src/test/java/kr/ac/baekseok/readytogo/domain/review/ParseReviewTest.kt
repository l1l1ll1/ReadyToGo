package kr.ac.baekseok.readytogo.domain.review

import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.domain.edit.EditCommand
import kr.ac.baekseok.readytogo.domain.edit.MealBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** reviewIssues 응답 해석. 명령 해석은 말로 고치기와 같은 parseCommands를 거친다. */
class ParseReviewTest {

    private val ids = setOf("kind-3-LUNCH", "closed-2-visit-b1")

    @Test
    fun `문제별로 설명과 명령을 묶는다`() {
        val response = mapOf(
            "fixes" to listOf(
                mapOf(
                    "issue_id" to "kind-3-LUNCH",
                    "why" to " 이틀 연속 라멘이에요. ",
                    "commands" to listOf(mapOf("op" to "swap_meal", "meal" to "lunch", "basis" to "exclude_same_kind")),
                ),
                mapOf(
                    "issue_id" to "closed-2-visit-b1",
                    "why" to "월요일 휴무예요.",
                    "commands" to listOf(mapOf("op" to "remove", "id" to "a1")),
                ),
            ),
        )

        val advice = parseReviewFixes(response, ids)

        assertEquals("이틀 연속 라멘이에요.", advice.getValue("kind-3-LUNCH").why)
        assertEquals(
            listOf(EditCommand.SwapMeal(MealTime.LUNCH, MealBasis.EXCLUDE_SAME_KIND)),
            advice.getValue("kind-3-LUNCH").commands,
        )
        assertEquals(listOf(EditCommand.Remove("a1")), advice.getValue("closed-2-visit-b1").commands)
    }

    @Test
    fun `모르는 문제 id와 모르는 명령은 버린다`() {
        val response = mapOf(
            "fixes" to listOf(
                mapOf("issue_id" to "made-up", "why" to "?", "commands" to emptyList<Any>()),
                mapOf(
                    "issue_id" to "kind-3-LUNCH",
                    "why" to "",
                    "commands" to listOf(mapOf("op" to "swap_place", "id" to "a1")),
                ),
            ),
        )

        val advice = parseReviewFixes(response, ids)

        assertEquals(setOf("kind-3-LUNCH"), advice.keys)
        assertTrue(advice.getValue("kind-3-LUNCH").commands.isEmpty())
    }

    @Test
    fun `형식이 틀리면 빈 결과`() {
        assertTrue(parseReviewFixes(null, ids).isEmpty())
        assertTrue(parseReviewFixes(mapOf("fixes" to "x"), ids).isEmpty())
    }
}
