package kr.ac.baekseok.readytogo.ui.plan

/**
 * 말로 고치기 시트에 필요한 것. 화면에서 시트까지 내려가는 값을 하나로 묶었다.
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

/** 함수가 200자를 넘으면 거절한다(functions/src/editDay.ts). 입력 단계에서 막아 헛호출을 없앤다. */
internal const val MAX_LENGTH = 200
