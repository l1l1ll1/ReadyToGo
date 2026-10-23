package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.domain.edit.DaySummary
import kr.ac.baekseok.readytogo.domain.edit.EditCommand

/**
 * 사용자 문장을 하루 편집 명령으로 바꿔 주는 곳. 지금 구현은 Cloud Functions → Claude Haiku다.
 *
 * **명령을 돌려줄 뿐 실행하지 않는다.** 검증·실행은 앱의 기존 편집 규칙이 한다
 * ([kr.ac.baekseok.readytogo.domain.edit.applyCommands]).
 */
interface DayEditAssistant {
    suspend fun interpret(sentence: String, summary: DaySummary): Result<AssistantReply>
}

/** 토큰 수는 비용 확인용이다. 화면에는 쓰지 않고 로그로만 남긴다. */
data class AssistantReply(
    val commands: List<EditCommand>,
    val inputTokens: Int,
    val outputTokens: Int,
)

/**
 * 실패 이유. 화면 문구가 갈린다 — "잠시 후 다시"와 "로그인 필요"는 사용자가 할 일이 다르다.
 */
class DayEditException(val reason: Reason, cause: Throwable? = null) : Exception(cause) {
    enum class Reason { SIGN_IN_REQUIRED, TOO_MANY_REQUESTS, UNAVAILABLE }
}
