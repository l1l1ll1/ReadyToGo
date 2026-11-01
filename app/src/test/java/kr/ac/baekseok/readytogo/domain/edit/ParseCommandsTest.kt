package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.MealTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 함수 응답 → 명령. callable 결과처럼 Map/List로 넣는다.
 * 형식이 어긋난 하나 때문에 나머지를 버리지 않는지가 핵심이다.
 */
class ParseCommandsTest {

    private fun response(vararg commands: Map<String, Any?>) =
        mapOf("commands" to commands.toList(), "usage" to mapOf("input" to 900, "output" to 80))

    @Test
    fun `도구 세 가지를 모두 읽는다`() {
        val parsed = parseCommands(
            response(
                mapOf("op" to "remove", "id" to "a3"),
                mapOf("op" to "swap_meal", "meal" to "dinner", "basis" to "near_lodging"),
                mapOf("op" to "move", "id" to "a4", "after_id" to "m_breakfast"),
            ),
        )

        assertEquals(
            listOf(
                EditCommand.Remove("a3"),
                EditCommand.SwapMeal(MealTime.DINNER, MealBasis.NEAR_LODGING),
                EditCommand.Move("a4", "m_breakfast"),
            ),
            parsed,
        )
    }

    @Test
    fun `모르는 op, 빠진 필드, 틀린 enum은 그것만 버린다`() {
        val parsed = parseCommands(
            response(
                mapOf("op" to "teleport", "id" to "a1"),
                mapOf("op" to "swap_place", "id" to "a1", "basis" to "cheaper"),
                mapOf("op" to "move", "id" to "a1"),
                mapOf("op" to "swap_meal", "meal" to "brunch", "basis" to "cheaper"),
                mapOf("op" to "swap_meal", "meal" to "lunch", "basis" to "tastier"),
                mapOf("op" to "remove", "id" to ""),
                mapOf("op" to "remove", "id" to "a2"),
            ),
        )

        assertEquals(listOf(EditCommand.Remove("a2")), parsed)
    }

    @Test
    fun `명령이 없거나 응답 모양이 다르면 빈 목록`() {
        assertTrue(parseCommands(response()).isEmpty())
        assertTrue(parseCommands(null).isEmpty())
        assertTrue(parseCommands(mapOf("commands" to "remove a1")).isEmpty())
        assertTrue(parseCommands(listOf(mapOf("op" to "remove", "id" to "a1"))).isEmpty())
    }

    @Test
    fun `관광지 교체는 id와 기준을 읽는다`() {
        val parsed = parseCommands(
            response(
                mapOf("op" to "swap_place", "id" to "a1", "basis" to "indoor"),
                mapOf("op" to "swap_place", "id" to "a2", "basis" to "near_prev"),
            ),
        )

        assertEquals(
            listOf(EditCommand.SwapPlace("a1", PlaceBasis.INDOOR), EditCommand.SwapPlace("a2", PlaceBasis.NEAR_PREV)),
            parsed,
        )
    }
}
