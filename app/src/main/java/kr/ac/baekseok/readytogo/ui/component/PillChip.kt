package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 고르는 칩(예산 빠른 선택·여행 목적·저장 탭 분류).
 *
 * 결과 화면의 세그먼트 탭·날짜 칩과 같은 말투로 맞춘다 — 고른 것은 남색 바탕 흰 글자, 안 고른 것은
 * 흰 바탕, 테두리 없음. M3 기본(연노랑 + 회색 테두리)을 두면 화면마다 "고른 상태"가 두 가지 색으로 보인다.
 */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    // 흰 카드 안에서는 흰 칩이 사라지므로 배경색을 넘긴다.
    container: Color = LocalPalette.current.Surface,
) {
    val palette = LocalPalette.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text, maxLines = 1) },
        leadingIcon = icon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        },
        shape = Tokens.Radius.chip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            labelColor = palette.TextPrimary,
            iconColor = palette.PointDark,
            selectedContainerColor = palette.TextPrimary,
            selectedLabelColor = palette.Surface,
            selectedLeadingIconColor = palette.Surface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
        ),
    )
}
