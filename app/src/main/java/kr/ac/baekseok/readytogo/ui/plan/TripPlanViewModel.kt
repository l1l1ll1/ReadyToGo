package kr.ac.baekseok.readytogo.ui.plan

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.USE_REAL_API
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.data.model.canSave
import kr.ac.baekseok.readytogo.data.repository.flight.SerpApiFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockPlaceRepository
import kr.ac.baekseok.readytogo.data.repository.places.GooglePlacesLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.places.GooglePlacesRepository
import kr.ac.baekseok.readytogo.domain.plan.TripPlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 사용자가 타임라인에서 손본 것. 일차 번호 → 항목 키 집합.
 *
 * 계획 자체는 그대로 두고 이 두 집합만 바꿔서 화면을 다시 그린다.
 */
data class TimelineEdits(
    val deleted: Map<Int, Set<String>> = emptyMap(),
    val swappedMeals: Map<Int, Set<String>> = emptyMap(),
) {
    fun deletedIn(dayNumber: Int): Set<String> = deleted[dayNumber].orEmpty()
    fun swappedIn(dayNumber: Int): Set<String> = swappedMeals[dayNumber].orEmpty()
}

/** 결과 화면의 상태. Repository가 실제 API처럼 시간이 걸리므로 로딩과 실패를 구분해 들고 있어야 한다. */
sealed interface TripPlanUiState {
    data object Loading : TripPlanUiState
    data class Success(val plan: TripPlan) : TripPlanUiState
    data class Failure(val message: String) : TripPlanUiState
}

/**
 * 결과 화면 상태 관리.
 *
 * 기본값으로 Mock 구현체를 물려둔다. **7단계에서 실제 API로 갈아끼울 곳이 여기 한 군데다** —
 * 화면 코드는 TripPlanner만 알고 있어서 어느 구현체가 붙었는지 모른다(CLAUDE.md).
 */
class TripPlanViewModel(
    application: Application,
) : AndroidViewModel(application) {

    /**
     * 7단계 교체 지점. 하나씩 실물로 바꾼다.
     *
     * 장소·숙소는 Places API, 항공은 SerpApi로 바뀌었다.
     * 키 문제나 할당량 초과로 되돌려야 하면 해당 줄을 Mock 구현체로 바꾸면 된다.
     * (항공은 SerpApiFlightRepository가 실패 시 알아서 Mock으로 넘어간다.)
     */
    private val planner: TripPlanner = TripPlanner(
        // USE_REAL_API가 false면 호출이 한 번도 나가지 않는다. 기본값이 false다 — 이유는
        // ApiMode.kt 주석 참고. 켜면 이 세 줄이 전부 실제 구현체로 바뀐다.
        flightRepository = if (USE_REAL_API) {
            SerpApiFlightRepository(application)
        } else {
            MockFlightRepository()
        },
        lodgingRepository = if (USE_REAL_API) {
            GooglePlacesLodgingRepository(application)
        } else {
            MockLodgingRepository()
        },
        placeRepository = if (USE_REAL_API) {
            GooglePlacesRepository(application)
        } else {
            MockPlaceRepository()
        },
        itineraryRepository = MockItineraryRepository(),
        // Mock 식당은 15곳뿐이라 5일 일정(30칸)에서 반드시 소진된다. 테스트 중 화면이 비지 않게
        // 재사용을 허용한다. 실제 API 경로에서는 같은 집을 두 번 추천하지 않는다.
        allowRestaurantReuse = !USE_REAL_API,
    )

    init {
        Log.d(TAG, if (USE_REAL_API) "실제 API 사용" else "Mock 사용 — 외부 호출 없음")
    }

    private val _uiState = MutableStateFlow<TripPlanUiState>(TripPlanUiState.Loading)
    val uiState: StateFlow<TripPlanUiState> = _uiState.asStateFlow()

    /**
     * 즐겨찾기한 장소 id.
     *
     * 목록 전체가 아니라 id 집합을 보는 이유: 카드 하나하나가 "내가 담은 곳인가"를 알아야 하는데,
     * 카드마다 목록을 훑으면 10일 일정에서 수백 번 비교하게 된다.
     */
    val favoriteIds: StateFlow<Set<String>> = AppContainer.favorites.observeIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), emptySet())

    // 저장 가능 여부로 ♥를 비활성화하지는 않는다. 눌리지 않는 버튼은 왜 안 되는지 알려주지 못해서,
    // 누르게 두고 Repository가 돌려주는 "로그인하면 저장할 수 있습니다"를 [message]로 띄운다.

    /** 이 일정이 이미 저장돼 있는지. 저장 버튼의 모양이 갈린다. */
    val saved: StateFlow<Boolean> = combine(
        AppContainer.savedTrips.observeAll(),
        _uiState,
    ) { trips, _ ->
        val id = loadedCondition?.let { SavedTrip.idFor(it) }
        id != null && trips.any { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), false)

    /** 저장·즐겨찾기를 눌렀을 때 화면에 띄울 한 줄. 띄운 뒤 화면이 [clearMessage]로 비운다. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * 사용자가 일정에서 손본 것.
     *
     * **계획을 다시 만들지 않고 위에 덮어쓴다.** 항목 하나를 지울 때마다 다시 만들면
     * Places·SerpApi 호출이 또 나가고(비용), 다른 날 추천까지 바뀌어버린다.
     *
     * 일차 번호별로 들고 있는 이유: 같은 끼니 키("meal-LUNCH")가 모든 날에 있어서
     * 날짜를 구분하지 않으면 1일차 점심을 지울 때 전부 지워진다.
     */
    private val _edits = MutableStateFlow(TimelineEdits())
    val edits: StateFlow<TimelineEdits> = _edits.asStateFlow()

    /** 이미 만든 조건을 다시 요청하지 않도록 기억해둔다. 화면 회전이나 재구성 때 중복 호출을 막는다. */
    private var loadedCondition: TripCondition? = null

    fun load(condition: TripCondition) {
        if (loadedCondition == condition && _uiState.value !is TripPlanUiState.Failure) return
        loadedCondition = condition

        viewModelScope.launch {
            _uiState.value = TripPlanUiState.Loading

            // 계획 하나를 만드는 데 걸리는 시간. 검색을 병렬로 바꾸는 등의 개선 효과를
            // 눈이 아니라 숫자로 확인하기 위해 남긴다.
            val startedAt = SystemClock.elapsedRealtime()
            val result = planner.createPlan(condition)
            Log.d(TAG, "계획 생성 ${SystemClock.elapsedRealtime() - startedAt}ms")

            _uiState.value = result.fold(
                onSuccess = { TripPlanUiState.Success(it) },
                onFailure = { TripPlanUiState.Failure(it.message ?: "계획을 만들지 못했습니다.") },
            )
        }
    }

    /** 실패했을 때 다시 시도. */
    fun retry() {
        val condition = loadedCondition ?: return
        loadedCondition = null
        load(condition)
    }

    /**
     * 이 일정을 "여행" 탭에 저장한다. 이미 저장돼 있으면 지운다.
     *
     * 계획 전체가 아니라 조건만 저장한다 — [SavedTrip] 주석 참고.
     */
    fun toggleSaved() {
        val condition = loadedCondition ?: return
        val destination = condition.destination ?: return
        val departure = condition.departureDate ?: return
        val returnDate = condition.returnDate ?: return
        val id = SavedTrip.idFor(condition) ?: return

        viewModelScope.launch {
            if (saved.value) {
                AppContainer.savedTrips.delete(id)
                _message.value = "저장을 해제했습니다."
                return@launch
            }

            val trip = SavedTrip(
                id = id,
                // 기본 이름은 "도쿄 5일"처럼 자동으로 만든다. 이름을 먼저 물으면 저장이 번거로워진다.
                title = "${destination.city} ${ChronoUnit.DAYS.between(departure, returnDate) + 1}일",
                destinationName = destination.city,
                departureDate = departure,
                returnDate = returnDate,
                condition = condition,
                savedAt = LocalDate.now(),
            )
            AppContainer.savedTrips.save(trip)
                .onSuccess { _message.value = "여행 탭에 저장했습니다." }
                .onFailure { _message.value = it.message ?: "저장하지 못했습니다." }
        }
    }

    /** 식당·관광지 ♥를 켜고 끈다. */
    fun toggleFavorite(place: FavoritePlace) {
        viewModelScope.launch {
            if (place.placeId in favoriteIds.value) {
                AppContainer.favorites.remove(place.placeId)
                return@launch
            }
            AppContainer.favorites.add(place)
                .onFailure { _message.value = it.message ?: "저장하지 못했습니다." }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /**
     * 타임라인에서 항목을 뺀다. 지운 것은 계획에 남아 있고 화면에서만 빠진다.
     *
     * 방금 뺀 것을 [lastDeleted]에 남겨 되돌릴 수 있게 한다 — 되돌리기가 없으면 실수로 뺐을 때
     * 계획을 처음부터 다시 만들어야 하고, 그건 Places·SerpApi 호출이 또 나가는 일이다.
     */
    fun deleteTimelineItem(dayNumber: Int, key: String) {
        _edits.update { edits ->
            edits.copy(deleted = edits.deleted.plusKey(dayNumber, key))
        }
        _lastDeleted.value = dayNumber to key
        _message.value = "일정에서 뺐습니다."
    }

    /** 방금 뺀 항목. 되돌리기를 보여줄 근거다. */
    private val _lastDeleted = MutableStateFlow<Pair<Int, String>?>(null)
    val lastDeleted: StateFlow<Pair<Int, String>?> = _lastDeleted.asStateFlow()

    fun undoDelete() {
        val (dayNumber, key) = _lastDeleted.value ?: return
        _edits.update { edits ->
            edits.copy(deleted = edits.deleted.minusKey(dayNumber, key))
        }
        _lastDeleted.value = null
    }

    /** 되돌리기 기회가 지났다. 스낵바가 사라질 때 화면이 부른다. */
    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

    /** 끼니를 대안으로 교체한다. 다시 누르면 원래 추천으로 돌아온다. */
    fun toggleMealSwap(dayNumber: Int, key: String) {
        _edits.update { edits ->
            val current = edits.swappedIn(dayNumber)
            val next = if (key in current) {
                edits.swappedMeals.minusKey(dayNumber, key)
            } else {
                edits.swappedMeals.plusKey(dayNumber, key)
            }
            edits.copy(swappedMeals = next)
        }
    }

    /** 일차별 집합에 키를 넣고 빼는 잔손질. 세 함수에서 같은 코드를 반복하지 않으려고 뺐다. */
    private fun Map<Int, Set<String>>.plusKey(dayNumber: Int, key: String) =
        this + (dayNumber to (this[dayNumber].orEmpty() + key))

    private fun Map<Int, Set<String>>.minusKey(dayNumber: Int, key: String) =
        this + (dayNumber to (this[dayNumber].orEmpty() - key))

    private companion object {
        const val TAG = "TripPlan"
        const val FLOW_TIMEOUT_MS = 5_000L
    }
}