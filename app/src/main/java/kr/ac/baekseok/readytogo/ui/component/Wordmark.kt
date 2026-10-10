package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import kr.ac.baekseok.readytogo.R

/**
 * "ReadyToGo" 워드마크.
 *
 * **그리지 않고 그림 한 장을 쓴다.** 전에는 글꼴(Sora)을 넣고 Canvas로 글자와 궤적을
 * 직접 그렸는데, 디자인이 손글씨체로 바뀌면서 글꼴로는 낼 수 없는 모양이 됐다.
 * 그림 한 장이면 디자인을 고칠 때 파일만 바꾸면 되고, 글꼴 파일과 라이선스 표기도 빠진다.
 *
 * 배경이 투명해서 흰색·연노랑·터쿼이즈 어느 바탕에도 올릴 수 있다. 글자가 딥 네이비라
 * **어두운 바탕에는 쓰지 않는다.**
 *
 * @param width 그릴 가로 길이. 세로는 그림 비율대로 따라간다.
 */
@Composable
fun Wordmark(width: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.wordmark),
        contentDescription = null,
        // 세로를 지정하지 않고 비율에 맡긴다. 높이까지 정하면 디자인을 바꿀 때마다
        // 호출부 숫자를 같이 고쳐야 한다.
        contentScale = ContentScale.FillWidth,
        modifier = modifier.width(width),
    )
}
