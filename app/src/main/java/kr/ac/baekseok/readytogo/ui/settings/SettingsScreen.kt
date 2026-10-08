package kr.ac.baekseok.readytogo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.data.model.AppLanguage
import kr.ac.baekseok.readytogo.data.model.TextScale
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 설정 화면.
 *
 * 언어·알림·접근성을 하위 화면으로 빼지 않고 여기서 토글·다이얼로그로 끝낸다.
 * 항목이 두세 개씩이라 화면을 만들면 탭 한 번이 더 늘어날 뿐이다.
 *
 * **1단계에서는 값만 바뀌고 실제로 동작하지 않는다.** 언어를 영어로 바꿔도 화면은 한국어다.
 * 실제 동작은 8단계 3번에서 연결한다.
 */
@Composable
fun SettingsScreen(
    onOpenAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showTextScalePicker by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = Tokens.Space.screenHorizontal,
            end = Tokens.Space.screenHorizontal,
            top = Tokens.Space.lg,
            bottom = Tokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
    ) {
        item { SectionHeader(title = "계정") }
        item { NavigationRow(label = "계정 관리", onClick = onOpenAccount) }

        item { SettingsSectionGap(title = "언어") }
        item {
            NavigationRow(
                label = "앱 언어",
                value = settings.language.label,
                onClick = { showLanguagePicker = true },
            )
        }

        item { SettingsSectionGap(title = "알림") }
        item {
            ToggleRow(
                label = "출발 7일 전 알림",
                checked = settings.notifyBeforeWeek,
                onCheckedChange = viewModel::setNotifyBeforeWeek,
            )
        }
        item {
            ToggleRow(
                label = "출발 1일 전 알림",
                checked = settings.notifyBeforeDay,
                onCheckedChange = viewModel::setNotifyBeforeDay,
            )
        }

        item { SettingsSectionGap(title = "접근성") }
        item {
            NavigationRow(
                label = "글자 크기",
                value = settings.textScale.label,
                onClick = { showTextScalePicker = true },
            )
        }
        item {
            ToggleRow(
                label = "고대비",
                description = "글자와 배경의 대비를 높입니다.",
                checked = settings.highContrast,
                onCheckedChange = viewModel::setHighContrast,
            )
        }

        item { SettingsSectionGap(title = "도움") }
        item { NavigationRow(label = "고객센터", onClick = onOpenSupport) }
        item { NavigationRow(label = "정보", onClick = onOpenAbout) }
    }

    if (showLanguagePicker) {
        ChoiceDialog(
            title = "앱 언어",
            options = AppLanguage.entries,
            selected = settings.language,
            labelOf = { it.label },
            onSelect = {
                viewModel.setLanguage(it)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false },
        )
    }

    if (showTextScalePicker) {
        ChoiceDialog(
            title = "글자 크기",
            options = TextScale.entries,
            selected = settings.textScale,
            labelOf = { it.label },
            onSelect = {
                viewModel.setTextScale(it)
                showTextScalePicker = false
            },
            onDismiss = { showTextScalePicker = false },
        )
    }
}

/** 섹션 사이 간격 + 제목. 첫 섹션만 간격이 없어야 해서 따로 쓴다. */
@Composable
private fun SettingsSectionGap(title: String) {
    Column {
        HorizontalDivider(
            color = Tokens.Palette.Divider,
            modifier = Modifier.padding(vertical = Tokens.Space.lg),
        )
        SectionHeader(title = title)
    }
}

/**
 * 눌러서 들어가거나 고르는 줄.
 * @param value 현재 고른 값. 오른쪽에 회색으로 보여줘서 들어가지 않고도 알 수 있게 한다.
 */
@Composable
private fun NavigationRow(label: String, value: String? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.Space.lg),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.Palette.TextSecondary,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // 스위치만 누를 수 있게 하면 손가락으로 맞추기 어렵다. 줄 전체를 누를 수 있게 한다.
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = Tokens.Space.md),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Tokens.Palette.TextSecondary,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 하나만 고르는 다이얼로그. 언어와 글자 크기가 같은 모양이라 공용으로 둔다. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = Tokens.Space.sm),
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                        )
                        Text(text = labelOf(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}