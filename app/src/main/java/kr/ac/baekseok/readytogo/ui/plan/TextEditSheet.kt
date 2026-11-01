package kr.ac.baekseok.readytogo.ui.plan

import kr.ac.baekseok.readytogo.ui.component.RoundIconButton
import kr.ac.baekseok.readytogo.ui.component.FitText
import kr.ac.baekseok.readytogo.ui.component.DashedDivider
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
 * 결과 화면 오른쪽 아래의 "AI 도우미" 원형 버튼(⑫).
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
 * AI 도우미 시트(design/mockup/edit-sheet와 같게). 둥근 닫기 → 제목·안내 → 예시 칩 → 흰 카드
 * (요청 입력 + 미리보기) → 저장 안 됨 안내 → 남색 pill "적용하기" → "직접 편집할게요".
 *
 * **두 단계다.** 문장을 쓰고 미리보기를 받으면(함수 호출 1회) 카드 안에 "전 → 후"가 줄마다 보인다.
 * "적용하기"는 그 결과를 쓴다 — 다시 부르지 않는다. 문장을 고치면 낡은 미리보기는 버린다.
 * 적용하면 시트를 닫고 결과 화면 스낵바(되돌리기 포함)로 알린다.
 *
 * 규칙(로그인 필요, 200자 제한, 날짜를 바꾸면 비움)은 예전 일정표 위 입력창과 같다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextEditSheet(
    dayNumber: Int,
    controls: TextEditControls,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable(dayNumber) { mutableStateOf("") }
    // 지금 문장에 대한 미리보기만 쓴다. 다른 날·다른 문장의 것은 버린 것으로 본다.
    val preview = controls.preview?.takeIf { it.dayNumber == dayNumber && it.sentence == text.trim() }
    val canPreview = controls.enabled && text.isNotBlank() && !controls.busy && preview == null
    val canApply = preview != null && preview.lines.isNotEmpty() && !controls.busy

    fun edit(value: String) {
        if (value.length > MAX_LENGTH) return
        text = value
        if (controls.preview != null) controls.onClearPreview()
    }

    fun requestPreview() {
        if (canPreview) controls.onPreview(dayNumber, text)
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
                    modifier = Modifier.pressClickable(onClick = controls.onSignIn),
                )
                return@Column
            }

            // 예시 칩 — 누르면 입력창에 채워진다(바로 보내지는 않는다. 고쳐 쓸 수 있게).
            // 도착일은 관광지가 한 곳뿐이고 점심 뒤에 오지 않아 "오후 하나 빼줘"가 늘 "이해하지 못했어요"로 끝난다.
            val chips = listOfNotNull(
                stringResource(R.string.plan_text_edit_chip_1).takeIf { dayNumber != ARRIVAL_DAY },
                stringResource(R.string.plan_text_edit_chip_2),
                stringResource(R.string.plan_text_edit_chip_3),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            ) {
                chips.forEach { chip ->
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(Tokens.Radius.chip)
                            .background(palette.Surface)
                            .pressClickable(role = Role.Button) { edit(chip) }
                            .padding(horizontal = Tokens.Space.lg, vertical = Tokens.Space.md),
                    )
                }
            }

            RequestCard(
                text = text,
                onTextChange = ::edit,
                canPreview = canPreview,
                busy = controls.busy,
                preview = preview,
                onRequestPreview = ::requestPreview,
            )

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
                    .background(if (canApply) palette.TextPrimary else palette.Divider)
                    .pressClickable(enabled = canApply) {
                        controls.onApply()
                        onDismiss()
                    },
            ) {
                Text(
                    text = stringResource(R.string.plan_text_edit_apply),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.Surface,
                )
            }

            Text(
                text = stringResource(R.string.plan_text_edit_manual),
                style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .pressClickable(role = Role.Button, onClick = onDismiss)
                    .padding(Tokens.Space.sm),
            )
        }
    }
}

/**
 * 흰 카드 — "요청" 입력과, 받았으면 아래 점선 너머 "미리보기"(전 → 후 한 줄씩).
 * 입력 오른쪽 화살표나 키보드 보내기로 미리보기를 받는다.
 */
@Composable
private fun RequestCard(
    text: String,
    onTextChange: (String) -> Unit,
    canPreview: Boolean,
    busy: Boolean,
    preview: TextEditPreview?,
    onRequestPreview: () -> Unit,
) {
    val palette = LocalPalette.current
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = palette.TextPrimary),
                cursorBrush = SolidColor(palette.Point),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onRequestPreview() }),
                modifier = Modifier.weight(1f),
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
            when {
                busy -> CircularProgressIndicator(
                    strokeWidth = Tokens.Size.loadingStroke,
                    modifier = Modifier.size(Tokens.Size.icon),
                )
                canPreview -> RoundIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    description = stringResource(R.string.plan_text_edit_preview_send),
                    onClick = onRequestPreview,
                )
            }
        }

        if (busy || preview != null) {
            DashedDivider(modifier = Modifier.padding(vertical = Tokens.Space.sm))
            Text(
                text = stringResource(R.string.plan_text_edit_preview),
                style = MaterialTheme.typography.labelMedium,
                color = palette.TextSecondary,
            )
            when {
                busy -> Text(
                    text = stringResource(R.string.plan_text_edit_preview_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.TextSecondary,
                )
                preview != null -> {
                    preview.lines.forEach { PreviewRow(it) }
                    preview.notice?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = palette.Warning)
                    }
                }
            }
        }
    }
}

/** 미리보기 한 줄 — 민트 원 안 바꿈 아이콘 + "전 → 후". 말줄임 없이 한 줄(FitText). */
@Composable
private fun PreviewRow(line: PreviewLine) {
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Tokens.Size.reviewIcon)
                .clip(CircleShape)
                .background(palette.PointLight),
        ) {
            Icon(line.kind.icon(), contentDescription = null, tint = palette.PointDark)
        }
        FitText(
            text = "${line.before} → ${line.after}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
    }
}


// AI 버튼 대기 펄스의 테두리 진하기 범위.
private const val PULSE_MIN_ALPHA = 0.15f
private const val PULSE_MAX_ALPHA = 0.9f

/** 첫날이 도착일이다(일정 생성이 항상 그렇게 만든다). */
private const val ARRIVAL_DAY = 1

/** 빼기는 빼는 표시, 바꾸기는 좌우 교환, 순서는 위아래 교환 — 일정표가 위아래로 흐르기 때문이다. */
private fun PreviewKind.icon(): ImageVector = when (this) {
    PreviewKind.REMOVED -> Icons.Outlined.RemoveCircleOutline
    PreviewKind.SWAPPED -> Icons.Outlined.SwapHoriz
    PreviewKind.MOVED -> Icons.Outlined.SwapVert
}
