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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.formatDuration
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
        containerColor = Tokens.Palette.Background,
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
            }

            HorizontalDivider(color = Tokens.Palette.Divider)

            val mapUrl = item.mapUrl
            if (mapUrl.isNotBlank()) {
                SheetAction(
                    icon = Icons.Outlined.Map,
                    label = "지도에서 열기",
                    onClick = {
                        onOpenMap(mapUrl)
                        onDismiss()
                    },
                )
            }

            if (onSwap != null) {
                SheetAction(
                    icon = Icons.Outlined.SwapHoriz,
                    label = "대안으로 교체",
                    onClick = {
                        onSwap()
                        onDismiss()
                    },
                )
            }

            SheetAction(
                icon = Icons.Outlined.Delete,
                label = "일정에서 빼기",
                tint = Tokens.Palette.Danger,
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
            text = "관광지 · ★${attraction.rating}" +
                if (attraction.reviewCount > 0) {
                    " (리뷰 ${"%,d".format(attraction.reviewCount)})"
                } else {
                    ""
                },
            style = MaterialTheme.typography.bodyLarge,
            color = Tokens.Palette.TextSecondary,
        )
        if (attraction.address.isNotBlank()) {
            Text(
                text = attraction.address,
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.Palette.TextSecondary,
            )
        }
        Text(
            text = "머무는 시간 ${formatDuration(item.stayMinutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.Palette.TextSecondary,
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
                text = item.meal.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (overBudget) Tokens.Palette.Danger else Tokens.Palette.Point,
                fontWeight = FontWeight.Bold,
            )
            if (overBudget) {
                Text(
                    text = " · 예산 초과",
                    style = MaterialTheme.typography.labelLarge,
                    color = Tokens.Palette.Danger,
                )
            }
        }
        Text(text = restaurant.name, style = MaterialTheme.typography.titleLarge)
        // detailLine에 카테고리·평점·주소가 이미 들어 있다. 주소를 따로 또 찍으면 두 번 나온다.
        Text(
            text = restaurant.detailLine(),
            style = MaterialTheme.typography.bodyLarge,
            color = Tokens.Palette.TextSecondary,
        )
        Text(
            text = restaurant.priceText(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (overBudget) Tokens.Palette.Danger else Tokens.Palette.TextPrimary,
        )

        // 교체하면 무엇으로 바뀌는지 미리 알려준다. 누르고 나서 알게 하면 되돌리게 된다.
        item.other?.let { alternative ->
            Text(
                text = "교체하면 → ${alternative.item.name} (${alternative.item.priceText()
                    .replace("\n", " · ")})",
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.Palette.TextSecondary,
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Tokens.Palette.TextPrimary,
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

/** 지도 앱으로 넘길 주소. 관광지는 Places URL을 들고 있지 않아 비어 있을 수 있다. */
private val TimelineItem.mapUrl: String
    get() = when (this) {
        is TimelineItem.Visit -> ""
        is TimelineItem.Meal -> restaurant.mapUrl
    }
