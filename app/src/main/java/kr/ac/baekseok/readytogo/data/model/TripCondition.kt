package kr.ac.baekseok.readytogo.data.model

import java.io.Serializable
import java.time.LocalDate

/**
 * 여행 목적. label은 화면에 그대로 노출되는 한국어 이름이다.
 * 나중에 Repository 질의 조건으로도 쓰기 때문에 UI 패키지가 아니라 데이터 모델에 둔다.
 */
enum class TravelPurpose(val label: String) {
    REST("휴양"),
    SIGHTSEEING("관광"),
    FOOD("맛집"),
    ACTIVITY("액티비티"),
}

/**
 * 사용자가 입력한 여행 조건 묶음.
 *
 * 출발지·목적지가 문자열이 아니라 Destination인 이유는 Destination 주석 참고 —
 * 좌표와 공항 코드를 같이 들고 있어야 검색을 할 수 있다.
 *
 * Serializable인 이유: 화면 이동과 회전 시 이 조건을 Bundle에 담아 보존해야 한다.
 */
data class TripCondition(
    val origin: Destination? = null,
    val destination: Destination? = null,
    val departureDate: LocalDate? = null,
    val returnDate: LocalDate? = null,
    /** 원 단위. 통화가 늘어날 일이 없어 Long 한 개로 둔다. */
    val budgetWon: Long? = null,
    val purposes: Set<TravelPurpose> = emptySet(),
    /**
     * 성인 인원. [MIN_ADULTS]~[MAX_ADULTS].
     *
     * 아동·유아를 나누지 않는 이유: 항공료 할인율과 숙소 정원 규칙이 항공사·숙소마다 달라
     * 정확히 계산할 수 없다. 틀린 금액을 보여주느니 성인 기준으로만 잡고, 아동이 있으면
     * 사용자가 실제 예약 단계에서 조정하는 쪽이 낫다.
     *
     * 6명을 상한으로 둔 이유: 그 이상은 단체 요금·다인실 규칙이 따로 적용돼
     * 1인 기준 계산이 의미를 잃는다.
     */
    val adults: Int = 1,
) : Serializable {

    /**
     * 필요한 객실 수. **2인 1실 기준**이다.
     *
     * 숙소 가격은 1박 1실 기준으로 들어오므로, 인원이 늘면 가격이 아니라 방 수가 늘어난다.
     * 홀수면 한 명이 남으므로 올림한다 — 3명이면 2실이다.
     */
    val roomCount: Int
        get() = (adults + ADULTS_PER_ROOM - 1) / ADULTS_PER_ROOM

    companion object {
        const val MIN_ADULTS = 1
        const val MAX_ADULTS = 6

        /** 2인 1실. 방 수 계산의 기준. */
        const val ADULTS_PER_ROOM = 2
    }
}
