package kr.ac.baekseok.readytogo.ui.plan

import android.content.res.Resources
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.review.ReviewIssue
import kr.ac.baekseok.readytogo.ui.format.Particle
import kr.ac.baekseok.readytogo.ui.format.withParticle
import java.time.format.TextStyle
import java.util.Locale

/**
 * 검토 문제의 사실 문장. 서버에 보내는 문장이자 화면의 항목 제목이다 — AI 설명이 없어도
 * (둘러보기·실패) 무엇이 문제인지는 보여야 한다. 화면 언어로 쓰고, 모델은 같은 언어로 답한다.
 *
 * 결과 화면과 홈(⑫ 검토 카드)이 같이 쓴다. 두 곳이 다른 문장을 쓰면 같은 문제가 다르게 읽힌다.
 *
 * @param resources 앱 언어가 반영된 리소스. 기기 Locale이 아니라 앱 설정 언어를 따라야 한다.
 */
class ReviewFactWriter(private val resources: Resources) {

    /**
     * 카드 항목의 제목. 하루 길이만 한 줄로 줄인다 — 길이·기준·가장 긴 구간까지 다 실은 사실 문장은
     * 제목으로 쓰면 다섯 줄이 됐다. 나머지는 사실 문장이 곧 제목이다(누가·어디서가 제목에 있어야 한다).
     */
    fun title(issue: ReviewIssue, plan: TripPlan): String = when (issue) {
        is ReviewIssue.LongDay -> resources.getString(R.string.review_title_long, issue.dayNumber)
        else -> fact(issue, plan)
    }

    /**
     * 제목을 줄인 문제에서 AI 설명이 없을 때(둘러보기·실패) 대신 보일 설명. 줄이지 않은 문제는 null —
     * 제목에 이미 다 있다. AI에는 언제나 [fact] 전체가 가므로 AI 설명에도 원인 구간이 들어간다.
     */
    fun detail(issue: ReviewIssue, plan: TripPlan): String? = when (issue) {
        is ReviewIssue.LongDay -> fact(issue, plan)
        else -> null
    }

    fun fact(issue: ReviewIssue, plan: TripPlan): String = when (issue) {
        is ReviewIssue.LongDay -> {
            val length = resources.getString(
                R.string.review_fact_long,
                issue.dayNumber,
                minutesText(issue.minutes),
                minutesText(issue.limitMinutes),
            )
            // 가장 긴 구간을 덧붙인다. AI는 이 문장만 보고 설명하므로, 여기 없으면 원인을 말할 수 없다.
            fun legText(leg: ReviewIssue.Leg, nameLimit: Int?) = resources.getString(
                R.string.review_fact_long_leg,
                leg.fromName.clip(nameLimit),
                leg.toName.clip(nameLimit),
                "%.1f".format(leg.distanceKm),
                minutesText(leg.minutes),
            )
            val leg = issue.longestLeg ?: return length
            val full = "$length ${legText(leg, null)}"
            // 서버(reviewIssues)는 사실 문장이 MAX_FACT_LENGTH를 넘으면 요청 전체를 거절한다.
            // 긴 영문·한자 상호 둘이 겹칠 때만 이름을 줄인다 — 구간을 통째로 빼면 원인이 사라진다.
            if (full.length <= MAX_FACT_LENGTH) full else "$length ${legText(leg, CLIPPED_NAME_LENGTH)}"
        }
        is ReviewIssue.SameKind -> {
            val target = mealRefText(ReviewIssue.MealRef(issue.dayNumber, issue.meal, issue.restaurantName))
                .particle(Particle.SUBJECT)
            // 조사는 마지막 이웃에만 붙는다("A, B와 같은 종류").
            val neighbors = issue.neighbors.map(::mealRefText)
            val last = neighbors.last().particle(Particle.AND)
            if (neighbors.size == 1) {
                resources.getString(R.string.review_fact_same_kind, target, last, issue.category)
            } else {
                resources.getString(R.string.review_fact_same_kind_both, target, neighbors.first(), last, issue.category)
            }
        }
        is ReviewIssue.Closed -> {
            val weekday = plan.dailyPlans.first { it.dayNumber == issue.dayNumber }.date.dayOfWeek
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
            val name = issue.name.particle(Particle.TOPIC)
            when (val meal = issue.meal) {
                null -> resources.getString(R.string.review_fact_closed_place, issue.dayNumber, weekday, name)
                else -> resources.getString(
                    R.string.review_fact_closed_meal, issue.dayNumber, weekday, resources.getString(meal.labelRes), name,
                )
            }
        }
        is ReviewIssue.Rainy -> resources.getString(
            R.string.review_fact_rainy,
            issue.dayNumber,
            issue.probability,
            issue.outdoorNames.joinToString(", "),
        )
    }

    /** "1일차 점심 라멘 하루카" */
    private fun mealRefText(ref: ReviewIssue.MealRef): String =
        resources.getString(R.string.review_meal_ref, ref.dayNumber, resources.getString(ref.meal.labelRes), ref.name)

    /**
     * 한국어 화면이면 받침에 맞는 조사를 붙인다. 영어 문장은 조사가 없어 이름 그대로 넘긴다 —
     * 같은 인자를 두 언어 문자열이 함께 쓰기 때문에 여기서 가른다.
     */
    private fun String.particle(particle: Particle): String {
        val language = resources.configuration.locales[0].language
        return if (language == Locale.KOREAN.language) withParticle(this, particle) else this
    }

    /** "3시간 20분". 화면의 durationText는 Composable이라 여기서 못 쓴다 — 같은 문자열 리소스를 쓴다. */
    private fun minutesText(minutes: Int): String {
        val hours = minutes / MINUTES_PER_HOUR
        val rest = minutes % MINUTES_PER_HOUR
        return when {
            hours == 0 -> resources.getString(R.string.fmt_duration_m, rest)
            rest == 0 -> resources.getString(R.string.fmt_duration_h, hours)
            else -> resources.getString(R.string.fmt_duration_hm, hours, rest)
        }
    }

    private fun String.clip(limit: Int?): String =
        if (limit == null || length <= limit) this else take(limit) + "…"

    private companion object {
        const val MINUTES_PER_HOUR = 60

        /** functions/src/reviewIssues.ts의 MAX_FACT_LENGTH와 같은 값. 바꾸면 둘 다 바꾼다. */
        const val MAX_FACT_LENGTH = 200

        /** 넘칠 때 이름 하나에 남길 글자 수. 기본 문장 약 100자 + 이름 둘이 200 안에 든다. */
        const val CLIPPED_NAME_LENGTH = 20
    }
}
