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
) : Serializable
