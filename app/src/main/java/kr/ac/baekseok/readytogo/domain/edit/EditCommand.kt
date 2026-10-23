package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.MealTime

/**
 * "말로 일정 고치기"의 명령 하나. 서버(Claude)가 문장을 이것들로 바꿔 보내고,
 * 앱이 검증한 뒤 기존 편집([kr.ac.baekseok.readytogo.data.model.TimelineEdits])으로 실행한다.
 *
 * id는 실제 키가 아니라 [DaySummary]의 **별칭**(a1, m_lunch)이다. 별칭을 키로 바꾸는 일은
 * [applyCommands]가 한다 — 해석을 한 곳에 둬야 "없는 id" 거부도 한 곳에서 한다.
 *
 * 관광지 교체(swap_place)는 없다. 계획에 관광지 예비 후보가 없어 ⑨-2로 미뤘다.
 */
sealed interface EditCommand {
    data class Remove(val id: String) : EditCommand

    /** 식당은 고르지 않고 기준만 받는다. 실제 선택은 기존 추천 점수가 한다. */
    data class SwapMeal(val meal: MealTime, val basis: MealBasis) : EditCommand

    /** [id]를 [afterId] 바로 뒤로. 같은 날 안에서만. */
    data class Move(val id: String, val afterId: String) : EditCommand
}

/** 끼니를 바꿀 때 "무엇보다 나은 곳"을 찾을지. 이름은 서버 도구 정의(functions/src/tools.ts)와 같다. */
enum class MealBasis(val wireName: String) {
    /** 지금 식당보다 숙소에 가까운 곳. */
    NEAR_LODGING("near_lodging"),

    /** 지금 식당보다 바로 앞 일정에 가까운 곳. */
    NEAR_PREV("near_prev"),

    /** 지금 식당보다 싼 곳. */
    CHEAPER("cheaper"),

    /** 지금 식당과 다른 종류(라멘 말고 다른 것). */
    EXCLUDE_SAME_KIND("exclude_same_kind"),
}

/**
 * 함수 응답의 `commands`를 명령으로 바꾼다.
 *
 * callable 결과는 이미 Map/List로 풀려서 온다. JSON 문자열을 다시 다루지 않아서
 * org.json 없이(=안드로이드 없이) 단위 테스트할 수 있다.
 *
 * **모르는 op나 빠진 필드는 버린다.** 서버가 strict 스키마로 막지만, 서버와 앱의 배포 시점이
 * 어긋나면(새 도구가 먼저 배포되면) 앱이 모르는 명령이 올 수 있다. 하나 때문에 전부를
 * 버리면 나머지까지 못 쓴다.
 */
fun parseCommands(response: Any?): List<EditCommand> {
    val raw = (response as? Map<*, *>)?.get("commands") as? List<*> ?: return emptyList()
    return raw.mapNotNull { (it as? Map<*, *>)?.toCommand() }
}

private fun Map<*, *>.toCommand(): EditCommand? {
    fun text(key: String) = (get(key) as? String)?.takeIf { it.isNotBlank() }

    return when (text("op")) {
        "remove" -> text("id")?.let { EditCommand.Remove(it) }
        "move" -> {
            val id = text("id") ?: return null
            val after = text("after_id") ?: return null
            EditCommand.Move(id, after)
        }
        "swap_meal" -> {
            val meal = MealTime.entries.firstOrNull { it.name.equals(text("meal"), ignoreCase = true) }
            val basis = MealBasis.entries.firstOrNull { it.wireName == text("basis") }
            if (meal == null || basis == null) null else EditCommand.SwapMeal(meal, basis)
        }
        else -> null
    }
}
