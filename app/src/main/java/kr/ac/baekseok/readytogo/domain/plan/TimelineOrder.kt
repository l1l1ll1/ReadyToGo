package kr.ac.baekseok.readytogo.domain.plan

/**
 * [key]를 [afterKey] 바로 뒤로 옮긴 순서. 옮길 수 없으면 null.
 *
 * 끌어서 옮기기(TimelineTab.moveVisit)는 "몇 번째 칸으로"를 받고, 말로 고치기는
 * "무엇 뒤로"를 받는다. 입력이 달라 함수를 나눴지만 결과는 같은 형식(보이는 항목의 키 순서)이라
 * 둘 다 [kr.ac.baekseok.readytogo.data.model.TimelineEdits.order]에 그대로 들어간다.
 */
fun moveAfter(keys: List<String>, key: String, afterKey: String): List<String>? {
    if (key == afterKey || key !in keys || afterKey !in keys) return null
    val rest = keys - key
    return rest.toMutableList().apply { add(rest.indexOf(afterKey) + 1, key) }
}
