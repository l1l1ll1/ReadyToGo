package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 큰 사진 카드.
 *
 * **사진 자리를 색 그라데이션으로 채운다.** 실제 사진을 쓰려면 Google Places Photos가 필요한데
 * 호출마다 과금되는 별도 SKU라 아직 붙이지 않았다(사용자와 합의). 이미지 로딩 라이브러리도
 * 그래서 넣지 않았다. 사진을 붙일 때는 이 함수 안의 [Box] 배경만 교체하면 된다.
 *
 * 색은 [key]로 고르므로 같은 장소는 항상 같은 색을 받는다 — 스크롤마다 색이 바뀌면 어지럽다.
 *
 * @param key 색을 고르는 기준. 장소 id나 도시 이름처럼 항목마다 고정된 값을 넘긴다.
 * @param overlay 사진 위에 겹칠 것. ♥ 버튼이나 D-day 배지가 들어간다.
 */
@Composable
fun PhotoCard(
    key: String,
    title: String,
    subtitle: String? = null,
    height: Dp = Tokens.Size.photoCardHeight,
    shape: RoundedCornerShape = Tokens.Radius.card,
    onClick: (() -> Unit)? = null,
    /**
     * 길게 누를 때. 삭제처럼 **되돌릴 수 없는 동작**을 여기 둔다.
     * 버튼으로 카드 위에 올려두면 카드를 열려다 누르게 된다.
     */
    onLongClick: (() -> Unit)? = null,
    /**
     * 배경 색을 직접 정하고 싶을 때. 기본은 [key]로 고른 사진 대용 그라데이션이다.
     *
     * 홈의 "다가오는 여행" 카드가 노을을 쓰려고 열어 둔 구멍이다 — 그 카드는 목적지가
     * 무엇이든 브랜드 색으로 보여야 한다.
     */
    colors: List<Color>? = null,
    modifier: Modifier = Modifier,
    overlay: @Composable (BoxScope.() -> Unit)? = null,
) {
    val fill = colors ?: Tokens.placeholderGradient(key)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Brush.linearGradient(fill))
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        // 아래쪽을 어둡게 덮는다. 그라데이션 색에 따라 흰 글자가 안 보이는 경우를 막는다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        overlay?.invoke(this)
    }
}
