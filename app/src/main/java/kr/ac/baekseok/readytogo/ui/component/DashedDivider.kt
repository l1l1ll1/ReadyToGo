package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 목록·카드 안의 구분선. ⑫부터 실선 대신 점선이다(design/mockup).
 *
 * 흰 카드 안에서 실선은 카드를 두 장으로 쪼개 보이게 한다. 점선은 "같은 카드 안의 다음 줄"로 읽힌다.
 * Material HorizontalDivider와 같은 자리에 그대로 바꿔 끼울 수 있게 인자 모양을 맞췄다.
 */
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.Divider,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(Tokens.Size.divider),
    ) {
        val dash = Tokens.Size.dividerDash.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
    }
}
