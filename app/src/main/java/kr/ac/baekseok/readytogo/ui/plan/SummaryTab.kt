package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kr.ac.baekseok.readytogo.data.model.TripPlan
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
            .background(Tokens.Palette.SurfaceMuted)
            .padding(Tokens.Space.lg),
    ) {
        Text(
            text = "${c.origin?.city ?: "-"} → ${c.destination?.city ?: "-"}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = listOfNotNull(
                c.destination?.country,
                c.departureDate?.let { departure ->
                    c.returnDate?.let { back -> "${departure.format(DATE)} – ${back.format(DATE)}" }
                },
                "${plan.dailyPlans.size}일",
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.Palette.TextSecondary,
        )
        if (c.purposes.isNotEmpty()) {
            Text(
                text = c.purposes.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.Palette.Point,
            )
        }

        val total = b.totalWon
        val planned = plan.plannedSpendWon()
        if (total != null) {
            HorizontalDivider(
                color = Tokens.Palette.Divider,
                modifier = Modifier.padding(vertical = Tokens.Space.sm),
            )
            BudgetUsageBar(totalWon = total, plannedWon = planned)
        }
    }
}

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
    val color = if (overBudget) MaterialTheme.colorScheme.error else Tokens.Palette.Point

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "예산 사용",
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Palette.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${plannedWon.won()} / ${totalWon.won()} (${(ratio * 100).toInt()}%)",
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
                .background(Tokens.Palette.Divider),
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
                text = "계획이 예산을 넘습니다. 예산 초과로 표시된 항목을 대안으로 바꿔보세요.",
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
                    color = Tokens.Palette.Point,
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
                                " · 리뷰 ${"%,d".format(attraction.reviewCount)}"
                            } else {
                                ""
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Tokens.Palette.TextSecondary,
                    )
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
    val flight = flights.minOfOrNull { it.priceWon } ?: 0L

    val nights = (dailyPlans.size - 1).coerceAtLeast(0)
    val lodgingTotal = (lodging?.qualityFirst?.item?.priceWon ?: 0L) * nights

    val meals = dailyPlans.sumOf { day ->
        day.meals.values.sumOf { it.qualityFirst.item.priceWon }
    }

    return flight + lodgingTotal + meals + (budget.otherWon ?: 0L)
}

private const val HIGHLIGHT_COUNT = 3
