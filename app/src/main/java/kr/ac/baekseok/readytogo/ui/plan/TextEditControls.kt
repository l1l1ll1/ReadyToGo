package kr.ac.baekseok.readytogo.ui.plan

import kr.ac.baekseok.readytogo.domain.edit.EditCommand

/**
 * 말로 고치기 시트에 필요한 것. 화면에서 시트까지 내려가는 값을 하나로 묶었다.
 *
 * @param enabled 로그인했는지. 함수가 로그인 토큰을 요구한다.
 * @param busy 함수 응답(미리보기)을 기다리는 중.
 * @param preview 받아 둔 미리보기. 없으면 "적용하기"가 비활성.
 * @param onPreview 일차 번호와 문장으로 미리보기를 받는다(함수 호출 1회).
 * @param onApply 받아 둔 미리보기를 적용한다(호출 없음).
 * @param onClearPreview 문장을 고치거나 시트를 닫을 때 낡은 미리보기를 버린다.
 */
data class TextEditControls(
    val enabled: Boolean = false,
    val busy: Boolean = false,
    val preview: TextEditPreview? = null,
    val onPreview: (Int, String) -> Unit = { _, _ -> },
    val onApply: () -> Unit = {},
    val onClearPreview: () -> Unit = {},
    val onSignIn: () -> Unit = {},
)

/** 함수가 200자를 넘으면 거절한다(functions/src/editDay.ts). 입력 단계에서 막아 헛호출을 없앤다. */
internal const val MAX_LENGTH = 200

/**
 * AI 도우미 미리보기 — 적용하면 무엇이 바뀌는지(⑫, design/mockup/edit-sheet).
 *
 * @param commands·keyByAlias 적용할 때 그대로 쓴다(함수를 다시 부르지 않는다).
 * @param lines 바뀌는 항목마다 "전 → 후". 비어 있으면 적용할 것이 없다.
 * @param notice 이해하지 못함·일부만 가능 같은 안내. 없으면 null.
 */
data class TextEditPreview(
    val dayNumber: Int,
    val sentence: String,
    val commands: List<EditCommand>,
    val keyByAlias: Map<String, String>,
    val lines: List<PreviewLine>,
    val notice: String?,
)

/** 미리보기 한 줄. [after]는 바뀐 장소 이름, 또는 "빼기"·"순서 바꿈". */
data class PreviewLine(val before: String, val after: String)
