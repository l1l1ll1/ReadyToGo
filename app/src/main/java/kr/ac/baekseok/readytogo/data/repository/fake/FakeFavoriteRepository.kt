package kr.ac.baekseok.readytogo.data.repository.fake

import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.FavoriteKind
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.canSave
import kr.ac.baekseok.readytogo.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 즐겨찾기 Mock — 메모리에만 둔다.
 *
 * 샘플 2건을 넣어두는 이유: "저장" 탭이 빈 상태만 보이면 목록 카드가 제대로 그려지는지 모른다.
 * 8단계 2번에서 Firestore 구현체로 교체한다.
 *
 * **로그인하지 않았으면 빈 목록을 준다.** 즐겨찾기도 저장이므로 둘러보기 중에는 쓸 수 없다
 * (CLAUDE.md 앱 구조 → 로그인). 저장된 일정과 같은 규칙이다.
 */
class FakeFavoriteRepository(
    private val authState: StateFlow<AuthState>,
) : FavoriteRepository {

    private val _favorites = MutableStateFlow(SAMPLES)

    private val visible: Flow<List<FavoritePlace>> =
        combine(_favorites, authState) { favorites, auth ->
            if (auth.canSave) favorites else emptyList()
        }

    override fun observeAll(): Flow<List<FavoritePlace>> = visible

    override fun observeIds(): Flow<Set<String>> =
        visible.map { list -> list.map { it.placeId }.toSet() }

    override suspend fun add(place: FavoritePlace): Result<Unit> {
        if (!authState.value.canSave) {
            return Result.failure(IllegalStateException("로그인하면 장소를 저장할 수 있습니다."))
        }
        // 같은 장소를 두 번 담지 않는다. ♥를 연타해도 목록에 한 줄만 남아야 한다.
        if (_favorites.value.none { it.placeId == place.placeId }) {
            _favorites.value = _favorites.value + place
        }
        return Result.success(Unit)
    }

    override suspend fun remove(placeId: String): Result<Unit> {
        _favorites.value = _favorites.value.filterNot { it.placeId == placeId }
        return Result.success(Unit)
    }

    override suspend fun deleteAll(): Result<Unit> {
        _favorites.value = emptyList()
        return Result.success(Unit)
    }

    private companion object {
        val SAMPLES = listOf(
            FavoritePlace(
                placeId = "sample-fav-1",
                name = "츠지한 니혼바시 본점",
                category = "일본 음식점",
                kind = FavoriteKind.RESTAURANT,
                location = LatLng(35.6833, 139.7744),
                cityName = "도쿄",
                mapUrl = "https://maps.google.com/?q=%EC%B8%A0%EC%A7%80%ED%95%9C",
            ),
            FavoritePlace(
                placeId = "sample-fav-2",
                name = "신주쿠 교엔",
                category = "공원",
                kind = FavoriteKind.ATTRACTION,
                location = LatLng(35.6852, 139.7100),
                cityName = "도쿄",
                mapUrl = "https://maps.google.com/?q=%EC%8B%A0%EC%A3%BC%EC%BF%A0+%EA%B5%90%EC%97%94",
            ),
        )
    }
}
