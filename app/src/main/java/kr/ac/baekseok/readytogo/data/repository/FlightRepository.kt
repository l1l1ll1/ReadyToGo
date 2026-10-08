package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.Flight
import kr.ac.baekseok.readytogo.data.model.TripCondition

/**
 * 항공권 검색.
 *
 * 3단계에서 Mock 구현체를, 7단계에서 실제 API 구현체를 붙인다.
 * 화면은 이 인터페이스만 알면 되고, 어느 쪽이 붙어 있는지 알 필요가 없다(CLAUDE.md).
 */
interface FlightRepository {
    /**
     * 조건에 맞는 항공권을 찾는다. **가격 오름차순으로 정렬해서 반환한다**(CLAUDE.md 최저가 기준).
     * 정렬을 호출부가 아니라 구현체 책임으로 둔 이유는, 실제 API가 정렬된 결과를 주는 경우
     * 다시 정렬하지 않아도 되게 하기 위함이다.
     */
    suspend fun search(condition: TripCondition): Result<List<Flight>>
}