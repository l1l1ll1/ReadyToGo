package kr.ac.baekseok.readytogo.data.repository.mock

import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.LodgingRepository
import kotlinx.coroutines.delay

/**
 * 숙소 Mock.
 *
 * 정렬도 추천도 하지 않고 후보 전체를 그대로 준다. 고르는 일은 domain/recommend가 한다.
 */
class MockLodgingRepository : LodgingRepository {

    override suspend fun search(condition: TripCondition): Result<List<Lodging>> {
        delay(NETWORK_DELAY_MS)
        return Result.success(MockData.lodgings)
    }

    private companion object {
        const val NETWORK_DELAY_MS = 500L
    }
}