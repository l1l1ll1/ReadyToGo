package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kotlinx.coroutines.flow.Flow

/**
 * 저장된 일정.
 *
 * 목록을 [Flow]로 내보내는 이유: 결과 화면에서 저장을 누르면 "여행" 탭이 바로 바뀌어야 한다.
 * 탭을 다시 열 때마다 불러오는 구조면 저장했는데 목록에 없는 순간이 생긴다.
 */
interface SavedTripRepository {

    fun observeAll(): Flow<List<SavedTrip>>

    suspend fun save(trip: SavedTrip): Result<Unit>

    suspend fun delete(id: String): Result<Unit>

    /** 이름만 바꾼다. 일정 전체를 다시 저장하지 않아도 되게 따로 둔다. */
    suspend fun rename(id: String, title: String): Result<Unit>
}
