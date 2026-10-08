package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.TripCondition

/**
 * 일정 생성. 후보 관광지를 "며칠째에 어떤 순서로 돌지"로 배치한다.
 *
 * 이것도 데이터 출처로 본다(CLAUDE.md). 지금은 Mock이 단순 규칙으로 나누지만,
 * 나중에 Python/FastAPI 서버나 AI가 이 자리를 대신할 수 있다.
 */
interface ItineraryRepository {
    /**
     * @return 하루치씩 담긴 리스트. 첫 번째 원소가 1일차이고, 각 원소 안의 순서가 방문 순서다.
     *         일정이 빈 날(도착 당일 등)은 빈 리스트로 채워 날짜 수와 길이를 맞춘다.
     */
    suspend fun planDailyAttractions(
        condition: TripCondition,
        candidates: List<Attraction>,
    ): Result<List<List<Attraction>>>
}