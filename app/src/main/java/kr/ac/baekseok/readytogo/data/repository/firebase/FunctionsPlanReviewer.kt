package kr.ac.baekseok.readytogo.data.repository.firebase

import com.google.firebase.functions.FirebaseFunctions
import kr.ac.baekseok.readytogo.data.repository.PlanReviewer
import kr.ac.baekseok.readytogo.data.repository.ReviewQuestion
import kr.ac.baekseok.readytogo.data.repository.ReviewReply
import kr.ac.baekseok.readytogo.data.repository.await
import kr.ac.baekseok.readytogo.domain.edit.DaySummary
import kr.ac.baekseok.readytogo.domain.review.parseReviewFixes

/**
 * callable 함수 `reviewIssues`(functions/src/reviewIssues.ts)를 부른다.
 *
 * 말로 고치기([FunctionsDayEditAssistant])와 같은 리전·전송 형식·실패 규칙을 쓴다.
 * 둘러보기 사용자는 함수가 거절하므로 화면에서 부르지 않는다(규칙 설명과 기본 수정만 쓴다).
 */
class FunctionsPlanReviewer : PlanReviewer {

    private val callable = FirebaseFunctions.getInstance(FUNCTIONS_REGION).getHttpsCallable(FUNCTION_NAME)

    override suspend fun review(
        issues: List<ReviewQuestion>,
        days: Map<Int, DaySummary>,
    ): Result<ReviewReply> =
        try {
            val data = callable.call(requestOf(issues, days)).await().getData()
            val usage = (data as? Map<*, *>)?.get("usage") as? Map<*, *>
            Result.success(
                ReviewReply(
                    advice = parseReviewFixes(data, issues.map { it.id }.toSet()),
                    inputTokens = (usage?.get("input") as? Number)?.toInt() ?: 0,
                    outputTokens = (usage?.get("output") as? Number)?.toInt() ?: 0,
                ),
            )
        } catch (e: Exception) {
            Result.failure(e.toDayEditException())
        }

    /** 요청 형식은 functions/src/reviewIssues.ts의 ReviewRequest와 같다. */
    private fun requestOf(issues: List<ReviewQuestion>, days: Map<Int, DaySummary>): Map<String, Any?> = mapOf(
        "issues" to issues.map { mapOf("id" to it.id, "day" to it.dayNumber, "fact" to it.fact) },
        "days" to days.entries.sortedBy { it.key }.map { (day, summary) ->
            mapOf("day" to day, "items" to summary.toWireItems())
        },
    )

    private companion object {
        const val FUNCTION_NAME = "reviewIssues"
    }
}
