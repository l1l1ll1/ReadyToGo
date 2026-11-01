package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.domain.review.ReviewIssue
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 검토 카드 한 줄에 필요한 것.
 *
 * @param fact 규칙이 쓴 사실 문장. AI가 없어도(둘러보기·실패) 무엇이 문제인지는 이것으로 보인다.
 * @param why AI 설명. 아직 묻지 않았거나 빈 답이면 null.
 */
data class ReviewItemUi(
    val issue: ReviewIssue,
    val fact: String,
    val why: String?,
)

/** 요약 탭 검토 카드의 상태와 동작. 화면 함수 인자가 늘지 않게 묶는다([TextEditControls]와 같은 방식). */
data class ReviewControls(
    val items: List<ReviewItemUi> = emptyList(),
    val loading: Boolean = false,
    /**
     * 펼쳐 있는지. **카드 안이 아니라 ViewModel이 든다.** 문제가 0건이 되면 카드가 화면에서 빠지는데,
     * 카드 안의 상태는 그때 함께 사라져 되돌리기로 문제가 돌아오면 접힌 채로 나왔다.
     */
    val expanded: Boolean = false,
    /** 머리를 눌렀을 때. 펼치는 순간 AI 호출이 한 번 나간다(답 없는 문제만). */
    val onToggle: () -> Unit = {},
    val onFix: (ReviewIssue) -> Unit = {},
)

/**
 * 요약 탭의 "검토 N건" 카드. 접혀 있다가 펼치면 항목마다 설명과 "고치기".
 *
 * **처음엔 접어 둔다.** 펼칠 때 AI를 부르므로, 요약만 보고 지나가는 사용자에게 호출이 나가지 않는다.
 * 문제가 없으면 호출부가 카드를 아예 그리지 않는다.
 */
@Composable
fun ReviewCard(controls: ReviewControls) {
    val palette = LocalPalette.current
    val expanded = controls.expanded

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(palette.SurfaceMuted),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = controls.onToggle)
                .padding(Tokens.Space.lg),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.review_card_title, controls.items.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.Warning,
                )
                Text(
                    text = stringResource(R.string.review_card_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.TextSecondary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = palette.TextSecondary,
                modifier = Modifier.size(Tokens.Size.icon),
            )
        }

        if (expanded) {
            controls.items.forEach { item ->
                HorizontalDivider(color = palette.Divider)
                ReviewRow(item, controls.loading, onFix = { controls.onFix(item.issue) })
            }
        }
    }
}

@Composable
private fun ReviewRow(item: ReviewItemUi, loading: Boolean, onFix: () -> Unit) {
    val palette = LocalPalette.current

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Tokens.Space.lg, end = Tokens.Space.sm, top = Tokens.Space.md),
    ) {
        Text(
            text = item.fact,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = Tokens.Space.sm),
        )
        // 설명 자리는 답이 올 때까지 "불러오는 중"을 둔다. 비워 두면 답이 와서 줄이 생길 때 화면이 튄다.
        val why = item.why?.takeIf { it.isNotBlank() }
        when {
            why != null -> Text(
                text = why,
                style = MaterialTheme.typography.bodySmall,
                color = palette.TextSecondary,
                modifier = Modifier.padding(end = Tokens.Space.sm),
            )
            loading -> Text(
                text = stringResource(R.string.review_loading),
                style = MaterialTheme.typography.bodySmall,
                color = palette.TextSecondary,
            )
        }
        TextButton(onClick = onFix, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.review_fix), color = palette.Point)
        }
    }
}
