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

    // 함수를 서울 리전에 배포했다. 리전을 안 맞추면 기본(us-central1)으로 가서 404가 난다.
    private val callable = FirebaseFunctions.getInstance(REGION).getHttpsCallable(FUNCTION_NAME)

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
        } catch (e: FirebaseFunctionsException) {
            val reason = when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> DayEditException.Reason.SIGN_IN_REQUIRED
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> DayEditException.Reason.TOO_MANY_REQUESTS
                else -> DayEditException.Reason.UNAVAILABLE
            }
            Result.failure(DayEditException(reason, e))
        } catch (e: Exception) {
            // 네트워크 끊김 등. 함수까지 가지도 못했다.
            Result.failure(DayEditException(DayEditException.Reason.UNAVAILABLE, e))
        }

    /** 요청 형식은 functions/src/editDay.ts의 DayItem과 같다. */
    private fun requestOf(sentence: String, summary: DaySummary): Map<String, Any?> = mapOf(
        "sentence" to sentence,
        "items" to summary.items.map { item ->
            mapOf(
                "id" to item.id,
                "kind" to if (item.isMeal) "meal" else "place",
                "name" to item.name,
                "category" to item.category,
                "stayMin" to item.stayMinutes,
                "moveMin" to item.moveMinutes,
                "lodgingKm" to item.lodgingKm,
            )
        },
    )

    private companion object {
        const val REGION = "asia-northeast3"
        const val FUNCTION_NAME = "editDay"
    }
}
