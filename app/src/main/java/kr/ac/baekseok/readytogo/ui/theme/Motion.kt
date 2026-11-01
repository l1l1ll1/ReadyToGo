package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * 시스템 "애니메이션 제거"(개발자 옵션·접근성)가 켜져 있는지. 켜져 있으면 직접 만든 효과
 * (눌림 축소, 펄스, 하이라이트, 시트 뒤 축소)를 끈다 — 어지럼증이 있는 사용자를 위한 설정이다.
 *
 * Compose 기본 애니메이션도 이 배율을 따르지만, 반복 애니메이션(펄스)은 배율 0에서 멈춘 첫 프레임에
 * 갇혀 이상하게 보일 수 있어 아예 그리지 않는다.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** 지금 기기의 애니메이션 배율이 0인지 읽는다. 앱 테마 루트에서 한 번 읽어 [LocalReduceMotion]으로 내린다. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** 모션 길이를 지키는 tween. 애니메이션 제거가 켜져 있으면 바로 끝난다. */
@Composable
@ReadOnlyComposable
fun <T> motionTween(durationMs: Int): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else tween(durationMs)

/**
 * 누르는 동안 [Tokens.Motion.pressScale]로 살짝 줄인다(⑫). 손가락 아래에서 무언가 눌렸다는 걸
 * 색보다 먼저 알린다. clickable과 같은 [interactionSource]를 넘겨야 눌림을 안다.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !LocalReduceMotion.current) Tokens.Motion.pressScale else 1f,
        animationSpec = motionTween(Tokens.Motion.shortMs),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 누르면 살짝 줄어드는 clickable(⑫). 버튼·카드마다 interactionSource를 따로 만들지 않게 묶었다.
 * 물결(ripple) 효과는 그대로 둔다 — 줄어듦은 거기에 더해지는 손맛이다.
 */
@Composable
fun Modifier.pressClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return pressScale(interaction).clickable(
        interactionSource = interaction,
        indication = LocalIndication.current,
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}
