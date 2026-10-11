package kr.ac.baekseok.readytogo.data.repository.fake

import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.Destination
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.canSave
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.SavedTripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * 저장된 일정 Mock — 메모리에만 둔다. 앱을 껐다 켜면 샘플 상태로 돌아간다.
 *
 * 샘플 3건을 미리 넣어두는 이유: 빈 목록만 보면 "여행" 탭과 D-day 카드가 제대로
 * 그려지는지 확인할 수 없다. 8단계 2번에서 Firestore 구현체로 교체한다.
 *
 * **로그인하지 않았으면 빈 목록을 준다.** 저장된 일정은 계정에 딸린 것이므로
 * 둘러보기 중에 남의 일정처럼 샘플이 보이면 안 된다. Firestore 구현체도 uid가 없으면
 * 조회 자체가 빈 결과이므로, 가짜 단계에서도 같은 모양을 유지한다.
 *
 * @param authState 로그인 상태. StateFlow만 받는다 — Repository가 AuthRepository 전체를
 *        알 필요는 없고, 그러면 두 Repository가 서로를 참조하게 된다.
 */
class FakeSavedTripRepository(
    private val authState: StateFlow<AuthState>,
    /** 오늘. D-day 샘플을 오늘 기준으로 만들어야 "D-12"처럼 의미 있는 값이 나온다. */
    today: LocalDate = LocalDate.now(),
) : SavedTripRepository {

    private val _trips = MutableStateFlow(sampleTrips(today))

    override fun observeAll(): Flow<List<SavedTrip>> =
        combine(_trips, authState) { trips, auth ->
            if (auth.canSave) trips else emptyList()
        }

    override suspend fun save(trip: SavedTrip): Result<Unit> {
        if (!authState.value.canSave) {
            return Result.failure(IllegalStateException(AppContainer.string(R.string.error_sign_in_to_save_trip)))
        }
        // 같은 id면 덮어쓴다. 결과 화면에서 저장을 두 번 눌러도 목록에 두 줄이 생기면 안 된다.
        _trips.value = _trips.value.filterNot { it.id == trip.id } + trip
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        _trips.value = _trips.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun rename(id: String, title: String): Result<Unit> {
        _trips.value = _trips.value.map { if (it.id == id) it.copy(title = title) else it }
        return Result.success(Unit)
    }

    override suspend fun deleteAll(): Result<Unit> {
        _trips.value = emptyList()
        return Result.success(Unit)
    }

    private companion object {

        /** 다가오는 여행 하나, 먼 여행 하나, 지난 여행 하나. 세 경우의 카드를 다 볼 수 있다. */
        fun sampleTrips(today: LocalDate): List<SavedTrip> = listOf(
            trip(
                title = "도쿄 5일",
                destination = Destination("일본", "도쿄", LatLng(35.6762, 139.6503), listOf("HND", "NRT")),
                departure = today.plusDays(12),
                nights = 4,
                budget = 3_000_000,
                purposes = setOf(TravelPurpose.SIGHTSEEING, TravelPurpose.REST),
                savedAt = today.minusDays(3),
            ),
            trip(
                title = "오사카 맛집 투어",
                destination = Destination("일본", "오사카", LatLng(34.6937, 135.5023), listOf("ITM", "KIX")),
                departure = today.plusDays(58),
                nights = 3,
                budget = 2_000_000,
                purposes = setOf(TravelPurpose.FOOD),
                savedAt = today.minusDays(10),
            ),
            trip(
                title = "다낭 휴양",
                destination = Destination("베트남", "다낭", LatLng(16.0544, 108.2022), listOf("DAD")),
                departure = today.minusDays(40),
                nights = 4,
                budget = 1_500_000,
                purposes = setOf(TravelPurpose.REST),
                savedAt = today.minusDays(70),
            ),
        )

        private val INCHEON =
            Destination("대한민국", "인천", LatLng(37.4602, 126.4407), listOf("ICN"))

        /**
         * 샘플 일정 하나.
         *
         * id를 직접 적지 않고 [SavedTrip.idFor]로 만든다 — 결과 화면이 저장할 때 쓰는 것과
         * 같은 규칙이어야 한다. 샘플에 "sample-tokyo" 같은 고정 id를 적었더니, 그 일정을 열어
         * 저장을 누르면 id가 달라서 "도쿄 5일"이 두 장으로 쌓였다.
         */
        private fun trip(
            title: String,
            destination: Destination,
            departure: LocalDate,
            nights: Int,
            budget: Long,
            purposes: Set<TravelPurpose>,
            savedAt: LocalDate,
        ): SavedTrip {
            val returnDate = departure.plusDays(nights.toLong())
            val condition = TripCondition(
                origin = INCHEON,
                destination = destination,
                departureDate = departure,
                returnDate = returnDate,
                budgetWon = budget,
                purposes = purposes,
            )
            return SavedTrip(
                id = SavedTrip.idFor(condition) ?: error("샘플 조건이 덜 채워졌습니다: $title"),
                title = title,
                destinationName = destination.shortName,
                departureDate = departure,
                returnDate = returnDate,
                condition = condition,
                savedAt = savedAt,
            )
        }
    }
}
