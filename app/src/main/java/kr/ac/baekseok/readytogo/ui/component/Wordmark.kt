package kr.ac.baekseok.readytogo.ui.component

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.PathParser
import kr.ac.baekseok.readytogo.ui.theme.SoraFontFamily

/**
 * "ReadyToGo" 워드마크 (design/wordmark_readytogo.svg).
 *
 * **노을 배경 전용이다.** 글자와 궤적이 흰색이라 흰 배경에서는 보이지 않는다.
 * 흰 화면에는 쓰지 말 것.
 *
 * **SVG를 그대로 넣지 않고 Canvas로 그리는 이유**: 글자가 들어 있기 때문이다. SVG의 text를
 * 벡터로 변환해 두면 글자 모양이 기기 글꼴 설정과 무관하게 고정되는 대신, 글꼴을 바꾸거나
 * 글자를 고칠 때 다시 변환해야 한다. Compose가 Sora로 직접 그리면 그럴 일이 없다.
 *
 * 설계 좌표(300×80)로 적고 [width]에 맞춰 통째로 비례 확대한다. 그래서 크기를 바꿔도
 * 글자와 궤적의 관계가 틀어지지 않는다.
 *
 * @param width 그릴 가로 길이. 세로는 설계 비율(80/300)로 따라간다.
 */
@Composable
fun Wordmark(width: Dp, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()

    val label = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)) {
            append("Ready")
        }
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = CREAM)) {
            append("ToGo")
        }
    }

    Canvas(modifier = modifier.size(width, width * (DESIGN_HEIGHT / DESIGN_WIDTH))) {
        // 설계 좌표 1 단위가 화면에서 몇 픽셀인지.
        val unit = size.width / DESIGN_WIDTH

        val layout = measurer.measure(
            text = label,
            style = TextStyle(
                fontFamily = SoraFontFamily,
                fontSize = (38f * unit).toSp(),
                letterSpacing = (-0.8f * unit).toSp(),
                shadow = Shadow(
                    color = SHADOW,
                    offset = Offset(0f, 1.5f * unit),
                    blurRadius = 1.5f * unit,
                ),
            ),
        )

        // SVG는 글자를 baseline 기준으로 놓는다(y=56). Compose는 좌상단 기준이라 빼준다.
        drawText(layout, topLeft = Offset(2f * unit, 56f * unit - layout.firstBaseline))

        drawTrail(unit)
        drawPlane(unit)
        drawPin(unit)
    }
}

/** 점선 궤적. 비행기에서 핀까지 이어진다. */
private fun DrawScope.drawTrail(unit: Float) {
    val trail = Path().apply {
        moveTo(12f * unit, 70f * unit)
        cubicTo(
            60f * unit, 30f * unit,
            140f * unit, 20f * unit,
            262f * unit, 20f * unit,
        )
    }

    withShadow(unit) {
        drawPath(
            path = trail,
            color = Color.White,
            style = Stroke(
                width = 3f * unit,
                cap = StrokeCap.Round,
                // 점 하나(1)와 빈칸(7). 둥근 끝 때문에 점이 동그랗게 찍힌다.
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(1f * unit, 7f * unit),
                ),
            ),
        )
    }
}

private fun DrawScope.drawPlane(unit: Float) {
    val plane = svgPath(PLANE_PATH).transformed(unit) {
        postScale(0.75f, 0.75f)
        postRotate(-50f)
        postTranslate(10f * unit, 70f * unit)
    }
    withShadow(unit) { drawPath(plane, Color.White) }
}

private fun DrawScope.drawPin(unit: Float) {
    val pin = svgPath(PIN_PATH).transformed(unit) {
        postScale(0.875f, 0.875f)
        postTranslate(272f * unit, 2f * unit)
    }
    withShadow(unit) { drawPath(pin, CREAM) }

    // 핀 가운데 점. 그림자를 주지 않는다 — 핀 위에 얹히는 작은 점이라 번져 보이기만 한다.
    drawCircle(
        color = PIN_CENTER,
        radius = 2.4f * unit,
        center = Offset(272f * unit, 9f * unit),
    )
}

/**
 * 그리는 동안만 옅은 그림자를 건다.
 *
 * Compose의 DrawScope에는 블러가 없어 안드로이드 Paint의 shadowLayer를 빌려 쓴다.
 * 글자 쪽은 TextStyle의 shadow가 같은 일을 해주므로 여기 올 필요가 없다.
 */
private inline fun DrawScope.withShadow(unit: Float, block: DrawScope.() -> Unit) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        setShadowLayer(1.5f * unit, 0f, 1.5f * unit, SHADOW.toArgb())
    }
    // 레이어를 하나 띄워야 그 안에 그린 것에 그림자가 붙는다.
    val layer = canvas.saveLayer(null, paint)
    block()
    canvas.restoreToCount(layer)
}

/** `d` 속성 문자열을 Path로. 손으로 옮겨 적다 틀리는 것보다 파서에 맡기는 편이 안전하다. */
private fun svgPath(data: String): android.graphics.Path =
    PathParser.createPathFromPathData(data)

/**
 * 설계 좌표(단위 1)로 적힌 Path를 화면 크기에 맞춰 옮긴다.
 *
 * `post*`로 쌓으므로 **적은 순서대로** 적용된다 — 설계 단위를 픽셀로 키운 뒤
 * 호출부가 준 축소·회전·이동이 차례로 얹힌다.
 */
private fun android.graphics.Path.transformed(
    unit: Float,
    build: Matrix.() -> Unit,
): Path {
    val matrix = Matrix().apply {
        setScale(unit, unit)
        build()
    }
    val moved = android.graphics.Path()
    transform(matrix, moved)
    return moved.asComposePath()
}

/** 설계 원본 크기. 여기 숫자가 SVG와 같아야 비율이 맞는다. */
private const val DESIGN_WIDTH = 300f
private const val DESIGN_HEIGHT = 80f

private val CREAM = Color(0xFFFEF3C7)
private val PIN_CENTER = Color(0xFFF43F5E)
private val SHADOW = Color(0x407C2D12)

private const val PLANE_PATH =
    "M0 -10 l3 7 l9 6 l-1 3 l-8 -3 l-1 6 l3 2 l-1 2 l-4 -1 l-4 1 l-1 -2 l3 -2 l-1 -6 " +
        "l-8 3 l-1 -3 l9 -6 z"

private const val PIN_PATH =
    "M0 0 a7 7 0 0 1 7 7 c0 5 -7 13 -7 13 s-7 -8 -7 -13 a7 7 0 0 1 7 -7 z"
