package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.ui.format.durationText
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 타임라인 항목을 눌렀을 때 뜨는 상세 시트.
 *
 * 세 가지를 할 수 있다 — 지도에서 열기, 대안으로 교체, 일정에서 빼기.
 * 별도 화면으로 만들지 않은 이유: 하루에 항목이 대여섯 개라 들어갔다 나오는 일이 잦다.
 *
 * @param onSwap 대안이 없으면 null. 버튼을 숨겨야 한다 — 눌려도 아무 일이 없으면 고장으로 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailSheet(
    item: TimelineItem,
    onOpenMap: (String) -> Unit,
    onSwap: (() -> Unit)?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalPalette.current.Background,
        shape = Tokens.Radius.sheet,
    ) {
        Column(
            modifier = Modifier.padding(
                start = Tokens.Space.screenHorizontal,
                end = Tokens.Space.screenHorizontal,
                bottom = Tokens.Space.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            when (item) {
                is TimelineItem.Visit -> VisitDetail(item)
                is TimelineItem.Meal -> MealDetail(item)
                // 추천이 없는 끼니는 상세를 띄우지 않는다. 호출부가 막지만 타입상 분기가 필요하다.
                is TimelineItem.NoMeal -> Text(
                    text = stringResource(R.string.plan_sheet_no_restaurant),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider(color = LocalPalette.current.Divider)

            val mapUrl = item.mapUrl
            if (mapUrl.isNotBlank()) {
                SheetAction(
                    icon = Icons.Outlined.Map,
                    label = stringResource(R.string.plan_sheet_open_map),
                    onClick = {
                        onOpenMap(mapUrl)
                        onDismiss()
                    },
                )
            }

            if (onSwap != null) {
                SheetAction(
                    icon = Icons.Outlined.SwapHoriz,
                    label = stringResource(R.string.plan_sheet_swap),
                    onClick = {
                        onSwap()
                        onDismiss()
                    },
                )
            }

            SheetAction(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.plan_sheet_remove),
                tint = LocalPalette.current.Danger,
                onClick = {
                    onDelete()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun VisitDetail(item: TimelineItem.Visit) {
    val attraction = item.attraction
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Text(text = attraction.name, style = MaterialTheme.typography.titleLarge)
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
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPalette.current.TextSecondary,
        )
        if (attraction.address.isNotBlank()) {
            Text(
                text = attraction.address,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
            )
        }
        Text(
            text = stringResource(R.string.plan_sheet_stay_time, durationText(item.stayMinutes)),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalPalette.current.TextSecondary,
        )
    }
}

@Composable
private fun MealDetail(item: TimelineItem.Meal) {
    val restaurant = item.restaurant
    val overBudget = item.shown.overBudget

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(item.meal.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point,
                fontWeight = FontWeight.Bold,
            )
            if (overBudget) {
                Text(
                    text = stringResource(R.string.plan_over_budget_suffix),
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalPalette.current.Accent,
                )
            }
        }
        Text(text = restaurant.name, style = MaterialTheme.typography.titleLarge)
        // detailLine에 카테고리·평점·주소가 이미 들어 있다. 주소를 따로 또 찍으면 두 번 나온다.
        Text(
            text = restaurant.detailLine(),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPalette.current.TextSecondary,
        )
        Text(
            text = restaurant.priceText(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.TextPrimary,
        )

        // 교체하면 무엇으로 바뀌는지 미리 알려준다. 누르고 나서 알게 하면 되돌리게 된다.
        item.other?.let { alternative ->
            Text(
                text = "교체하면 → ${alternative.item.name} (${alternative.item.priceText()
                    .replace("\n", " · ")})",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = LocalPalette.current.TextPrimary,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}

/** 지도 앱으로 넘길 링크. Mock 데이터에는 없어서 비어 있을 수 있고, 그때는 버튼을 숨긴다. */
private val TimelineItem.mapUrl: String
    get() = when (this) {
        is TimelineItem.Visit -> attraction.mapUrl
        is TimelineItem.Meal -> restaurant.mapUrl
        is TimelineItem.NoMeal -> ""
    }
