package kr.ac.baekseok.readytogo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * 홈 화면 상태.
 *
 * @param upcoming 가장 가까운 다가오는 여행. D-day 카드에 쓴다. 없으면 카드를 숨긴다.
 * @param recent 최근 저장한 일정. 지난 여행도 포함한다 — 다시 쓰려고 열어보는 경우가 많다.
 */
data class HomeUiState(
    val authState: AuthState = AuthState.Unknown,
    val upcoming: SavedTrip? = null,
    val recent: List<SavedTrip> = emptyList(),
    val today: LocalDate = LocalDate.now(),
)

class HomeViewModel : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        AppContainer.auth.authState,
        AppContainer.savedTrips.observeAll(),
    ) { auth, trips ->
        val today = LocalDate.now()
        HomeUiState(
            authState = auth,
            // 다가오는 여행이 여럿이면 가장 가까운 것 하나만 보여준다.
            // 카드를 여러 개 쌓으면 "다음에 갈 곳"이라는 의미가 흐려진다.
            upcoming = trips.filter { it.isUpcoming(today) }.minByOrNull { it.departureDate },
            recent = trips.sortedByDescending { it.savedAt }.take(RECENT_COUNT),
            today = today,
        )
    }.stateIn(
        scope = viewModelScope,
        // 화면을 잠깐 벗어났다 돌아올 때 목록을 다시 만들지 않도록 잠시 유지한다.
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    private companion object {
        const val RECENT_COUNT = 5
        const val FLOW_TIMEOUT_MS = 5_000L
    }
}