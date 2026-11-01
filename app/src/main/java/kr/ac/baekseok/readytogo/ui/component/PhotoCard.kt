package kr.ac.baekseok.readytogo.ui.component

import kr.ac.baekseok.readytogo.ui.theme.pressScale
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
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
    /**
     * 사진을 띄울 장소 id. null이면 그라데이션만 남는다.
     *
     * 설정에서 장소 사진을 끄면 [rememberPlacePhoto]가 null을 돌려주므로 자동으로
     * 그라데이션으로 떨어진다 — 이 쪽에서 설정을 따로 보지 않는다.
     *
     * 사진은 카드가 **화면에 보일 때** 한 번만 받고 기기에 캐시된다. 목록을 다시 열거나
     * 앱을 재시작해도 호출이 나가지 않는다.
     */
    photoPlaceId: String? = null,
    /**
     * 아래쪽을 어둡게 덮을지. 배경 색을 모르는 카드(사진 대용 그라데이션)는 켜 둬야
     * 흰 글자가 밝은 색 위에 얹히는 일을 막는다.
     *
     * **배경 색을 직접 정한 카드는 끈다.** 브랜드 색을 깔아 놓고 그 위에 검은 덮개를
     * 씌우면 공들여 고른 색이 아래쪽에서 탁해진다. 그 경우 글자는 [TITLE_SHADOW]가 지킨다.
     */
    scrim: Boolean = true,
    modifier: Modifier = Modifier,
    /**
     * 사진이 없을 때 바탕 위에 그리는 장식(홈 히어로의 원 무늬, ⑫). 사진보다 아래에 그려서
     * 사진이 도착하면 저절로 가려진다 — 이 쪽에서 사진 유무를 따로 보지 않는다.
     */
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    /**
     * 글자를 [title] 대신 [overlay]에 직접 그리는 카드(홈 히어로). 사진이 얹히면 덮개를 켜야
     * 흰 글자가 읽힌다 — 덮개는 원래 제목이 있을 때만 깔리므로 따로 알려 준다.
     */
    overlayHasText: Boolean = false,
    overlay: @Composable (BoxScope.() -> Unit)? = null,
) {
    val fill = colors ?: Tokens.placeholderGradient(key)
    val photo = photoPlaceId?.let { rememberPlacePhoto(it) }

    // 누르는 동안 카드 전체가 살짝 줄어든다(⑫). 길게 누르기(삭제)도 같은 눌림을 쓴다.
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(interaction)
            .clip(shape)
            .background(Brush.linearGradient(fill))
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        decoration?.invoke(this)

        // 사진이 도착하면 그라데이션 위에 덮는다. 그라데이션을 먼저 깔아 두었기 때문에
        // 사진이 늦어도 빈 네모가 보이지 않는다.
        if (photo != null) {
            AsyncImage(
                model = photo.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        // 아래쪽을 어둡게 덮는다. 그라데이션 색에 따라 흰 글자가 안 보이는 경우를 막는다.
        //
        // **글자가 없으면 깔지 않는다.** 온보딩처럼 제목 없이 그림 자리로만 쓰는 카드에서는
        // 지킬 글자가 없는데, 노을 그라데이션 위에 검은 덮개가 겹쳐 아래쪽이 탁해졌다.
        // 사진을 얹었으면 덮개를 켠다 — 사진이 밝으면 흰 글자가 묻힌다.
        // 배경 색을 직접 정한 카드라도 그 색은 이미 사진에 가려져 있다.
        if ((scrim || photo != null) && (title.isNotEmpty() || subtitle != null || overlayHasText)) {
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
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        ) {
            // 말줄임·넘김 없이 한 줄(⑫). 길면 글자를 조금 줄인다(FitText).
            if (title.isNotEmpty()) {
                FitText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(shadow = PhotoTextShadow),
                    color = Color.White,
                )
            }
            if (subtitle != null) {
                FitText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = PhotoTextShadow),
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        overlay?.invoke(this)
    }
}

/**
 * 카드 위 흰 글자에 까는 그림자.
 *
 * 덮개 없이도 글자가 읽히게 하는 장치다. 사진 위 출처 글자도 같은 것을 쓴다. 번짐을 4로 얕게 둬서 글자 테두리만
 * 살짝 어두워지고 배경 색은 그대로 보인다 — 더 키우면 글자 주변만 때처럼 뭉친다.
 */
internal val PhotoTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.35f),
    offset = Offset(0f, 1f),
    blurRadius = 4f,
)
