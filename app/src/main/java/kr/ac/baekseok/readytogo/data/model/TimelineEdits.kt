package kr.ac.baekseok.readytogo.data.model

/**
 * 사용자가 타임라인에서 손본 것. 전부 일차 번호로 묶는다.
 *
 * **계획 자체는 그대로 두고 이것만 바꿔서 화면을 다시 그린다.** 계획을 다시 만들면
 * Places·SerpApi 호출이 또 나간다.
 *
 * 일차 번호별로 들고 있는 이유: 같은 끼니 키("meal-LUNCH")가 모든 날에 있어서
 * 날짜를 구분하지 않으면 1일차 점심을 지울 때 전부 지워진다.
 *
 * **UI가 아니라 데이터 모델에 둔다.** 저장된 일정과 함께 Firestore에 올라가기 때문이다 —
 * 편집이 메모리에만 있으면 화면을 나갔다 오는 순간 원래대로 돌아간다.
 */
data class TimelineEdits(
    /** 뺀 항목의 키. */
    val deleted: Map<Int, Set<String>> = emptyMap(),
    /** 직접 고른 끼니. 일차 → (끼니 키 → 고른 장소 id). */
    val pickedMeals: Map<Int, Map<String, String>> = emptyMap(),
    /** 즐겨찾기로 바꿔 끼운 관광지. 일차 → (자리 키 → 고른 장소 id). */
    val pickedAttractions: Map<Int, Map<String, String>> = emptyMap(),
    /**
     * 끌어서 바꾼 순서. 일차 → 항목 키 순서.
     *
     * 끼니까지 포함한 전체 순서를 그대로 적어 둔다. 끼니는 끌 수 없지만, 관광지가
     * 끼니를 넘어가면 끼니의 자리도 따라 밀리므로 결과 순서를 통째로 두는 편이 단순하다.
     * 계획이 바뀌어 키가 맞지 않으면 기본 순서로 돌아간다.
     */
    val order: Map<Int, List<String>> = emptyMap(),
) {
    fun deletedIn(dayNumber: Int): Set<String> = deleted[dayNumber].orEmpty()
    fun pickedIn(dayNumber: Int): Map<String, String> = pickedMeals[dayNumber].orEmpty()
    fun pickedAttractionsIn(dayNumber: Int): Map<String, String> =
        pickedAttractions[dayNumber].orEmpty()
    fun orderIn(dayNumber: Int): List<String> = order[dayNumber].orEmpty()

    /** 손댄 것이 하나도 없는지. 저장할 때 빈 값을 올리지 않으려고 본다. */
    val isEmpty: Boolean
        get() = deleted.values.all { it.isEmpty() } &&
            pickedMeals.values.all { it.isEmpty() } &&
            pickedAttractions.values.all { it.isEmpty() } &&
            order.values.all { it.isEmpty() }
}
