package kr.ac.baekseok.readytogo.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.model.canSave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * @param upcoming 아직 안 간 여행. 출발이 가까운 것부터.
 * @param past 지난 여행. 최근에 다녀온 것부터 — 오래된 여행을 먼저 보여줄 이유가 없다.
 */
data class TripsUiState(
    val upcoming: List<SavedTrip> = emptyList(),
    val past: List<SavedTrip> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    /** 저장할 수 있는 상태인지. 목록이 비었을 때 보여줄 문구가 갈린다. */
    val canSave: Boolean = false,
)

class TripsViewModel : ViewModel() {

    private val repository = AppContainer.savedTrips

    val uiState: StateFlow<TripsUiState> = combine(
        repository.observeAll(),
        AppContainer.auth.authState,
    ) { trips, auth ->
            val today = LocalDate.now()
            val (upcoming, past) = trips.partition { it.isUpcoming(today) }
            TripsUiState(
                upcoming = upcoming.sortedBy { it.departureDate },
                past = past.sortedByDescending { it.departureDate },
                today = today,
                canSave = auth.canSave,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS),
            initialValue = TripsUiState(),
        )

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun rename(id: String, title: String) {
        viewModelScope.launch { repository.rename(id, title) }
    }

    private companion object {
        const val FLOW_TIMEOUT_MS = 5_000L
    }
}