package kr.ac.baekseok.readytogo.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.notification.canPostNotifications
import kr.ac.baekseok.readytogo.data.model.AppLanguage
import kr.ac.baekseok.readytogo.data.model.TextScale
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 설정 화면.
 *
 * 언어·알림·접근성을 하위 화면으로 빼지 않고 여기서 토글·다이얼로그로 끝낸다.
 * 항목이 두세 개씩이라 화면을 만들면 탭 한 번이 더 늘어날 뿐이다.
 *
 * 언어를 바꾸면 시스템이 화면을 다시 만들어 그 자리에서 글자가 바뀐다(AppLocale 참고).
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

    val context = LocalContext.current
    val permissionDenied = stringResource(R.string.settings_notify_permission_denied)

    // 권한 요청은 비동기라 "어느 토글을 켜려던 것인지"를 들고 있어야 한다.
    var pendingToggle by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val askNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // 거부하면 토글을 켜지 않는다. 켜진 채로 두면 오지 않는 알림을 기다리게 된다.
        if (granted) {
            pendingToggle?.invoke(true)
        } else {
            Toast.makeText(context, permissionDenied, Toast.LENGTH_LONG).show()
        }
        pendingToggle = null
    }

    /** 알림 토글. 켤 때만 권한을 묻는다 — 끄는 데는 권한이 필요 없다. */
    fun toggleNotification(enabled: Boolean, apply: (Boolean) -> Unit) {
        if (!enabled || context.canPostNotifications()) {
            apply(enabled)
            return
        }
        pendingToggle = apply
        askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = Tokens.Space.screenHorizontal,
            end = Tokens.Space.screenHorizontal,
            top = Tokens.Space.lg,
            bottom = Tokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
    ) {
        item { SectionHeader(title = stringResource(R.string.settings_section_account)) }
        item {
            NavigationRow(
                label = stringResource(R.string.settings_account),
                onClick = onOpenAccount,
            )
        }

        item { SettingsSectionGap(title = stringResource(R.string.settings_section_language)) }
        item {
            NavigationRow(
                label = stringResource(R.string.settings_language),
                value = stringResource(settings.language.labelRes),
                onClick = { showLanguagePicker = true },
            )
        }

        item { SettingsSectionGap(title = stringResource(R.string.settings_section_notification)) }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_notify_week),
                checked = settings.notifyBeforeWeek,
                onCheckedChange = { toggleNotification(it, viewModel::setNotifyBeforeWeek) },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_notify_day),
                checked = settings.notifyBeforeDay,
                onCheckedChange = { toggleNotification(it, viewModel::setNotifyBeforeDay) },
            )
        }

        item { SettingsSectionGap(title = stringResource(R.string.settings_section_accessibility)) }
        item {
            NavigationRow(
                label = stringResource(R.string.settings_text_scale),
                value = stringResource(settings.textScale.labelRes),
                onClick = { showTextScalePicker = true },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_high_contrast),
                description = stringResource(R.string.settings_high_contrast_body),
                checked = settings.highContrast,
                onCheckedChange = viewModel::setHighContrast,
            )
        }

        item { SettingsSectionGap(title = stringResource(R.string.settings_section_help)) }
        item {
            NavigationRow(
                label = stringResource(R.string.profile_support),
                onClick = onOpenSupport,
            )
        }
        item {
            NavigationRow(
                label = stringResource(R.string.profile_about),
                onClick = onOpenAbout,
            )
        }
    }

    if (showLanguagePicker) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries,
            selected = settings.language,
            labelOf = { stringResource(it.labelRes) },
            onSelect = {
                viewModel.setLanguage(it)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false },
        )
    }

    if (showTextScalePicker) {
        ChoiceDialog(
            title = stringResource(R.string.settings_text_scale),
            options = TextScale.entries,
            selected = settings.textScale,
            labelOf = { stringResource(it.labelRes) },
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
            color = LocalPalette.current.Divider,
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
                color = LocalPalette.current.TextSecondary,
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
                    color = LocalPalette.current.TextSecondary,
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
    // stringResource를 쓰려면 @Composable이어야 한다 — 이름표가 리소스에서 온다.
    labelOf: @Composable (T) -> String,
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
    )
}