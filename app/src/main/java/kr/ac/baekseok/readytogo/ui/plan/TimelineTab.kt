package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.plan.TimelineEntry
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.TravelLeg
import kr.ac.baekseok.readytogo.domain.plan.TravelMode
import kr.ac.baekseok.readytogo.ui.format.dateWithDay
import kr.ac.baekseok.readytogo.ui.format.durationText
import kr.ac.baekseok.readytogo.ui.format.travelLegText
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.domain.purpose.PurposeAttractionTypes
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.FavoriteButton
import kr.ac.baekseok.readytogo.ui.component.TagChip
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 일정표 탭 — 날짜 칩으로 전환하는 세로 타임라인.
 *
 * 전에는 하루치를 카드 한 장에 "가볼 곳 / 아침 / 점심 / 저녁" 네 덩어리로 묶어 보여줬다.
 * 그러면 **실제로 움직이는 순서**가 보이지 않는다 — 아침 먹고 어디를 가는지, 두 곳 사이가
 * 걸어갈 거리인지 알 수 없었다. 관광지와 식당을 시간 순으로 섞고 사이에 이동시간을 넣는다.
 *
 * @param onDeleted 항목을 뺐을 때. 일차 번호와 항목 키를 넘긴다.
 */
@Composable
fun TimelineTab(
    plan: TripPlan,
    selectedDayNumber: Int,
    edits: TimelineEdits,
    favoriteIds: Set<String>,
    onSelectDay: (Int) -> Unit,
    onOpenMapForDay: (Int) -> Unit,
    onItemClick: (Int, TimelineItem) -> Unit,
    onToggleFavorite: (FavoritePlace) -> Unit,
) {
    val day = plan.dailyPlans.firstOrNull { it.dayNumber == selectedDayNumber }
        ?: plan.dailyPlans.firstOrNull()

    Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = ((day?.dayNumber ?: 1) - 1).coerceAtLeast(0),
            edgePadding = 0.dp,
        ) {
            plan.dailyPlans.forEach { candidate ->
                Tab(
                    selected = candidate.dayNumber == day?.dayNumber,
                    onClick = { onSelectDay(candidate.dayNumber) },
                    text = {
                        Text(stringResource(R.string.plan_day_tab, candidate.dayNumber))
                    },
                )
            }
        }

        if (day == null) {
            EmptyState(title = stringResource(R.string.plan_timeline_empty), description = "")
            return@Column
        }

        val entries = day.toTimeline(
            deletedKeys = edits.deletedIn(day.dayNumber),
            swappedMealKeys = edits.swappedIn(day.dayNumber),
        )

        LazyColumn(
            contentPadding = PaddingValues(
                start = Tokens.Space.lg,
                end = Tokens.Space.lg,
                top = Tokens.Space.md,
                bottom = Tokens.Space.xxl,
            ),
        ) {
            item {
                DayHeader(
                    day = day,
                    totalMinutes = entries.sumOf { entry ->
                        entry.item.stayMinutes + (entry.travelFromPrevious?.minutes ?: 0)
                    },
                    onOpenMap = { onOpenMapForDay(day.dayNumber) },
                )
            }

            if (entries.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.plan_day_empty_title),
                        description = stringResource(R.string.plan_day_empty_body),
                    )
                }
            }

            items(entries, key = { it.item.key }) { entry ->
                TimelineRow(
                    entry = entry,
                    favoriteIds = favoriteIds,
                    cityName = plan.condition.destination?.city.orEmpty(),
                    purposes = plan.condition.purposes,
                    // 추천이 없는 끼니는 보여줄 상세가 없다. 시트를 띄우지 않는다.
                    onClick = if (entry.item is TimelineItem.NoMeal) {
                        null
                    } else {
                        { onItemClick(day.dayNumber, entry.item) }
                    },
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(day: DayPlan, totalMinutes: Int, onOpenMap: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Tokens.Space.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.plan_day_header,
                    day.dayNumber,
                    dateWithDay(day.date),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            // 하루가 얼마나 빡빡한지 숫자로 알려준다. 항목만 세어서는 감이 안 온다.
            if (totalMinutes > 0) {
                Text(
                    text = stringResource(R.string.plan_day_total, durationText(totalMinutes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalPalette.current.TextSecondary,
                )
            }
        }
        TextButton(onClick = onOpenMap) { Text(stringResource(R.string.plan_open_map)) }
    }
}

/**
 * 타임라인 한 줄 — 왼쪽에 점과 세로줄, 오른쪽에 내용.
 *
 * 이동시간은 **항목 위**에 둔다. 아래에 두면 마지막 항목 뒤에 갈 곳 없는 이동이 남는다.
 */
@Composable
private fun TimelineRow(
    entry: TimelineEntry,
    favoriteIds: Set<String>,
    cityName: String,
    /** 목적 태그를 어떤 기준으로 붙일지. 비어 있으면 태그를 달지 않는다. */
    purposes: Set<TravelPurpose>,
    onClick: (() -> Unit)?,
    onToggleFavorite: (FavoritePlace) -> Unit,
) {
    Column {
        entry.travelFromPrevious?.let { TravelRow(it) }

        Row(modifier = Modifier.fillMaxWidth()) {
            TimelineRail(icon = entry.item.icon)

            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
                modifier = Modifier
                    .weight(1f)
                    .clip(Tokens.Radius.card)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(
                        start = Tokens.Space.md,
                        end = Tokens.Space.sm,
                        top = Tokens.Space.sm,
                        bottom = Tokens.Space.lg,
                    ),
            ) {
                when (val item = entry.item) {
                    is TimelineItem.Visit -> VisitContent(item, purposes)
                    is TimelineItem.Meal -> MealContent(item)
                    is TimelineItem.NoMeal -> NoMealContent(item)
                }
            }

            // ♥는 줄 전체 클릭과 겹치지 않게 Row의 바깥에 둔다.
            // 추천이 없는 끼니는 담을 것이 없어 자리만 비워 둔다.
            entry.item.toFavoritePlace(cityName)?.let { place ->
                FavoriteButton(
                    favorite = place.placeId in favoriteIds,
                    onToggle = { onToggleFavorite(place) },
                )
            }
        }
    }
}

/** 왼쪽 세로줄과 점. 항목이 한 줄로 이어져 보이게 한다. */
@Composable
private fun TimelineRail(icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(Tokens.Size.timelineRailWidth),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Tokens.Space.xxl)
                .clip(CircleShape)
                .background(LocalPalette.current.PointLight),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalPalette.current.PointDark,
                modifier = Modifier.size(Tokens.Space.lg),
            )
        }
        // 점 아래로 선을 늘려 다음 항목까지 이어 붙인다.
        Box(
            modifier = Modifier
                .width(Tokens.Size.timelineLineWidth)
                .height(Tokens.Size.timelineLineHeight)
                .background(LocalPalette.current.Divider),
        )
    }
}

@Composable
private fun VisitContent(item: TimelineItem.Visit, purposes: Set<TravelPurpose>) {
    val attraction = item.attraction
    Text(
        text = attraction.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )

    // **이 곳이 어떤 목적에 맞아서 들어왔는지** 보여준다. 점수에 목적 일치 가산점을 준
    // 바로 그 판정이라(AttractionScoring), 태그가 붙은 곳이 상위에 온 이유가 드러난다.
    // 맞는 목적이 없으면 붙일 이름이 없으므로 아무것도 그리지 않는다 — 평점·동선만으로
    // 올라온 곳도 있고, 그 자리에 "해당 없음" 같은 칸을 두면 흠처럼 보인다.
    val matched = PurposeAttractionTypes.matched(attraction.types, purposes)
    if (matched.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        ) {
            matched.forEach { purpose -> TagChip(text = stringResource(purpose.labelRes)) }
        }
    }

    Text(
        text = stringResource(R.string.plan_attraction) + " · " +
            if (attraction.reviewCount > 0) {
                stringResource(
                    R.string.plan_rating_reviews,
                    attraction.rating.toString(),
                    "%,d".format(attraction.reviewCount),
                )
            } else {
                "★${attraction.rating}"
            },
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
    Text(
        text = durationText(item.stayMinutes),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/**
 * 끼니 한 줄.
 *
 * 예산 초과 빨간 표시와 현지 통화 병기를 여기서도 유지한다 — 카드에서만 보이고 타임라인에서
 * 사라지면 같은 식당이 화면에 따라 다르게 보인다. 표기 함수는 결과 화면과 공유한다.
 */
@Composable
private fun MealContent(item: TimelineItem.Meal) {
    val restaurant = item.restaurant
    val overBudget = item.shown.overBudget
    // 예산 초과는 코랄. 붉은 Danger는 되돌릴 수 없는 일에만 쓴다(SummaryTab 주석 참고).
    val accent = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(item.meal.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        if (overBudget) {
            Text(
                text = stringResource(R.string.plan_over_budget_suffix),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
        }
    }
    Text(
        text = restaurant.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = restaurant.detailLine(),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
    Text(
        // priceText()가 원화와 현지 통화를 줄바꿈으로 함께 돌려준다.
        text = restaurant.priceText(),
        style = MaterialTheme.typography.bodyMedium,
        color = if (overBudget) accent else LocalPalette.current.TextPrimary,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = durationText(item.stayMinutes),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/**
 * 추천을 못 찾은 끼니.
 *
 * 왜 없는지 적는다 — 그냥 "없음"만 두면 앱이 빠뜨린 것으로 보인다.
 * 반경을 넓혀 다시 찾아도 없었다는 뜻이라, 사용자가 직접 찾아보는 수밖에 없다.
 */
@Composable
private fun NoMealContent(item: TimelineItem.NoMeal) {
    Text(
        text = stringResource(item.meal.labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(R.string.plan_no_meal_title),
        style = MaterialTheme.typography.titleSmall,
        color = LocalPalette.current.TextSecondary,
    )
    Text(
        text = stringResource(R.string.plan_no_meal_body),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/** 두 항목 사이의 이동. 세로줄 자리에 아이콘을 맞춰 줄이 끊기지 않게 한다. */
@Composable
private fun TravelRow(leg: TravelLeg) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Tokens.Space.sm),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(Tokens.Size.timelineRailWidth),
        ) {
            Icon(
                imageVector = when (leg.mode) {
                    TravelMode.WALK -> Icons.Outlined.DirectionsWalk
                    TravelMode.TRANSIT -> Icons.Outlined.DirectionsBus
                },
                contentDescription = null,
                tint = LocalPalette.current.TextSecondary,
                modifier = Modifier.size(Tokens.Space.lg),
            )
        }
        Text(
            text = "${travelLegText(leg)} · ${"%.1f".format(leg.distanceKm)}km",
            style = MaterialTheme.typography.labelMedium,
            color = LocalPalette.current.TextSecondary,
            modifier = Modifier.padding(start = Tokens.Space.md),
        )
    }
}

private val TimelineItem.icon: ImageVector
    get() = when (this) {
        is TimelineItem.Visit -> Icons.Outlined.Place
        is TimelineItem.Meal -> Icons.Outlined.LocalCafe
        is TimelineItem.NoMeal -> Icons.Outlined.HelpOutline
    }

private fun TimelineItem.toFavoritePlace(cityName: String): FavoritePlace? = when (this) {
    is TimelineItem.Visit -> attraction.toFavorite(cityName)
    is TimelineItem.Meal -> restaurant.toFavorite(cityName)
    // 담을 장소가 없다. 호출부가 이 항목에는 하트를 그리지 않는다.
    is TimelineItem.NoMeal -> null
}
