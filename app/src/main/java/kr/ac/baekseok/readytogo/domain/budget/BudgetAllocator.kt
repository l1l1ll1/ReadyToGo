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
    val adults = condition.adults
    val rooms = condition.roomCount

    val total = condition.budgetWon
        ?: return BudgetAllocation(
            totalWon = null,
            flightWon = cheapestFlightWon,
            remainingWon = null,
            lodgingPerNightWon = null,
            mealWon = null,
            otherWon = null,
            flightAloneExceedsBudget = false,
            adults = adults,
            roomCount = rooms,
        )

    val flight = cheapestFlightWon ?: 0L
    val remaining = (total - flight).coerceAtLeast(0L)

    val nights = nightsOf(condition)
    val profile = PurposeProfiles.of(condition.purposes)

    // 인원이 늘면 쓰는 양이 늘어난다. 총예산은 그대로이므로 **1인·1실 상한이 내려간다.**
    // 식당 가격은 1인 기준, 숙소 가격은 1실 기준으로 들어오므로 상한도 같은 기준으로 맞춘다.
    // 그러지 않으면 4명 여행에서 1인 10만원짜리 식당이 예산 안으로 잡힌다.
    val mealSlots = daysOf(condition) * MEALS_PER_DAY * adults
    val roomNights = nights * rooms

    return BudgetAllocation(
        totalWon = total,
        flightWon = cheapestFlightWon,
        remainingWon = remaining,
        lodgingPerNightWon = (remaining * profile.lodgingShare).toLong() / roomNights,
        mealWon = (remaining * profile.foodShare).toLong() / mealSlots,
        // 교통·입장료는 1인당이 아니라 일행 전체 몫이다. 화면에 안내용으로만 쓴다.
        otherWon = (remaining * profile.otherShare).toLong(),
        flightAloneExceedsBudget = cheapestFlightWon != null && cheapestFlightWon > total,
        adults = adults,
        roomCount = rooms,
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