package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kotlinx.coroutines.flow.Flow

/**
 * 즐겨찾기한 장소.
 *
 * [observeIds]를 따로 둔 이유: 결과 화면의 카드마다 ♥가 채워졌는지 알아야 하는데,
 * 카드 하나하나가 목록 전체를 받아 뒤지면 일정 열흘치에서 수백 번 훑게 된다.
 * id 집합만 받아 `in`으로 확인한다.
 */
interface FavoriteRepository {

    fun observeAll(): Flow<List<FavoritePlace>>

    fun observeIds(): Flow<Set<String>>

    suspend fun add(place: FavoritePlace): Result<Unit>

    suspend fun remove(placeId: String): Result<Unit>

    /** 이 계정의 즐겨찾기를 전부 지운다. 탈퇴에만 쓴다 — [SavedTripRepository.deleteAll] 주석 참고. */
    suspend fun deleteAll(): Result<Unit>
}
