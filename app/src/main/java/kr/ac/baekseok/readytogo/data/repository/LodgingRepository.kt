package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.TripCondition

/**
 * 숙소 검색.
 *
 * 정렬이나 추천은 하지 않고 후보만 돌려준다.
 * 점수 계산과 두 선택지 고르기는 domain/recommend의 순수 함수가 맡는다
 * (데이터 출처가 바뀌어도 추천 규칙은 그대로 유지되어야 하므로 분리했다).
 */
interface LodgingRepository {
    suspend fun search(condition: TripCondition): Result<List<Lodging>>
}