package kr.ac.baekseok.readytogo.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.FavoriteKind
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.canSave
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @param filter 고른 종류. null이면 전체.
 * @param visible 필터를 적용한 목록. 화면이 다시 거르지 않도록 여기서 끝낸다.
 */
data class SavedUiState(
    val all: List<FavoritePlace> = emptyList(),
    val visible: List<FavoritePlace> = emptyList(),
    val countsByKind: Map<FavoriteKind, Int> = emptyMap(),
    val filter: FavoriteKind? = null,
    /** 저장할 수 있는 상태인지. 목록이 비었을 때 보여줄 문구가 갈린다. */
    val canSave: Boolean = false,
)

class SavedViewModel : ViewModel() {

    private val repository = AppContainer.favorites

    /**
     * 필터는 화면 상태라 Repository에 두지 않는다.
     * 단, ViewModel에 두어야 탭을 옮겼다 돌아올 때 고른 필터가 남는다.
     */
    private val filter = MutableStateFlow<FavoriteKind?>(null)

    val uiState: StateFlow<SavedUiState> = combine(
        repository.observeAll(),
        filter,
        AppContainer.auth.authState,
    ) { favorites, kind, auth ->
        SavedUiState(
            all = favorites,
            visible = if (kind == null) favorites else favorites.filter { it.kind == kind },
            countsByKind = favorites.groupingBy { it.kind }.eachCount(),
            filter = kind,
            canSave = auth.canSave,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS),
        initialValue = SavedUiState(),
    )

    fun setFilter(kind: FavoriteKind?) {
        filter.value = kind
    }

    fun remove(placeId: String) {
        viewModelScope.launch { repository.remove(placeId) }
    }

    private companion object {
        const val FLOW_TIMEOUT_MS = 5_000L
    }
}