package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.ui.format.adultsText
import kr.ac.baekseok.readytogo.ui.format.dateWithDay
import kr.ac.baekseok.readytogo.ui.format.daysText
import kr.ac.baekseok.readytogo.ui.format.roomsText
import kr.ac.baekseok.readytogo.ui.format.durationText
import kr.ac.baekseok.readytogo.ui.format.money
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.ui.component.PhotoAttributionLine
import kr.ac.baekseok.readytogo.ui.component.PlacePhotoThumb
import kr.ac.baekseok.readytogo.ui.component.TagChip
import kr.ac.baekseok.readytogo.ui.component.rememberPlacePhoto
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 요약 탭의 맨 위 — 목적지·기간·예산 사용률.
 *
 * 사용률 바를 둔 이유: 예산 배분 금액만 나열하면 "150만원 중 얼마를 쓰는 계획인가"를
 * 사용자가 더해 봐야 한다. 한 줄로 보여주면 예산이 빡빡한지 여유로운지 바로 보인다.
 */
@Composable
fun SummaryHeader(plan: TripPlan) {
    val c = plan.condition
    val b = plan.budget

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(LocalPalette.current.SurfaceMuted)
            .padding(Tokens.Space.lg),
    ) {
        Text(
            text = "${c.origin?.shortName ?: "-"} → ${c.destination?.shortName ?: "-"}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = listOfNotNull(
                c.destination?.localizedCountry,
                c.departureDate?.let { departure ->
                    c.returnDate?.let { back -> "${dateWithDay(departure)} – ${dateWithDay(back)}" }
                },
                daysText(plan.dailyPlans.size),
                // 방 수까지 적는 이유: 숙소 예산이 왜 그 금액인지가 인원만으로는 안 보인다.
                adultsText(c.adults) +
                    if (c.roomCount > 1) " · " + roomsText(c.roomCount) else "",
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalPalette.current.TextSecondary,
        )
        // **목적은 글자가 아니라 배지로 둔다.** 한 줄 글자로 적으면 바로 위 날짜·인원 줄과
        // 같은 무게로 읽혀서, 이 계획을 무엇에 맞춰 짰는지가 묻힌다. 예산 배분과 하루
        // 관광지 수가 전부 여기서 갈리므로 헤더에서 가장 먼저 눈에 걸려야 한다.
        if (c.purposes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            ) {
                // 목적 순서는 선언 순서로 고정한다. Set 순회 순서에 맡기면 같은 조합인데
                // 화면을 다시 열 때마다 배지 차례가 바뀐다.
                TravelPurpose.entries.filter { it in c.purposes }.forEach { purpose ->
                    TagChip(text = stringResource(purpose.labelRes))
                }
            }
        }

        PlanDensityLine(plan)

        val total = b.totalWon
        val planned = plan.plannedSpendWon()
        if (total != null) {
            HorizontalDivider(
                color = LocalPalette.current.Divider,
                modifier = Modifier.padding(vertical = Tokens.Space.sm),
            )
            BudgetUsageBar(totalWon = total, plannedWon = planned)
            TightMealBudgetNotice(plan)
        }
    }
}

/**
 * 하루 일정이 얼마나 빡빡한지 한 줄.
 *
 * **목적이 예산만 바꾸는 게 아니라는 걸 보여주는 자리다.** 배분 막대가 돈 쪽을 맡고,
 * 이 줄이 시간 쪽을 맡는다. 휴양은 하루 2곳, 관광은 4곳이라 같은 5일이어도 일정 밀도가
 * 달라지는데, 그 차이가 숫자로 안 보이면 목적을 왜 골랐는지 알 수 없다.
 *
 * 값은 **실제로 배치된 일정에서 센다.** PurposeProfile의 설정값을 그대로 적으면
 * 도착일·출국일(1곳 고정)이나 후보가 모자라 덜 들어간 날을 못 보여준다.
 *
 * 온전한 날이 없으면(1박 2일) 관광지 수를 적지 않는다 — 전부 이동하는 날이라
 * "하루 1곳"이 목적 때문인 것처럼 읽힌다.
 */
@Composable
private fun PlanDensityLine(plan: TripPlan) {
    // 도착일·출국일은 목적과 무관하게 1곳이다(CLAUDE.md). 평균에 넣으면 밀도가 낮게 나온다.
    val fullDays = plan.dailyPlans.drop(1).dropLast(1)

    val perDay = fullDays
        .takeIf { it.isNotEmpty() }
        ?.let { days -> days.sumOf { it.attractions.size }.toDouble() / days.size }

    // 이동 시간은 전체가 아니라 하루 평균으로 적는다. 5일 합계 "25시간"은 큰 수로만 보이고
    // 하루가 어떤지 가늠이 안 된다.
    val minutesPerDay = plan.dailyPlans
        .takeIf { it.isNotEmpty() }
        ?.let { days ->
            days.sumOf { day ->
                day.toTimeline().sumOf { it.item.stayMinutes + (it.travelFromPrevious?.minutes ?: 0) }
            } / days.size
        }

    val parts = listOfNotNull(
        perDay?.let {
            // 3.0곳이 아니라 3곳으로 적는다. 소수점이 붙으면 추정값처럼 보인다.
            val count = "%.1f".format(it).removeSuffix(".0")
            stringResource(R.string.plan_density_attractions, count)
        },
        minutesPerDay?.takeIf { it > 0 }?.let {
            stringResource(R.string.plan_density_travel, durationText(it))
        },
    )
    if (parts.isEmpty()) return

    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/**
 * 식당 추천 대부분이 예산을 넘겼을 때 띄우는 안내.
 *
 * **왜 필요한가**: 카드마다 "예산 초과"가 빨갛게 붙어 있으면 앱이 추천을 잘못한 것처럼 보인다.
 * 실제로는 예산이 그 도시 물가에 비해 빠듯한 것이고, 그건 사용자만 고칠 수 있다.
 * 실측에서 도쿄 7일·3명·450만원이면 1끼 상한이 16,681원인데 현지 식당 대부분이
 * 17,000원을 넘어 42칸 중 35칸이 초과로 찍혔다.
 *
 * 몇 칸이 넘었는지가 아니라 **1끼에 쓸 수 있는 금액**을 알려주는 이유: 숫자를 봐야
 * 예산을 얼마나 올려야 하는지 가늠할 수 있다.
 */
@Composable
private fun TightMealBudgetNotice(plan: TripPlan) {
    val mealCap = plan.budget.mealWon ?: return

    val picks = plan.dailyPlans.flatMap { day ->
        day.meals.values.flatMap { listOfNotNull(it.qualityFirst, it.withinBudget) }
    }
    if (picks.isEmpty()) return

    val overRatio = picks.count { it.overBudget }.toDouble() / picks.size
    if (overRatio <= MEAL_OVER_BUDGET_WARNING_RATIO) return

    Text(
        text = stringResource(R.string.plan_meal_budget_tight, formatRoughWon(mealCap)),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
        modifier = Modifier.padding(top = Tokens.Space.sm),
    )
}

/**
 * "약 16,700원" — 백원 단위로 끊는다.
 *
 * 나눠서 나온 값이라 끝자리에 의미가 없다. 16,681원을 그대로 적으면 정확한 값으로 읽힌다.
 */
@Composable
private fun formatRoughWon(won: Long): String = money((won + 50) / 100 * 100)

/**
 * 예산 사용률 바.
 *
 * 100%를 넘으면 막대를 꽉 채우고 색을 오류 색으로 바꾼다 — 막대가 칸을 넘어 그려지면
 * 레이아웃이 깨지고, 넘친 정도는 숫자로 읽는 게 정확하다.
 */
@Composable
private fun BudgetUsageBar(totalWon: Long, plannedWon: Long) {
    val ratio = (plannedWon.toFloat() / totalWon).coerceAtLeast(0f)
    val overBudget = ratio > 1f
    // 예산 초과는 **코랄**이다. 붉은 Danger는 삭제·탈퇴처럼 되돌릴 수 없는 일에만 쓴다 —
    // 예산을 넘은 건 잘못이 아니라 "눈에 걸려야 할 것"이다.
    val color = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.plan_budget_usage),
                style = MaterialTheme.typography.labelMedium,
                color = LocalPalette.current.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.plan_budget_usage_value,
                    money(plannedWon),
                    money(totalWon),
                    (ratio * 100).toInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Tokens.Space.sm)
                .clip(Tokens.Radius.chip)
                .background(LocalPalette.current.Divider),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceAtMost(1f))
                    .height(Tokens.Space.sm)
                    .clip(Tokens.Radius.chip)
                    .background(color),
            )
        }

        if (overBudget) {
            Text(
                text = stringResource(R.string.plan_budget_over),
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

/** 일정 하이라이트 — 평점 높은 관광지 세 곳. 요약에서 "뭘 보러 가는지"가 바로 보이게 한다. */
@Composable
fun ItineraryHighlights(plan: TripPlan) {
    val top = plan.dailyPlans
        .flatMap { it.attractions }
        .distinctBy { it.id }
        .sortedByDescending { it.rating }
        .take(HIGHLIGHT_COUNT)

    if (top.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm)) {
        top.forEachIndexed { index, attraction ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalPalette.current.Point,
                )

                // 요약에서 "뭘 보러 가는지"를 글자보다 사진이 먼저 알려준다.
                // 사진을 붙이는 자리를 여기 셋으로 묶은 이유는 과금 때문이다 —
                // GooglePlacesPhotoRepository 주석 참고.
                val photo = rememberPlacePhoto(attraction.id)
                PlacePhotoThumb(
                    placeId = attraction.id,
                    photo = photo,
                    size = Tokens.Size.placeThumb,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attraction.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "★${attraction.rating}" +
                            if (attraction.reviewCount > 0) {
                                " · " + stringResource(
                                    R.string.plan_reviews,
                                    "%,d".format(attraction.reviewCount),
                                )
                            } else {
                                ""
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalPalette.current.TextSecondary,
                    )
                    PhotoAttributionLine(photo)
                }
            }
        }
    }
}

/**
 * 이 계획을 그대로 가면 쓸 금액.
 *
 * **배분 상한을 더하지 않는다.** 상한을 더하면 항상 총예산과 같아져 사용률 바가 늘 100%가 된다.
 * 추천된 항목의 실제 가격을 더해야 "여유가 있나"가 보인다.
 *
 * - 항공: 검색 결과 최저가(왕복 총액)
 * - 숙소: 1박가 × 숙박 일수(일정 일수 - 1)
 * - 식사: 추천 쪽 가격의 합. 식당 가격은 상한 기준이라 실제보다 넉넉하게 잡힌다
 * - 현지 교통·입장료는 배분 몫을 그대로 더한다. 실제 값을 알 방법이 없다
 */
private fun TripPlan.plannedSpendWon(): Long {
    // 항공은 이미 인원 전체 총액이다(SerpApi에 인원을 넘겨 받는다). 다시 곱하면 안 된다.
    val flight = flights.minOfOrNull { it.priceWon } ?: 0L

    // 숙소 가격은 1박 1실이다. 방 수와 박수를 둘 다 곱해야 전체 숙박비가 된다.
    val nights = (dailyPlans.size - 1).coerceAtLeast(0)
    val lodgingTotal =
        (lodging?.qualityFirst?.item?.priceWon ?: 0L) * nights * condition.roomCount

    // 식당 가격은 1인분이다. 인원을 곱한다.
    val meals = dailyPlans.sumOf { day ->
        day.meals.values.sumOf { it.qualityFirst.item.priceWon }
    } * condition.adults

    return flight + lodgingTotal + meals + (budget.otherWon ?: 0L)
}

private const val HIGHLIGHT_COUNT = 3

/**
 * 식당 추천 중 이 비율을 넘게 예산을 초과하면 안내를 띄운다.
 *
 * 절반으로 잡은 이유: 한두 끼가 넘는 것은 "조금 비싼 집을 골랐다"는 뜻이고 그건 선택지의 역할이다.
 * 절반을 넘기면 예산 자체가 그 도시 물가와 맞지 않는다는 신호다.
 */
private const val MEAL_OVER_BUDGET_WARNING_RATIO = 0.5
