package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 말로 고치기 입력창에 필요한 것. 화면 → 탭 내용 → 일정표 세 단계를 내려가서 하나로 묶었다.
 *
 * @param enabled 로그인했는지. 함수가 로그인 토큰을 요구한다.
 * @param busy 함수 응답을 기다리는 중.
 * @param onSubmit 일차 번호와 문장.
 */
data class TextEditControls(
    val enabled: Boolean = false,
    val busy: Boolean = false,
    val onSubmit: (Int, String) -> Unit = { _, _ -> },
    val onSignIn: () -> Unit = {},
)

/**
 * 일정표 위의 입력창. "오늘 피곤해, 오후 하나 빼고 저녁은 숙소 근처로" 같은 문장을 받는다.
 *
 * 둘러보기 중이면 입력창 대신 로그인 안내 한 줄을 둔다. 입력창을 그대로 두고 보낼 때 막으면
 * 문장을 다 쓴 뒤에야 안 된다는 것을 알게 된다.
 *
 * 보내면 칸을 비운다. 결과는 스낵바로 알리고, 잘못 바뀌었으면 되돌리기가 있다.
 */
@Composable
fun TextEditBar(
    dayNumber: Int,
    controls: TextEditControls,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val padded = modifier
        .fillMaxWidth()
        .padding(horizontal = Tokens.Space.screenHorizontal, vertical = Tokens.Space.sm)

    if (!controls.enabled) {
        Text(
            text = stringResource(R.string.plan_text_edit_sign_in_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.TextSecondary,
            modifier = padded.clickable(onClick = controls.onSignIn),
        )
        return
    }

    // 날짜를 바꾸면 쓰던 문장을 비운다. "오후 하나 빼줘"는 그날에 대한 말이다.
    var text by rememberSaveable(dayNumber) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (text.isBlank() || controls.busy) return
        controls.onSubmit(dayNumber, text)
        text = ""
        keyboard?.hide()
    }

    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= MAX_LENGTH) text = it },
        placeholder = { Text(stringResource(R.string.plan_text_edit_hint)) },
        singleLine = true,
        enabled = !controls.busy,
        shape = Tokens.Radius.button,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { submit() }),
        trailingIcon = {
            Box(contentAlignment = Alignment.Center) {
                if (controls.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(Tokens.Size.icon))
                } else {
                    IconButton(onClick = { submit() }, enabled = text.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(R.string.plan_text_edit_send),
                        )
                    }
                }
            }
        },
        modifier = padded,
    )
}

/** 함수가 200자를 넘으면 거절한다(functions/src/editDay.ts). 입력 단계에서 막아 헛호출을 없앤다. */
private const val MAX_LENGTH = 200
