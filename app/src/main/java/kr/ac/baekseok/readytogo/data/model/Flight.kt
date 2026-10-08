package kr.ac.baekseok.readytogo.data.model

import java.time.LocalDateTime

/**
 * 항공권 한 건.
 *
 * 추천 기준이 최저가 하나뿐이라(CLAUDE.md) 숙소·식당과 달리 점수 계산 대상이 아니다.
 * 그래서 ScoredCandidate를 구현하지 않는다.
 */
data class Flight(
    val id: String,
    val airline: String,
    val flightNumber: String,
    /** 공항 코드. 예: ICN */
    val departureAirport: String,
    val arrivalAirport: String,
    val departureAt: LocalDateTime,
    val arrivalAt: LocalDateTime,
    /** 경유 횟수. 0이면 직항. */
    val stops: Int,
    /** 왕복 1인 기준 가격(원). */
    val priceWon: Long,
    /** 결제 연동은 하지 않는다(CLAUDE.md 범위). 검색 결과 링크만 띄워준다. */
    val bookingUrl: String,
    /**
     * 실제 검색 결과가 아니라 예시 데이터면 true.
     *
     * 항공권 API는 무료 한도가 월 250회라 한도 초과나 네트워크 실패가 실제로 일어난다.
     * 그때 계획 전체를 실패시키는 대신 예시 데이터로 채우는데, 사용자가 이 가격을
     * 진짜로 오해하면 안 되므로 화면에 반드시 표시한다.
     */
    val isSample: Boolean = false,
)