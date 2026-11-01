package kr.ac.baekseok.readytogo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 닉네임 입력.
 *
 * **첫 로그인과 프로필의 "닉네임 변경"이 같은 화면을 쓴다.** 입력 칸 하나짜리 화면을
 * 두 벌 만들면 글자 수 제한 같은 규칙이 한쪽에만 들어간다. 문구만 [firstTime]으로 가른다.
 *
 * 건너뛰기를 두지 않는 이유: Google 계정 이름이 미리 채워져 있어 한 번 누르면 넘어간다.
 * 건너뛸 수 있게 하면 "닉네임 없음" 상태를 화면마다 따로 처리해야 한다.
 *
 * @param firstTime 첫 로그인 직후인지. 환영 문구와 버튼 글자가 달라진다.
 * @param onDone 저장이 끝났을 때. 호출부가 다음 화면으로 보낸다.
 */
@Composable
fun NicknameScreen(
    firstTime: Boolean,
    onDone: () -> Unit,
    viewModel: NicknameViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // 저장이 끝나면 넘어간다. 화면을 그리는 도중에 이동시키면 안 되므로 여기서 받는다.
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Surface(color = LocalPalette.current.Background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Tokens.Space.screenHorizontal),
        ) {
            Spacer(modifier = Modifier.height(Tokens.Space.xxl))

            Text(
                text = stringResource(
                    if (firstTime) R.string.nickname_welcome else R.string.nickname_change,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(Tokens.Space.sm))
            Text(
                text = stringResource(R.string.nickname_body),
                style = MaterialTheme.typography.bodyLarge,
                color = LocalPalette.current.TextSecondary,
            )

            Spacer(modifier = Modifier.height(Tokens.Space.xl))

            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChange,
                label = { Text(stringResource(R.string.nickname_label)) },
                singleLine = true,
                isError = state.tooLong,
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.nickname_counter,
                            state.input.trim().length,
                            NicknameViewModel.MAX_LENGTH,
                        ),
                        color = if (state.tooLong) {
                            LocalPalette.current.Warning
                        } else {
                            LocalPalette.current.TextSecondary
                        },
                    )
                },
                // 키보드의 완료 키로도 저장되게 한다. 버튼까지 손을 옮기지 않아도 된다.
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { viewModel.save() },
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Spacer(modifier = Modifier.height(Tokens.Space.sm))
                Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalPalette.current.Warning,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(
                    if (firstTime) R.string.nickname_start else R.string.nickname_save,
                ),
                onClick = viewModel::save,
                enabled = state.canSave,
                loading = state.saving,
            )

            Spacer(modifier = Modifier.height(Tokens.Space.xxl))
        }
    }
}

/** 두 화면이 같은 Composable을 쓰므로, 호출부가 읽기 쉽게 이름만 나눠 둔다. */
@Composable
fun FirstNicknameScreen(onDone: () -> Unit) = NicknameScreen(firstTime = true, onDone = onDone)

@Composable
fun EditNicknameScreen(onDone: () -> Unit) = NicknameScreen(firstTime = false, onDone = onDone)
