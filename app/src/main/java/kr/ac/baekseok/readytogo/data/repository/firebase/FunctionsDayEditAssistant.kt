package kr.ac.baekseok.readytogo.data.repository.firebase

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kr.ac.baekseok.readytogo.data.repository.AssistantReply
import kr.ac.baekseok.readytogo.data.repository.DayEditAssistant
import kr.ac.baekseok.readytogo.data.repository.DayEditException
import kr.ac.baekseok.readytogo.data.repository.await
import kr.ac.baekseok.readytogo.domain.edit.DaySummary
import kr.ac.baekseok.readytogo.domain.edit.parseCommands

/**
 * callable 함수 `editDay`(functions/src/editDay.ts)를 부른다.
 *
 * 로그인 토큰은 SDK가 알아서 실어 보낸다. 함수가 토큰 없는 요청을 거절하므로
 * 둘러보기 사용자는 여기까지 오지 않게 화면에서 막는다.
 */
class FunctionsDayEditAssistant : DayEditAssistant {

    private val callable = FirebaseFunctions.getInstance(FUNCTIONS_REGION).getHttpsCallable(FUNCTION_NAME)

    override suspend fun interpret(sentence: String, summary: DaySummary): Result<AssistantReply> =
        try {
            val data = callable.call(requestOf(sentence, summary)).await().getData()
            val usage = (data as? Map<*, *>)?.get("usage") as? Map<*, *>
            Result.success(
                AssistantReply(
                    commands = parseCommands(data),
                    inputTokens = (usage?.get("input") as? Number)?.toInt() ?: 0,
                    outputTokens = (usage?.get("output") as? Number)?.toInt() ?: 0,
                ),
            )
        } catch (e: Exception) {
            Result.failure(e.toDayEditException())
        }

    /** 요청 형식은 functions/src/editDay.ts의 DayItem과 같다. */
    private fun requestOf(sentence: String, summary: DaySummary): Map<String, Any?> = mapOf(
        "sentence" to sentence,
        "items" to summary.toWireItems(),
    )

    private companion object {
        const val FUNCTION_NAME = "editDay"
    }
}

// 함수를 서울 리전에 배포했다. 리전을 안 맞추면 기본(us-central1)으로 가서 404가 난다.
internal const val FUNCTIONS_REGION = "asia-northeast3"

/** 하루 요약의 전송 형식. functions/src/editDay.ts의 DayItem과 같다. 일정 검토도 같은 형식을 쓴다. */
internal fun DaySummary.toWireItems(): List<Map<String, Any?>> = items.map { item ->
    mapOf(
        "id" to item.id,
        "kind" to if (item.isMeal) "meal" else "place",
        "name" to item.name,
        "category" to item.category,
        "stayMin" to item.stayMinutes,
        "moveMin" to item.moveMinutes,
        "lodgingKm" to item.lodgingKm,
    )
}

/** callable 실패를 화면 문구가 갈리는 이유로 바꾼다. 말로 고치기와 일정 검토가 같은 규칙을 쓴다. */
internal fun Exception.toDayEditException(): DayEditException {
    val reason = when ((this as? FirebaseFunctionsException)?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> DayEditException.Reason.SIGN_IN_REQUIRED
        FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> DayEditException.Reason.TOO_MANY_REQUESTS
        // 그 밖의 함수 오류, 또는 네트워크 끊김처럼 함수까지 가지도 못한 경우.
        else -> DayEditException.Reason.UNAVAILABLE
    }
    return DayEditException(reason, this)
}
