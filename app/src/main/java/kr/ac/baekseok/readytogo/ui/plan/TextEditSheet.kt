package kr.ac.baekseok.readytogo.ui.plan

import kr.ac.baekseok.readytogo.ui.theme.LocalReduceMotion
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import kr.ac.baekseok.readytogo.ui.theme.pressClickable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 결과 화면 오른쪽 아래의 "말로 고치기" 원형 버튼(⑫).
 *
 * 예전에는 일정표 맨 위에 입력창이 늘 떠 있어 일정 첫 줄을 한 칸 밀어냈다. 버튼으로 접어 두고
 * 필요할 때 시트로 연다. 응답을 기다리는 동안에는 아이콘 대신 진행 표시가 돈다.
 */
@Composable
fun AiEditButton(busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    // 응답을 기다리는 동안 테두리가 숨 쉬듯 밝아졌다 흐려진다(⑫). 진행 표시만 돌면 화면 구석의
    // 작은 원이라 놓치기 쉽다. 애니메이션 제거가 켜져 있으면 테두리 없이 진행 표시만.
    val pulsing = busy && !LocalReduceMotion.current
    val ringAlpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "aiPulse")
        val alpha by transition.animateFloat(
            initialValue = PULSE_MIN_ALPHA,
            targetValue = PULSE_MAX_ALPHA,
            animationSpec = infiniteRepeatable(tween(Tokens.Motion.pulseMs), RepeatMode.Reverse),
            label = "aiPulseAlpha",
        )
        alpha
    } else {
        0f
    }
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = palette.Point,
        contentColor = palette.Surface,
        // 그림자 없이(⑫). 바탕과 색 차이로 뜬다.
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = Tokens.Elevation.none,
            pressedElevation = Tokens.Elevation.none,
            focusedElevation = Tokens.Elevation.none,
            hoveredElevation = Tokens.Elevation.none,
        ),
        modifier = modifier
            .size(Tokens.Size.aiButton)
            .border(Tokens.Size.pulseRing, palette.PointDark.copy(alpha = ringAlpha), CircleShape),
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = palette.Surface,
                strokeWidth = Tokens.Size.loadingStroke,
                modifier = Modifier.size(Tokens.Size.icon),
            )
        } else {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = stringResource(R.string.plan_text_edit_title))
        }
    }
}

/**
 * 말로 고치기 시트(design/mockup/edit-sheet). 둥근 닫기, 큰 제목, "N일차 · 한 문장으로" 안내,
 * 흰 카드 안 입력("요청"), 저장 안 됨·되돌리기 안내, 남색 pill "적용하기".
 *
 * 시안의 예시 칩과 "미리보기 → 적용" 두 단계는 새 기능이라 넣지 않았다 — 보내면 바로 적용되고
 * 결과는 결과 화면의 스낵바(되돌리기 포함)로 알린다. 그래서 보내는 즉시 시트를 닫는다.
 *
 * 규칙(로그인 필요, 200자 제한, 날짜를 바꾸면 비움)은 예전 일정표 위 입력창과 같다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditSheet(
    dayNumber: Int,
    controls: TextEditControls,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable(dayNumber) { mutableStateOf("") }
    val canSend = controls.enabled && text.isNotBlank() && !controls.busy

    fun submit() {
        if (!canSend) return
        controls.onSubmit(dayNumber, text)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Tokens.Radius.sheet,
        containerColor = palette.Background,
        // 손잡이 대신 둥근 닫기 버튼(시안). 손잡이는 끌어 내리는 법을 알아야 쓴다.
        dragHandle = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Tokens.Space.screenHorizontal, vertical = Tokens.Space.xl),
        ) {
            RoundIconButton(Icons.Outlined.ExpandMore, stringResource(R.string.common_close), onDismiss)

            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
                Text(stringResource(R.string.plan_text_edit_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(
                        R.string.plan_text_edit_subtitle,
                        stringResource(R.string.plan_day_tab, dayNumber),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.TextSecondary,
                )
            }

            if (!controls.enabled) {
                // 둘러보기: 입력 대신 로그인 안내. 다 쓴 뒤에야 안 된다는 것을 알게 하지 않는다.
                Text(
                    text = stringResource(R.string.plan_text_edit_sign_in_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.PointDark,
                    modifier = Modifier.clickable(onClick = controls.onSignIn),
                )
                return@Column
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Tokens.Radius.card)
                    .background(palette.Surface)
                    .padding(Tokens.Space.lg),
            ) {
                Text(
                    text = stringResource(R.string.plan_text_edit_request),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.TextSecondary,
                )
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_LENGTH) text = it },
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = palette.TextPrimary),
                    cursorBrush = SolidColor(palette.Point),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { field ->
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.plan_text_edit_hint),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.TextSecondary,
                            )
                        }
                        field()
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = palette.TextSecondary,
                    modifier = Modifier.size(Tokens.Size.inlineIcon),
                )
                Text(
                    text = stringResource(R.string.plan_text_edit_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.TextSecondary,
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Tokens.Size.button)
                    .clip(Tokens.Radius.button)
                    .background(if (canSend) palette.TextPrimary else palette.Divider)
                    .pressClickable(enabled = canSend, onClick = ::submit),
            ) {
                Text(
                    text = stringResource(R.string.plan_text_edit_apply),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.Surface,
                )
            }
        }
    }
}

// AI 버튼 대기 펄스의 테두리 진하기 범위.
private const val PULSE_MIN_ALPHA = 0.15f
private const val PULSE_MAX_ALPHA = 0.9f
