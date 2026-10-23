package kr.ac.baekseok.readytogo.ui.plan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.FavoriteKind
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.data.model.canSave
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.repository.DayEditException
import kr.ac.baekseok.readytogo.domain.edit.EditCommand
import kr.ac.baekseok.readytogo.domain.edit.RejectReason
import kr.ac.baekseok.readytogo.domain.edit.applyCommands
import kr.ac.baekseok.readytogo.domain.edit.summarizeDay
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.generation.PlanGeneration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 결과 화면의 상태. Repository가 실제 API처럼 시간이 걸리므로 로딩과 실패를 구분해 들고 있어야 한다. */
sealed interface TripPlanUiState {
    data object Loading : TripPlanUiState
    data class Success(val plan: TripPlan) : TripPlanUiState
    data class Failure(val message: String) : TripPlanUiState
}

/**
 * 결과 화면 상태 관리.
 *
 * 일정 생성 자체는 [PlanGenerator]가 맡는다. 여기는 그 결과를 받아 보여주고,
 * 사용자가 손본 것(편집·저장·즐겨찾기)을 관리한다.
 */
class TripPlanViewModel(
    application: Application,
) : AndroidViewModel(application) {

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

    /**
     * 저장한 장소 전체. 상세 시트의 "내가 저장한 곳"이 쓴다.
     *
     * id 집합([favoriteIds])과 따로 두는 이유: ♥를 칠할 때는 id만 있으면 되지만,
     * 끼워 넣을 후보로 보여주려면 이름·좌표·종류가 필요하다.
     */
    val favorites: StateFlow<List<FavoritePlace>> = AppContainer.favorites.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), emptyList())

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

    /**
     * 기간을 바꿀 수 있는지. **저장된 일정이고 아직 출발 전**일 때만.
     *
     * 저장하지 않은 계획은 바꿀 문서가 없다 — 입력 화면으로 돌아가 새로 만들면 된다.
     */
    val canChangeDates: StateFlow<Boolean> = combine(
        AppContainer.savedTrips.observeAll(),
        _uiState,
    ) { trips, _ ->
        val id = loadedCondition?.let { SavedTrip.idFor(it) }
        trips.firstOrNull { it.id == id }?.canChangeDates(LocalDate.now()) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), false)

    /**
     * 기간을 바꿔 새로 열어야 할 조건. 화면이 받아서 내비게이션에 담고 [clearConditionChange]로 비운다.
     *
     * **여기서 바로 load하지 않는 이유**: 결과 화면의 조건은 내비게이션 항목에 담겨 있다.
     * 그쪽을 안 바꾸면 화면을 돌리거나 다시 만들 때 옛 조건으로 load가 다시 불려
     * 지워진 문서의 일정으로 되돌아간다.
     */
    private val _conditionChange = MutableStateFlow<TripCondition?>(null)
    val conditionChange: StateFlow<TripCondition?> = _conditionChange.asStateFlow()

    fun clearConditionChange() {
        _conditionChange.value = null
    }

    /** 저장·즐겨찾기를 눌렀을 때 화면에 띄울 한 줄. 띄운 뒤 화면이 [clearMessage]로 비운다. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * 둘러보기 중에 저장을 눌렀다. 화면이 로그인 버튼이 달린 안내를 띄운다.
     *
     * [message]와 따로 두는 이유: 이 안내에는 "로그인" 버튼이 붙고, 일반 메시지의
     * "되돌리기" 버튼과 겹칠 수 없다. 한 칸에 섞으면 어느 버튼을 달지 매번 따져야 한다.
     */
    private val _signInRequested = MutableStateFlow(false)
    val signInRequested: StateFlow<Boolean> = _signInRequested.asStateFlow()

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

        // 저장해 둔 편집을 꺼내 온다. **계획 만들기와 나란히 돌린다** — 계획이 몇 초
        // 걸리므로 편집은 그 전에 도착한다. 순서대로 기다리면 저장하지 않은 계획에서
        // 제한 시간만큼 화면이 늦게 뜬다.
        viewModelScope.launch { _edits.value = restoreEdits(condition) }

        // 생성은 앱 범위에서 돈다. 같은 조건이 이미 돌고 있거나 끝나 있으면 생성기가 새로 시작하지
        // 않으므로, 앱을 나갔다 와서 이 화면이 새로 만들어져도 진행 중인 것에 그대로 이어붙는다.
        val generator = AppContainer.planGenerator
        _uiState.value = TripPlanUiState.Loading
        generator.start(condition)

        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            generator.state.collect { state ->
                // 다른 조건의 생성은 이 화면과 무관하다 — 지금 보여주는 것을 그대로 둔다.
                if (state !is PlanGeneration.Done || state.condition != condition) return@collect
                _uiState.value = state.result.fold(
                    onSuccess = { TripPlanUiState.Success(it) },
                    onFailure = { TripPlanUiState.Failure(it.message ?: string(R.string.plan_create_failed)) },
                )
            }
        }
    }

    /** 생성기 상태를 지켜보는 코루틴. 다시 시도할 때 겹쳐 돌지 않게 하나만 둔다. */
    private var watchJob: Job? = null

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
                _message.value = string(R.string.plan_save_removed)
                return@launch
            }

            val trip = SavedTrip(
                id = id,
                // 기본 이름은 "도쿄 5일"처럼 자동으로 만든다. 이름을 먼저 물으면 저장이 번거로워진다.
                title = savedTripTitle(
                    destination.shortName,
                    (ChronoUnit.DAYS.between(departure, returnDate) + 1).toInt(),
                ),
                destinationName = destination.shortName,
                departureDate = departure,
                returnDate = returnDate,
                condition = condition,
                savedAt = LocalDate.now(),
                // 지금까지 손본 것을 같이 올린다. 편집하고 나서 저장하는 순서가 흔하다.
                edits = _edits.value,
                coverPlaceId = coverPlaceId(),
            )

            // 둘러보기 중이면 저장할 곳이 없다. 일정을 기억해 두고 로그인을 권한다 —
            // 로그인 뒤에 AppContainer가 이 일정을 알아서 저장한다. 다시 입력하지 않아도 된다.
            if (!AppContainer.auth.authState.value.canSave) {
                AppContainer.rememberPendingTrip(trip)
                _signInRequested.value = true
                return@launch
            }

            AppContainer.savedTrips.save(trip)
                .onSuccess { _message.value = string(R.string.plan_saved) }
                .onFailure { _message.value = it.message ?: string(R.string.plan_save_failed) }
        }
    }

    /**
     * 저장된 일정의 기간을 바꾼다.
     *
     * 일수가 같으면 편집을 그대로 가져가고, 다르면 비운다(확인은 화면이 먼저 받는다).
     * 어느 쪽이든 새 조건으로 다시 만든다 — 항공만 다시 받아 끼워 넣으면 지금 화면은 맞아도,
     * 다음에 열 때 조건으로 전체를 다시 만들면서 끼니 예산·도착 공항이 달라져 결과가 어긋난다.
     * 관광지·식당 검색은 위치 기준 캐시에 걸려 새로 나가는 호출은 항공 1회뿐이다.
     *
     * D-day 알림은 손대지 않는다. 저장 목록이 바뀌면 AppContainer가 전부 다시 예약한다.
     */
    fun changeDates(departure: LocalDate, returning: LocalDate) {
        val condition = loadedCondition ?: return
        val oldId = SavedTrip.idFor(condition) ?: return

        viewModelScope.launch {
            val trips = AppContainer.savedTrips.observeAll().first()
            val trip = trips.firstOrNull { it.id == oldId } ?: return@launch

            if (!trip.canChangeDates(LocalDate.now())) {
                _message.value = string(R.string.plan_dates_departed)
                return@launch
            }
            if (departure == trip.departureDate && returning == trip.returnDate) return@launch

            val moved = trip.withDates(departure, returning) { days ->
                savedTripTitle(trip.destinationName, days)
            }
            // 같은 조건·같은 날짜로 저장된 일정이 이미 있으면 덮어쓰지 않는다. 그쪽 편집이 날아간다.
            if (trips.any { it.id == moved.id }) {
                _message.value = string(R.string.plan_dates_duplicate)
                return@launch
            }

            AppContainer.savedTrips.replace(oldId, moved)
                .onSuccess {
                    // 직전에 뺀 항목의 되돌리기 기회를 닫는다. 남아 있으면 이 안내에 "되돌리기"가
                    // 붙는데, 눌러도 되돌아가는 것은 기간이 아니라 엉뚱한 삭제다(에뮬레이터에서 봤다).
                    _undoEdits.value = null
                    if (moved.dayCount == trip.dayCount) {
                        _message.value = string(R.string.plan_dates_moved)
                    }
                    _conditionChange.value = moved.condition
                }
                .onFailure { _message.value = it.message ?: string(R.string.plan_save_failed) }
        }
    }

    /**
     * 카드에 띄울 대표 장소. **1일차 첫 관광지, 없으면 1일차 첫 식당.**
     *
     * 지금 손에 있는 계획에서 id만 집어 온다 — 도시 사진을 따로 검색하지 않으므로
     * 추가 호출이 0회다. 사진 자체는 카드가 화면에 보일 때 한 번만 받는다.
     *
     * 1일차를 쓰는 이유: 어느 날을 골라도 상관없다면 가장 앞의 것이 설명하기 쉽고,
     * 나중에 일정을 편집해도 대표 장소가 널뛰지 않는다. 관광지를 먼저 보는 이유는
     * 도시를 알아보게 하는 사진이 식당보다 관광지 쪽일 가능성이 크기 때문이다.
     */
    private fun coverPlaceId(): String? {
        val day = (_uiState.value as? TripPlanUiState.Success)?.plan?.dailyPlans?.firstOrNull()
            ?: return null
        return day.attractions.firstOrNull()?.id
            ?: day.meals.values.firstOrNull()?.qualityFirst?.item?.id
    }

    /** 식당·관광지 ♥를 켜고 끈다. */
    fun toggleFavorite(place: FavoritePlace) {
        viewModelScope.launch {
            if (place.placeId in favoriteIds.value) {
                AppContainer.favorites.remove(place.placeId)
                return@launch
            }
            AppContainer.favorites.add(place)
                .onFailure { _message.value = it.message ?: string(R.string.plan_save_failed) }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearSignInRequest() {
        _signInRequested.value = false
    }

    /**
     * 타임라인에서 항목을 뺀다. 지운 것은 계획에 남아 있고 화면에서만 빠진다.
     *
     * 뺀 직전의 편집을 [canUndo] 뒤에 남겨 되돌릴 수 있게 한다 — 되돌리기가 없으면 실수로 뺐을 때
     * 계획을 처음부터 다시 만들어야 하고, 그건 Places·SerpApi 호출이 또 나가는 일이다.
     */
    fun deleteTimelineItem(dayNumber: Int, key: String) {
        _undoEdits.value = _edits.value
        updateEdits { edits ->
            edits.copy(deleted = edits.deleted.plusKey(dayNumber, key))
        }
        _message.value = string(R.string.plan_item_removed)
    }

    /**
     * 되돌릴 때 돌아갈 편집. 되돌리기 버튼을 보여줄 근거다.
     *
     * **뺀 키 하나가 아니라 편집 전체를 들고 있는다.** 말로 고치기는 한 번에 빼기·끼니 교체·
     * 순서 변경을 함께 하는데, 키 하나만 기억하면 그중 일부만 되돌아간다.
     */
    private val _undoEdits = MutableStateFlow<TimelineEdits?>(null)
    val canUndo: StateFlow<Boolean> = _undoEdits.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun undoEdit() {
        val previous = _undoEdits.value ?: return
        updateEdits { previous }
        _undoEdits.value = null
    }

    /** 되돌리기 기회가 지났다. 스낵바가 사라질 때 화면이 부른다. */
    fun clearUndo() {
        _undoEdits.value = null
    }

    /** 말로 고치기를 쓸 수 있는지. 함수가 로그인 토큰을 요구한다. */
    val canEditByText: StateFlow<Boolean> = AppContainer.auth.authState.map { it.canSave }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS), false)

    /** 함수 응답을 기다리는 중. 입력창이 버튼 대신 진행 표시를 띄우고 중복 전송을 막는다. */
    private val _editingByText = MutableStateFlow(false)
    val editingByText: StateFlow<Boolean> = _editingByText.asStateFlow()

    /**
     * 문장으로 그날 일정을 고친다(⑨-1).
     *
     * 함수는 명령만 돌려주고, 검증과 실행은 [applyCommands]가 손 편집과 같은 규칙으로 한다.
     * 명령이 여러 개여도 저장은 한 번, 되돌리기도 한 번이다.
     */
    fun editByText(dayNumber: Int, sentence: String) {
        val text = sentence.trim()
        if (text.isEmpty() || _editingByText.value) return
        val plan = (_uiState.value as? TripPlanUiState.Success)?.plan ?: return
        val day = plan.dailyPlans.firstOrNull { it.dayNumber == dayNumber } ?: return
        val lodging = plan.lodging?.qualityFirst?.item?.location

        viewModelScope.launch {
            _editingByText.value = true
            val before = _edits.value
            val entries = day.toTimeline(
                deletedKeys = before.deletedIn(dayNumber),
                pickedMeals = before.pickedIn(dayNumber),
                customOrder = before.orderIn(dayNumber),
                pickedAttractions = before.pickedAttractionsIn(dayNumber),
                picked = _pickedPlaces.value,
            )
            val summary = summarizeDay(entries, lodging)

            AppContainer.dayEditAssistant.interpret(text, summary)
                .onSuccess { reply ->
                    // 비용 확인용. 문장은 남기지 않는다.
                    Log.d(TAG, "editByText 명령 ${reply.commands} 토큰 in=${reply.inputTokens} out=${reply.outputTokens}")
                    applyTextEdit(day, before, reply.commands, summary.keyByAlias, lodging)
                }
                .onFailure { _message.value = string(textEditFailureRes(it)) }
            _editingByText.value = false
        }
    }

    private fun applyTextEdit(
        day: DayPlan,
        before: TimelineEdits,
        commands: List<EditCommand>,
        keyByAlias: Map<String, String>,
        lodging: LatLng?,
    ) {
        if (commands.isEmpty()) {
            _message.value = string(R.string.plan_text_edit_not_understood)
            return
        }
        // 기다리는 동안 손으로 고친 것이 있으면 그 위에 얹는다. before에 얹으면 그 사이 편집이 사라진다.
        val result = applyCommands(day, _edits.value, commands, keyByAlias, lodging, _pickedPlaces.value)
        val skipped = result.rejections.joinToString(", ") { string(it.reason.labelRes()) }

        if (result.appliedCount == 0) {
            _message.value = string(R.string.plan_text_edit_none_applied, skipped)
            return
        }
        _undoEdits.value = _edits.value
        updateEdits { result.edits }
        _message.value = if (result.rejections.isEmpty()) {
            string(R.string.plan_text_edit_applied, result.appliedCount)
        } else {
            string(R.string.plan_text_edit_partly_applied, result.appliedCount, skipped)
        }
    }

    private fun RejectReason.labelRes(): Int = when (this) {
        RejectReason.UNKNOWN_ITEM -> R.string.plan_text_edit_reject_unknown
        RejectReason.MEAL_FIXED -> R.string.plan_text_edit_reject_meal_fixed
        RejectReason.BAD_POSITION -> R.string.plan_text_edit_reject_position
        RejectReason.NO_CANDIDATE -> R.string.plan_text_edit_reject_no_candidate
    }

    private fun textEditFailureRes(error: Throwable): Int =
        when ((error as? DayEditException)?.reason) {
            DayEditException.Reason.SIGN_IN_REQUIRED -> R.string.plan_text_edit_sign_in
            DayEditException.Reason.TOO_MANY_REQUESTS -> R.string.plan_text_edit_too_many
            else -> R.string.plan_text_edit_unavailable
        }

    /**
     * 여행 중 모드에서 항목을 "했음"으로 체크한다. 다시 누르면 해제.
     *
     * 지우기와 달리 되돌리기 안내를 띄우지 않는다 — 체크는 한 번 더 누르면 풀리고,
     * 걷는 중에 스낵바가 매번 올라오면 방해만 된다.
     */
    fun toggleDone(dayNumber: Int, key: String) {
        updateEdits { edits ->
            val next = if (key in edits.doneIn(dayNumber)) {
                edits.done.minusKey(dayNumber, key)
            } else {
                edits.done.plusKey(dayNumber, key)
            }
            edits.copy(done = next)
        }
    }

    /**
     * 끼니를 다른 후보로 바꾼다.
     *
     * 추천(점수 1등)을 다시 고르면 기록에서 지운다 — 기본값과 같은 값을 들고 있을 이유가 없고,
     * 저장할 때도 쓸데없는 항목이 남지 않는다.
     */
    fun pickMeal(dayNumber: Int, mealKey: String, placeId: String?) {
        updateEdits { edits ->
            val current = edits.pickedIn(dayNumber)
            val next = if (placeId == null) current - mealKey else current + (mealKey to placeId)
            edits.copy(pickedMeals = edits.pickedMeals + (dayNumber to next))
        }
    }

    /**
     * 끌어서 바뀐 순서를 기록한다.
     *
     * 화면이 이미 제약(끼니는 고정, 관광지만 이동)을 지켜 만든 순서를 그대로 받는다.
     * 여기서 다시 검사하지 않는 이유: 규칙이 두 곳에 생기면 어긋난다.
     */
    fun reorderDay(dayNumber: Int, keys: List<String>) {
        // **여기서는 저장하지 않는다.** 끌기 한 번에 이 함수가 수십 번 불린다.
        // 저장은 손을 뗄 때 commitEdits가 한 번만 한다.
        _edits.update { it.copy(order = it.order + (dayNumber to keys)) }
    }

    /** 끌기가 끝났다. 지금까지 바뀐 순서를 저장한다. */
    fun commitEdits() {
        persistEdits(_edits.value)
    }

    /**
     * 즐겨찾기에서 받아 온 장소들. 앱이 떠 있는 동안만 산다.
     *
     * 편집에는 id만 남고 실제 장소는 여기 있다([PickedPlaces] 주석). 다시 켜면 비어
     * 있다가, 그 자리를 그릴 때 다시 받아 온다.
     */
    private val _pickedPlaces = MutableStateFlow(PickedPlaces())
    val pickedPlaces: StateFlow<PickedPlaces> = _pickedPlaces.asStateFlow()

    /**
     * 즐겨찾기한 곳을 일정에 끼워 넣는다.
     *
     * **Places 상세 조회가 나가는 유일한 자리다.** 목록을 펼치거나 탭을 여는 것으로는
     * 호출되지 않는다 — 조회마다 과금되기 때문이다. 같은 곳을 다시 고르면 Repository가
     * 메모리에 들고 있던 것을 돌려줘서 호출이 없다.
     *
     * 먼저 장소를 받아 둔 다음 편집을 기록한다. 순서가 반대면 아직 못 받은 id를 가리키는
     * 자리가 한 번 그려져서 원래 추천이 깜빡 보인다.
     */
    fun pickFavorite(dayNumber: Int, slotKey: String, favorite: FavoritePlace) {
        val details = AppContainer.placeDetails ?: return

        viewModelScope.launch {
            when (favorite.kind) {
                FavoriteKind.RESTAURANT -> {
                    val restaurant = details.restaurantFor(favorite)
                    _pickedPlaces.update {
                        it.copy(restaurants = it.restaurants + (favorite.placeId to restaurant))
                    }
                    updateEdits { edits ->
                        val next = edits.pickedIn(dayNumber) + (slotKey to favorite.placeId)
                        edits.copy(pickedMeals = edits.pickedMeals + (dayNumber to next))
                    }
                }

                else -> {
                    val attraction = details.attractionFor(favorite)
                    _pickedPlaces.update {
                        it.copy(attractions = it.attractions + (favorite.placeId to attraction))
                    }
                    updateEdits { edits ->
                        val next = edits.pickedAttractionsIn(dayNumber) + (slotKey to favorite.placeId)
                        edits.copy(pickedAttractions = edits.pickedAttractions + (dayNumber to next))
                    }
                }
            }
        }
    }

    /**
     * 편집을 저장된 일정에 붙인다.
     *
     * **저장해 둔 일정일 때만 올린다.** 저장하지 않은 계획에 편집만 올려 둘 곳이 없다.
     * 편집할 때마다 쓰기가 한 번 나가지만, 일정 문서 하나를 덮어쓰는 것이라 가볍다.
     */
    /**
     * 편집을 바꾸고 저장까지 한다.
     *
     * 바꾸는 곳마다 저장을 잊지 않도록 한 군데로 묶었다 — 삭제는 저장되는데 순서는 안 되는
     * 식의 어긋남이 생기기 쉽다.
     */
    private fun updateEdits(transform: (TimelineEdits) -> TimelineEdits) {
        val next = transform(_edits.value)
        _edits.value = next
        persistEdits(next)
    }

    /**
     * 저장된 일정에 붙어 있는 편집. 저장한 적 없으면 빈 값.
     *
     * **첫 방출을 그냥 쓰면 안 된다.** 로그인 상태가 잡히기 전에는 빈 목록이 먼저 오고,
     * 그걸 집으면 저장해 둔 편집을 놓친다. 실제로 편집이 사라지는 것을 봤다.
     * 찾는 일정이 들어 있는 방출까지 기다리되, 없는 경우를 위해 시간 제한을 둔다.
     *
     * 둘러보기 중이거나 아직 저장하지 않은 계획은 꺼낼 곳이 없다. 그때는 제한 시간이
     * 지나고 빈 값으로 돌아간다 — 이번 화면에서만 사는 편집이 된다.
     */
    private suspend fun restoreEdits(condition: TripCondition): TimelineEdits {
        val id = SavedTrip.idFor(condition) ?: return TimelineEdits()

        val trips = withTimeoutOrNull(RESTORE_EDITS_TIMEOUT_MS) {
            AppContainer.savedTrips.observeAll().first { list -> list.any { it.id == id } }
        }
        return trips?.firstOrNull { it.id == id }?.edits ?: TimelineEdits()
    }

    private fun persistEdits(edits: TimelineEdits) {
        val condition = loadedCondition ?: return
        val id = SavedTrip.idFor(condition) ?: return
        if (!saved.value) return

        viewModelScope.launch {
            val trip = AppContainer.savedTrips.observeAll().first().firstOrNull { it.id == id }
                ?: return@launch
            AppContainer.savedTrips.save(trip.copy(edits = edits))
        }
    }

    /** 일차별 집합에 키를 넣고 빼는 잔손질. 지우기·되돌리기·체크가 같은 코드를 쓴다. */
    private fun Map<Int, Set<String>>.plusKey(dayNumber: Int, key: String) =
        this + (dayNumber to (this[dayNumber].orEmpty() + key))

    private fun Map<Int, Set<String>>.minusKey(dayNumber: Int, key: String) =
        this + (dayNumber to (this[dayNumber].orEmpty() - key))

    /**
     * 문자열 리소스를 읽는다.
     *
     * 스낵바 문구는 ViewModel이 만들어 화면으로 넘긴다. 번역된 글자를 여기서 집어야
     * 하는데 Composable이 아니라 stringResource를 못 쓴다 — Application Context로 읽는다.
     */
    private fun string(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    /**
     * 저장할 때 자동으로 붙는 이름. "도쿄 5일" / "Tokyo, 5 days"
     *
     * 날짜 수는 plurals라 따로 뽑는다 — 영어에서 "1 days"가 되지 않게 한다.
     */
    private fun savedTripTitle(destination: String, days: Int): String {
        val resources = getApplication<Application>().resources
        val daysText = resources.getQuantityString(R.plurals.days_count, days, days)
        return string(R.string.plan_saved_title, destination, daysText)
    }

    private companion object {
        const val TAG = "TripPlan"
        const val FLOW_TIMEOUT_MS = 5_000L

        /**
         * 저장된 편집을 기다리는 시간.
         *
         * 저장하지 않은 계획에서는 끝까지 기다렸다 빈 값이 된다. 계획 만들기와 나란히
         * 도는 일이라 화면이 늦어지지는 않는다.
         */
        const val RESTORE_EDITS_TIMEOUT_MS = 3_000L
    }
}
