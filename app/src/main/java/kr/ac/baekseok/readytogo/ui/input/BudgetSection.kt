package kr.ac.baekseok.readytogo.ui.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 예산 입력 — 슬라이더 · 빠른 선택 칩 · 금액 탭하면 직접 입력.
 *
 * **세 입력이 같은 값을 쓴다.** 대략 정하는 사람은 슬라이더나 칩이 빠르고, 이미 금액을
 * 정해 둔 사람은 숫자를 바로 넣는 게 빠르다. 셋이 따로 값을 들고 있으면 어느 게 진짜인지
 * 알 수 없으므로 무엇을 만져도 같은 값을 덮어쓴다.
 *
 * 직접 입력을 **항상 보이는 칸이 아니라 다이얼로그**로 둔 이유: 슬라이더·칩으로 정하는 사람이
 * 대부분인데, 빈 입력 칸이 아래에 있으면 거기도 채워야 하는 것처럼 보여 화면만 길어진다.
 *
 * @param budgetWon 항상 값이 있다. "미정" 상태를 두지 않는다 — 슬라이더 손잡이가 맨 왼쪽에
 *        붙으면 최솟값으로 정한 것처럼 보인다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSection(
    budgetWon: Long,
    onBudgetSelected: (Long) -> Unit,
    onBudgetTyped: (String) -> Unit,
) {
    var showInputDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "예산",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // 금액 자체가 직접 입력 버튼이다. 연필 아이콘으로 누를 수 있다는 걸 알린다.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
                modifier = Modifier
                    .clip(Tokens.Radius.chip)
                    .clickable { showInputDialog = true }
                    .padding(horizontal = Tokens.Space.sm, vertical = Tokens.Space.xs),
            ) {
                Text(
                    text = formatWon(budgetWon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Tokens.Palette.Point,
                )
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "예산 직접 입력",
                    tint = Tokens.Palette.Point,
                )
            }
        }

        Slider(
            value = budgetWon.toSliderPosition(),
            onValueChange = { position -> onBudgetSelected(position.toBudgetWon()) },
            // 0~1의 위치로 다룬다. 금액 변환은 로그 스케일이라 선형 범위를 쓸 수 없다.
            valueRange = 0f..1f,
            // steps를 주지 않는 이유: 10만원 단위면 눈금이 47개라 점이 빽빽해 막대가 점선처럼 보인다.
            // 값을 끊는 건 toBudgetWon에서 하므로 눈금은 없어도 된다.
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatWonShort(MIN_BUDGET_WON),
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Palette.TextSecondary,
            )
            Text(
                // 슬라이더 최댓값을 넘는 금액은 직접 입력으로만 넣을 수 있다. 그걸 알려준다.
                text = if (budgetWon > MAX_BUDGET_WON) {
                    "${formatWonShort(MAX_BUDGET_WON)} 초과"
                } else {
                    formatWonShort(MAX_BUDGET_WON)
                },
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Palette.TextSecondary,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        ) {
            QUICK_BUDGETS.forEach { amount ->
                FilterChip(
                    selected = budgetWon == amount,
                    onClick = { onBudgetSelected(amount) },
                    label = { Text(formatWonShort(amount)) },
                    shape = Tokens.Radius.chip,
                )
            }
        }
    }

    if (showInputDialog) {
        BudgetInputDialog(
            initialWon = budgetWon,
            onConfirm = { typed ->
                onBudgetTyped(typed)
                showInputDialog = false
            },
            onDismiss = { showInputDialog = false },
        )
    }
}

/** 예산을 숫자로 직접 넣는 다이얼로그. */
@Composable
private fun BudgetInputDialog(
    initialWon: Long,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 다이얼로그 안에서만 쓰는 글자. 확인을 누를 때까지 밖의 값을 바꾸지 않는다 —
    // 지우는 동안 슬라이더가 최솟값으로 튀어다니면 어지럽다.
    var text by remember { mutableStateOf(initialWon.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("예산 입력") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() } },
                    label = { Text("원") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = Tokens.Radius.button,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    // 숫자를 세지 않아도 되게 천 단위로 끊어 보여준다.
                    text = text.toLongOrNull()?.let { formatWon(it) } ?: "숫자를 입력하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Tokens.Palette.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = (text.toLongOrNull() ?: 0L) > 0L,
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

/**
 * 슬라이더 범위와 단위.
 *
 * 30만원 아래로는 해외여행 항공권조차 어려워 고를 이유가 없고,
 * 500만원을 넘기면 슬라이더 한 칸이 너무 커져 조절이 안 된다. 그보다 큰 금액은 직접 입력으로 받는다.
 */
private const val MIN_BUDGET_WON = 300_000L
private const val MAX_BUDGET_WON = 5_000_000L
private const val BUDGET_STEP_WON = 100_000L

/**
 * 슬라이더 위치(0~1) ↔ 금액 변환. **로그 스케일이다.**
 *
 * 선형으로 두면 기본값 100만원이 왼쪽 15% 지점에 붙어 "최솟값에 가깝다"로 보이고,
 * 실제로 많이 쓰는 50만~200만 구간이 슬라이더의 3분의 1도 안 된다.
 *
 * 예산은 배수로 느끼는 값이다 — 100만원과 200만원의 차이가 400만원과 500만원의 차이보다 크다.
 * 로그 스케일이면 100만원이 약 43% 지점(거의 가운데)에 오고, 낮은 구간을 세밀하게 조절할 수 있다.
 */
private fun Long.toSliderPosition(): Float {
    val clamped = coerceIn(MIN_BUDGET_WON, MAX_BUDGET_WON).toDouble()
    val ratio = ln(clamped / MIN_BUDGET_WON) / ln(MAX_BUDGET_WON.toDouble() / MIN_BUDGET_WON)
    return ratio.toFloat().coerceIn(0f, 1f)
}

private fun Float.toBudgetWon(): Long {
    val raw = MIN_BUDGET_WON * (MAX_BUDGET_WON.toDouble() / MIN_BUDGET_WON).pow(toDouble())
    // 10만원 단위로 끊는다. 끊지 않으면 "1,237,419원" 같은 값이 나온다.
    val snapped = (raw / BUDGET_STEP_WON).roundToLong() * BUDGET_STEP_WON
    return snapped.coerceIn(MIN_BUDGET_WON, MAX_BUDGET_WON)
}

/** 자주 쓰는 금액. 누르면 슬라이더도 같이 움직인다. */
private val QUICK_BUDGETS = listOf(500_000L, 1_000_000L, 1_500_000L, 2_000_000L)
