package kr.ac.baekseok.readytogo.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 저장된 여행 일정. "여행" 탭과 홈의 D-day 카드가 이걸 읽는다.
 *
 * **완성된 TripPlan을 통째로 들고 있지 않는 이유**: 목록 화면은 제목·날짜·도시만 필요한데
 * 계획 전체를 불러오면 일정 열 개를 보여주려고 항공·숙소·식당 수백 건을 읽어야 한다.
 * 상세로 들어갈 때 [condition]으로 다시 만든다.
 *
 * 2단계에서 Firestore 문서 하나가 이 모델 하나에 대응한다.
 *
 * @param condition 이 일정을 만들 때 쓴 조건. 상세 화면이 계획을 재생성하는 데 쓴다.
 */
data class SavedTrip(
    val id: String,
    /** 사용자가 바꿀 수 있는 이름. 기본값은 "도쿄 5일" 같은 자동 생성 문구다. */
    val title: String,
    val destinationName: String,
    val departureDate: LocalDate,
    val returnDate: LocalDate,
    val condition: TripCondition,
    val savedAt: LocalDate,
    /**
     * 사용자가 타임라인에서 손본 것.
     *
     * 계획 자체가 아니라 편집만 저장한다 — 계획은 조건으로 다시 만들 수 있고,
     * 장소 목록을 통째로 올리면 문서가 커지는 데다 가격·평점이 굳어 버린다.
     *
     * 기본값을 둔 이유: 이 필드가 생기기 전에 저장된 일정에는 없다.
     */
    val edits: TimelineEdits = TimelineEdits(),
) {
    /** 며칠짜리 여행인지. 출발일과 귀국일을 모두 포함해 센다. */
    val dayCount: Int
        get() = ChronoUnit.DAYS.between(departureDate, returnDate).toInt() + 1

    /**
     * 출발까지 남은 날.
     *
     * @param today 오늘. 인자로 받는 이유는 [LocalDate.now]를 안에서 부르면 테스트가
     *        실제 날짜에 따라 통과·실패를 왔다갔다 하기 때문이다.
     */
    fun daysUntil(today: LocalDate): Long = ChronoUnit.DAYS.between(today, departureDate)

    /** 아직 떠나지 않은 여행인지. 홈의 "다가오는 여행"이 이걸로 고른다. */
    fun isUpcoming(today: LocalDate): Boolean = !returnDate.isBefore(today)

    /** 지금 여행 중인지. 출발일과 귀국일 당일도 여행 중으로 본다. */
    fun isOngoing(today: LocalDate): Boolean =
        !today.isBefore(departureDate) && !today.isAfter(returnDate)

    /**
     * 오늘이 몇 일차인지. 여행 기간이 아니면 null.
     *
     * 1부터 센다 — 화면과 [TimelineEdits]가 쓰는
     * 일차 번호와 같은 값이어야 오늘 일정을 바로 집을 수 있다.
     */
    fun dayNumberOn(today: LocalDate): Int? {
        if (!isOngoing(today)) return null
        return ChronoUnit.DAYS.between(departureDate, today).toInt() + 1
    }

    companion object {
        /**
         * 조건에서 id를 만든다. 같은 조건이면 항상 같은 id가 나온다.
         *
         * **id를 여기 한 곳에서만 만드는 이유**: 저장을 두 번 눌러도 목록에 두 줄이 생기지 않아야 하고,
         * 저장한 일정을 다시 열었을 때 "이미 저장됨"으로 보여야 한다. 규칙이 두 곳에 있으면
         * 한쪽이 다른 id를 만들어 같은 여행이 두 장으로 쌓인다 — 실제로 한 번 그랬다.
         *
         * 조건이 덜 채워져 있으면 null이다. 그런 일정은 저장할 수도 없다.
         */
        fun idFor(condition: TripCondition): String? {
            val city = condition.destination?.city ?: return null
            val departure = condition.departureDate ?: return null
            val returnDate = condition.returnDate ?: return null
            val purposes = condition.purposes.map { it.name }.sorted()
            return "$city-$departure-$returnDate-${condition.budgetWon}-$purposes"
        }
    }
}
