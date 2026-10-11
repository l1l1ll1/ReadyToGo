package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.PlacePhoto
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 장소 사진을 찾아 둔다. 아직 못 받았거나 없으면 null.
 *
 * **카드가 화면에 보일 때 그 카드만 물어본다.** 사진은 요청마다 과금이라 계획을 만들 때
 * 미리 받아 두지 않는다. 스크롤해서 지나치지 않은 카드는 요청도 나가지 않는다.
 *
 * placeId로 기억하므로 같은 장소가 다시 그려져도 다시 묻지 않는다.
 */
@Composable
fun rememberPlacePhoto(placeId: String): PlacePhoto? {
    var photo by remember(placeId) { mutableStateOf<PlacePhoto?>(null) }

    LaunchedEffect(placeId) {
        if (placeId.isBlank()) return@LaunchedEffect
        photo = AppContainer.placePhotos.photoFor(placeId)
    }

    return photo?.takeIf { it.hasPhoto }
}

/**
 * 장소 사진 썸네일. 사진이 없으면 사진 대용 그라데이션이 그대로 남는다.
 *
 * 그라데이션을 **항상 먼저 깐다.** 사진이 늦게 도착하므로 그동안 빈 네모가 보이면
 * 목록이 깜빡이는 것처럼 보인다. 색은 [Tokens.placeholderGradient]가 장소마다 고정해 준다.
 */
@Composable
fun PlacePhotoThumb(
    placeId: String,
    photo: PlacePhoto?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(Tokens.Radius.image)
            .background(Brush.linearGradient(Tokens.placeholderGradient(placeId))),
    ) {
        if (photo?.hasPhoto == true) {
            AsyncImage(
                model = photo.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * 사진을 올린 사람. **Places 약관상 반드시 화면에 적어야 한다.**
 *
 * **사진 위가 아니라 카드 본문 맨 아래 한 줄로 둔다.** 썸네일이 64dp라 그 위에 글자를
 * 얹으면 읽을 수 없다. 보조 글자색으로 작게 적어, 적어야 할 것은 적되 장소 이름이
 * 주인공인 것은 그대로 둔다.
 *
 * 사진이 없거나 출처가 비어 있으면 아무것도 그리지 않는다.
 */
@Composable
fun PhotoAttributionLine(photo: PlacePhoto?, modifier: Modifier = Modifier) {
    val text = photo?.attribution?.takeIf { it.isNotBlank() } ?: return

    Text(
        // 이름만 덜렁 두면 주소의 일부처럼 읽힌다. 실제로 "일정 중심에서 0.2km" 아래
        // "東京ドームホテル"가 붙어 무엇인지 알 수 없었다.
        text = stringResource(R.string.photo_attribution, text),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
