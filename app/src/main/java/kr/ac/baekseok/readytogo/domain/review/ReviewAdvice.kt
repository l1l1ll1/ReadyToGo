package kr.ac.baekseok.readytogo.domain.review

import kr.ac.baekseok.readytogo.domain.edit.EditCommand
import kr.ac.baekseok.readytogo.domain.edit.parseCommands

/**
 * AI가 문제 하나에 낸 답 — 왜 불편한지 한 줄과 고칠 명령.
 *
 * @param keyByAlias 물어볼 때 쓴 별칭표. **고칠 때 그 시점의 표를 다시 만들면 안 된다.**
 *        그 사이 같은 날 다른 문제를 고쳐 관광지가 빠지면 별칭이 당겨져 "a2"가 다른 곳을 가리킨다.
 *        물어볼 때의 표로 실행하면, 이미 없어진 항목은 "없는 항목"으로 거부되고 기본 수정으로 넘어간다.
 */
data class ReviewAdvice(
    val why: String,
    val commands: List<EditCommand>,
    val keyByAlias: Map<String, String> = emptyMap(),
)

/**
 * 함수 응답의 `fixes`를 문제 id → 답으로 바꾼다. 명령 해석은 말로 고치기와 같은 [parseCommands]를 쓴다.
 * 모르는 문제 id는 버린다 — 서버가 enum으로 막지만 앱이 한 번 더 본다.
 */
fun parseReviewFixes(response: Any?, issueIds: Set<String>): Map<String, ReviewAdvice> {
    val fixes = (response as? Map<*, *>)?.get("fixes") as? List<*> ?: return emptyMap()
    return fixes.mapNotNull { raw ->
        val fix = raw as? Map<*, *> ?: return@mapNotNull null
        val id = fix["issue_id"] as? String ?: return@mapNotNull null
        if (id !in issueIds) return@mapNotNull null
        id to ReviewAdvice(
            why = (fix["why"] as? String).orEmpty().trim(),
            commands = parseCommands(mapOf("commands" to fix["commands"])),
        )
    }.toMap()
}
