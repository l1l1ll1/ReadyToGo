package kr.ac.baekseok.readytogo.ui.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.format.tripRangeText
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 여행 기간을 한 번에 고르는 바닥 시트.
 *
 * 출발일·귀국일을 각각 다른 달력으로 받던 것을 하나로 합쳤다. 두 번 열면 "출발일이 귀국일보다
 * 뒤"인 중간 상태가 생기고, 사용자는 며칠짜리 여행인지 세어 봐야 했다.
 *
 * @param onConfirm 두 날짜가 다 골라졌을 때만 불린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSheet(
    initialDeparture: LocalDate?,
    initialReturn: LocalDate?,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = LocalDate.now()

    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialDeparture?.toUtcMillis(),
        initialSelectedEndDateMillis = initialReturn?.toUtcMillis(),
        selectableDates = object : SelectableDates {
            // 지난 날짜로는 항공권을 검색할 수 없다. 고를 수 있게 두면 검색이 빈 결과로 돌아온다.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !utcTimeMillis.toLocalDate().isBefore(today)
        },
    )

    val start = pickerState.selectedStartDateMillis?.toLocalDate()
    val end = pickerState.selectedEndDateMillis?.toLocalDate()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalPalette.current.Background,
        shape = Tokens.Radius.sheet,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Tokens.Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.date_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }

            // 고른 기간을 시트 안에서도 보여준다. 달력만 보면 며칠짜리인지 세어 봐야 한다.
            Text(
                text = if (start != null && end != null) {
                    tripRangeText(start, end)
                } else {
                    stringResource(R.string.date_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (start != null && end != null) {
                    LocalPalette.current.Point
                } else {
                    LocalPalette.current.TextSecondary
                },
            )

            DateRangePicker(
                state = pickerState,
                // 기본 제목·헤드라인을 끈다. 위에 우리 문구가 이미 있어 같은 말이 두 번 나온다.
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = LocalPalette.current.Background),
                modifier = Modifier.weight(1f, fill = false),
            )

            PrimaryButton(
                text = stringResource(R.string.common_confirm),
                enabled = start != null && end != null,
                onClick = {
                    // enabled 조건과 같지만, 눌리는 순간 값이 null이 아님을 컴파일러에 알려야 한다.
                    val from = start ?: return@PrimaryButton
                    val to = end ?: return@PrimaryButton
                    onConfirm(from, to)
                },
                modifier = Modifier.padding(bottom = Tokens.Space.xl),
            )
        }
    }
}

/**
 * 날짜를 UTC 밀리초로. Material의 달력이 UTC 기준 밀리초만 받는다.
 *
 * 시간대를 섞으면 하루가 밀린다 — 한국(UTC+9)에서 자정에 고른 날짜를 현지 시간대로 변환하면
 * 전날이 되어버린다. 그래서 양쪽 변환을 모두 UTC로 고정한다.
 */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
