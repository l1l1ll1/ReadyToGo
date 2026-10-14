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

    /**
     * [oldId] 문서를 지우고 [trip]을 쓴다. **한 번에** 일어난다.
     *
     * 기간 변경에 쓴다. id가 날짜를 품고 있어 날짜가 바뀌면 문서 id도 바뀐다.
     * 쓰기와 지우기를 따로 부르면 중간에 실패했을 때 같은 여행이 두 장 남거나 하나도 안 남는다.
     */
    suspend fun replace(oldId: String, trip: SavedTrip): Result<Unit>

    /** 이름만 바꾼다. 일정 전체를 다시 저장하지 않아도 되게 따로 둔다. */
    suspend fun rename(id: String, title: String): Result<Unit>

    /**
     * 이 계정의 일정을 전부 지운다.
     *
     * 탈퇴에만 쓴다. 계정을 먼저 지우면 저장 위치에 접근할 권한이 사라져 데이터가 남아버린다.
     */
    suspend fun deleteAll(): Result<Unit>
}
