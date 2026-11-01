package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/**
 * 한 줄에 반드시 들어가는 글줄(⑫). **말줄임(…)도 두 줄 넘김도 없다.**
 *
 * [variants]를 긴 것부터 넣으면 폭에 들어가는 첫 번째를 그린다 — "리뷰 수 괄호를 빼고",
 * "엔화를 빼고"처럼 덜 중요한 것부터 떨어뜨리는 순서를 호출부가 정한다. 가장 짧은 것도 안 들어가면
 * 글자를 [MIN_SCALE]까지 조금씩 줄인다. 말줄임으로 끝을 자르면 가격·평점처럼 끝이 중요한 줄에서
 * 정작 숫자가 사라진다.
 */
@Composable
fun FitText(
    variants: List<AnnotatedString>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth
        val (text, fitted) = remember(variants, style, width) {
            fun fits(s: AnnotatedString, st: TextStyle) =
                measurer.measure(s, st, maxLines = 1, softWrap = false, constraints = Constraints())
                    .size.width <= width

            variants.firstOrNull { fits(it, style) }?.let { return@remember it to style }
            val last = variants.last()
            var scale = 1f
            while (scale > MIN_SCALE) {
                scale -= SCALE_STEP
                val smaller = style.copy(fontSize = style.fontSize * scale)
                if (fits(last, smaller)) return@remember last to smaller
            }
            last to style.copy(fontSize = style.fontSize * MIN_SCALE)
        }
        Text(text = text, style = fitted, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

/** 글자 하나뿐일 때. */
@Composable
fun FitText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified) =
    FitText(listOf(AnnotatedString(text)), modifier, style, color)

/** 줄여도 읽히는 하한. 이보다 작으면 같은 줄의 다른 글자와 위계가 무너진다. */
private const val MIN_SCALE = 0.8f
private const val SCALE_STEP = 0.05f
