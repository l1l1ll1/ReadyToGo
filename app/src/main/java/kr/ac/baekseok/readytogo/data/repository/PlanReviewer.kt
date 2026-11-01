package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.domain.edit.DaySummary
import kr.ac.baekseok.readytogo.domain.review.ReviewAdvice

/**
 * 규칙이 찾은 문제에 설명과 고칠 명령을 붙여 주는 곳(⑨-2). 지금 구현은 Cloud Functions → Claude Haiku다.
 *
 * 문제를 찾지 않는다 — 문제 목록은 [kr.ac.baekseok.readytogo.domain.review.reviewPlan]이 만든다.
 * 실패 이유는 말로 고치기와 같다([DayEditException]).
 */
interface PlanReviewer {
    /**
     * @param issues 문제 id·일차·사실 문장
     * @param days 문제가 있는 날의 요약. 일차 → 요약
     */
    suspend fun review(issues: List<ReviewQuestion>, days: Map<Int, DaySummary>): Result<ReviewReply>
}

/** 서버로 보내는 문제 하나. [fact]는 앱이 규칙 결과로 쓴 사실 문장이라, 모델이 지어낼 여지가 없다. */
data class ReviewQuestion(val id: String, val dayNumber: Int, val fact: String)

/** 토큰 수는 비용 확인용이다. 화면에는 쓰지 않고 로그로만 남긴다. */
data class ReviewReply(
    val advice: Map<String, ReviewAdvice>,
    val inputTokens: Int,
    val outputTokens: Int,
)
