package kr.ac.baekseok.readytogo.domain.budget

import kr.ac.baekseok.readytogo.data.model.BudgetAllocation
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.domain.purpose.PurposeProfiles
import java.time.temporal.ChronoUnit

/**
 * 항공료를 뺀 나머지 예산을 나누는 비율은 **여행 목적에 따라 달라진다**(PurposeProfiles).
 * 맛집 여행이면 식비 비중이 오르고, 휴양이면 숙박 비중이 오른다.
 *
 * 항공료 자체는 비율로 잡지 않는다 — 목적지·시즌에 따라 총액에서 차지하는 몫이 너무 달라서
 * (도쿄 20만원대 vs 유럽 150만원대) 추정이 의미가 없다. 검색 결과의 실제 최저가를 빼고 시작한다.
 */

/** 하루 끼니 수. 아침·점심·저녁. */
private const val MEALS_PER_DAY = 3

/**
 * 총예산을 항목별 상한으로 쪼갠다.
 *
 * 항공료를 먼저 실제 가격으로 빼는 것이 핵심이다. 남은 금액만 비율로 나누므로,
 * 항공권이 비싼 목적지에서는 숙소·식사 상한이 자동으로 내려간다.
 *
 * @param cheapestFlightWon 항공권 검색 결과의 최저가. 아직 검색 전이거나 결과가 없으면 null(0원으로 친다).
 */
fun allocateBudget(
    condition: TripCondition,
    cheapestFlightWon: Long?,
): BudgetAllocation {
    val total = condition.budgetWon
        ?: return BudgetAllocation(
            totalWon = null,
            flightWon = cheapestFlightWon,
            remainingWon = null,
            lodgingPerNightWon = null,
            mealWon = null,
            otherWon = null,
            flightAloneExceedsBudget = false,
        )

    val flight = cheapestFlightWon ?: 0L
    val remaining = (total - flight).coerceAtLeast(0L)

    val nights = nightsOf(condition)
    val meals = daysOf(condition) * MEALS_PER_DAY
    val profile = PurposeProfiles.of(condition.purposes)

    return BudgetAllocation(
        totalWon = total,
        flightWon = cheapestFlightWon,
        remainingWon = remaining,
        lodgingPerNightWon = (remaining * profile.lodgingShare).toLong() / nights,
        mealWon = (remaining * profile.foodShare).toLong() / meals,
        otherWon = (remaining * profile.otherShare).toLong(),
        flightAloneExceedsBudget = cheapestFlightWon != null && cheapestFlightWon > total,
    )
}

/**
 * 숙박 일수(박).
 *
 * 0으로 나누는 것을 막기 위해 최소 1로 본다. 당일치기(0박)면 호출부가 숙소 검색 자체를 건너뛰므로
 * 여기서 나온 1박 상한은 쓰이지 않는다.
 */
private fun nightsOf(condition: TripCondition): Long {
    val departure = condition.departureDate ?: return 1
    val returning = condition.returnDate ?: return 1
    return ChronoUnit.DAYS.between(departure, returning).coerceAtLeast(1)
}

/** 여행 일수. 4박이면 5일. */
private fun daysOf(condition: TripCondition): Long = nightsOf(condition) + 1