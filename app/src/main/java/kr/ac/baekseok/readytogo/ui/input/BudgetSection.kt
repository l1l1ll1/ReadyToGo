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
import kr.ac.baekseok.readytogo.domain.budget.BudgetRange
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
 * @param adults 인원. 슬라이더 범위와 칩 금액이 여기에 비례한다([BudgetRange]).
 * @param budgetWon 항상 값이 있다. "미정" 상태를 두지 않는다 — 슬라이더 손잡이가 맨 왼쪽에
 *        붙으면 최솟값으로 정한 것처럼 보인다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSection(
    budgetWon: Long,
    adults: Int,
    onBudgetSelected: (Long) -> Unit,
    onBudgetTyped: (String) -> Unit,
) {
    var showInputDialog by remember { mutableStateOf(false) }

    val minWon = BudgetRange.minFor(adults)
    val maxWon = BudgetRange.maxFor(adults)

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
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clip(Tokens.Radius.chip)
                    .clickable { showInputDialog = true }
                    .padding(horizontal = Tokens.Space.sm, vertical = Tokens.Space.xs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
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
                // 여러 명이면 총액만 보고는 넉넉한지 알 수 없다. 1인당 몫을 같이 보여준다.
                if (adults > 1) {
                    Text(
                        text = "1인당 약 ${formatWonShort(perAdult(budgetWon, adults))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Tokens.Palette.TextSecondary,
                    )
                }
            }
        }

        Slider(
            value = budgetWon.toSliderPosition(minWon, maxWon),
            onValueChange = { position ->
                onBudgetSelected(position.toBudgetWon(minWon, maxWon))
            },
            // 0~1의 위치로 다룬다. 금액 변환은 로그 스케일이라 선형 범위를 쓸 수 없다.
            valueRange = 0f..1f,
            // steps를 주지 않는 이유: 10만원 단위면 눈금이 수십 개라 막대가 점선처럼 보인다.
            // 값을 끊는 건 toBudgetWon에서 하므로 눈금은 없어도 된다.
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatWonShort(minWon),
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Palette.TextSecondary,
            )
            Text(
                // 슬라이더 최댓값을 넘는 금액은 직접 입력으로만 넣을 수 있다. 그걸 알려준다.
                text = if (budgetWon > maxWon) {
                    "${formatWonShort(maxWon)} 초과"
                } else {
                    formatWonShort(maxWon)
                },
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Palette.TextSecondary,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        ) {
            BudgetRange.quickOptionsFor(adults).forEach { amount ->
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
            adults = adults,
            onConfirm = { typed ->
                onBudgetTyped(typed)
                showInputDialog = false
            },
            onDismiss = { showInputDialog = false },
        )
    }
}

/**
 * 예산을 숫자로 직접 넣는 다이얼로그.
 *
 * **상한이 없다.** 슬라이더는 조절할 수 있는 범위로 제한하지만, 그보다 큰 예산을 쓰는
 * 사람을 막을 이유는 없다.
 */
@Composable
private fun BudgetInputDialog(
    initialWon: Long,
    adults: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 다이얼로그 안에서만 쓰는 글자. 확인을 누를 때까지 밖의 값을 바꾸지 않는다 —
    // 지우는 동안 슬라이더가 최솟값으로 튀어다니면 어지럽다.
    var text by remember { mutableStateOf(initialWon.toString()) }
    val typed = text.toLongOrNull()

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
                    text = typed?.let { won ->
                        if (adults > 1) {
                            "${formatWon(won)} · 1인당 약 ${formatWonShort(perAdult(won, adults))}"
                        } else {
                            formatWon(won)
                        }
                    } ?: "숫자를 입력하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Tokens.Palette.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = (typed ?: 0L) > 0L,
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

/**
 * 1인당 금액. 만원 단위로 끊어 "1인당 약 83만원"처럼 보여준다.
 *
 * "약"을 붙이는 이유: 나누면 끝자리가 떨어지지 않는다. 1,000,000 ÷ 3 = 333,333원인데
 * 그대로 적으면 정확한 값으로 읽힌다.
 */
private fun perAdult(totalWon: Long, adults: Int): Long {
    val per = totalWon / adults
    return (per / 10_000) * 10_000
}

/**
 * 슬라이더 위치(0~1) ↔ 금액 변환. **로그 스케일이다.**
 *
 * 선형으로 두면 기본값 100만원이 왼쪽 15% 지점에 붙어 "최솟값에 가깝다"로 보이고,
 * 실제로 많이 쓰는 구간이 슬라이더의 3분의 1도 안 된다.
 *
 * 예산은 배수로 느끼는 값이다 — 100만원과 200만원의 차이가 400만원과 500만원의 차이보다 크다.
 * 로그 스케일이면 기본값이 가운데쯤에 오고, 낮은 구간을 세밀하게 조절할 수 있다.
 *
 * 범위를 인자로 받는 이유: 인원에 따라 범위가 달라진다([BudgetRange]).
 */
private fun Long.toSliderPosition(minWon: Long, maxWon: Long): Float {
    val clamped = coerceIn(minWon, maxWon).toDouble()
    val ratio = ln(clamped / minWon) / ln(maxWon.toDouble() / minWon)
    return ratio.toFloat().coerceIn(0f, 1f)
}

private fun Float.toBudgetWon(minWon: Long, maxWon: Long): Long {
    val raw = minWon * (maxWon.toDouble() / minWon).pow(toDouble())
    // 10만원 단위로 끊는다. 끊지 않으면 "1,237,419원" 같은 값이 나온다.
    val snapped = (raw / BudgetRange.STEP_WON).roundToLong() * BudgetRange.STEP_WON
    return snapped.coerceIn(minWon, maxWon)
}
