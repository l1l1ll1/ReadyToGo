package kr.ac.baekseok.readytogo.ui.input

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.model.Destination
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.DestinationRepository
import kr.ac.baekseok.readytogo.data.repository.asset.AssetDestinationRepository
import kr.ac.baekseok.readytogo.domain.budget.BudgetRange
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 입력 화면이 보여줄 목록들. 목적지 도시는 나라를 골라야 채워진다. */
data class InputOptions(
    val origins: List<Destination> = emptyList(),
    val countries: List<String> = emptyList(),
    val cities: List<Destination> = emptyList(),
)

/**
 * 목적지 검색 시트의 상태.
 *
 * @param results 검색 결과. 검색어가 비어 있으면 빈 목록이고, 그때 화면은 나라별 둘러보기를 보여준다.
 */
data class DestinationSearchState(
    val query: String = "",
    val results: List<Destination> = emptyList(),
)

/**
 * 여행 조건 입력 화면의 상태 관리.
 *
 * Application이 필요한 이유는 목적지 목록이 assets에 있어서다. 나중에 서버에서 받아오게 되면
 * 이 의존은 사라진다.
 */
class TripInputViewModel(
    application: Application,
) : AndroidViewModel(application) {

    // 생성자 인자로 받지 않는 이유: 기본 ViewModel 팩토리는 Application 하나만 받는 생성자를 찾는다.
    // 인자를 더 두면(기본값이 있어도) 인스턴스를 만들지 못해 앱이 죽는다.
    // 7단계에서 서버 목록으로 바꿀 때 이 줄만 교체하면 된다.
    private val destinations: DestinationRepository = AssetDestinationRepository(application)

    /**
     * 예산은 처음부터 값을 갖고 시작한다.
     *
     * "미정"으로 두면 슬라이더 손잡이가 맨 왼쪽에 붙어 30만원으로 정한 것처럼 보이고,
     * 사용자는 예산을 꼭 정해야 하는지 알 수 없다. 흔한 금액을 미리 넣어두고 조절하게 한다.
     */
    private val _condition = MutableStateFlow(TripCondition(budgetWon = DEFAULT_BUDGET_WON))
    val condition: StateFlow<TripCondition> = _condition.asStateFlow()

    private val _options = MutableStateFlow(InputOptions())
    val options: StateFlow<InputOptions> = _options.asStateFlow()

    /** 목적지 도시 목록을 채우기 위해 지금 고른 나라를 따로 들고 있는다. */
    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    private val _search = MutableStateFlow(DestinationSearchState())
    val search: StateFlow<DestinationSearchState> = _search.asStateFlow()

    /** 입력할 때마다 새 검색을 띄우지 않도록 이전 검색을 취소한다. */
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val origins = destinations.origins().getOrDefault(emptyList())
            val countries = destinations.countries().getOrDefault(emptyList())
            _options.update { it.copy(origins = origins, countries = countries) }
            // 대부분 인천에서 출발하므로 첫 번째(인천)를 기본으로 골라둔다.
            _condition.update { it.copy(origin = origins.firstOrNull()) }
        }
    }

    fun onOriginSelected(origin: Destination) {
        _condition.update { it.copy(origin = origin) }
    }

    /** 나라를 바꾸면 이전 나라의 도시가 남아 있으면 안 되므로 목적지를 비운다. */
    fun onCountrySelected(country: String) {
        _selectedCountry.value = country
        _condition.update { it.copy(destination = null) }
        viewModelScope.launch {
            val cities = destinations.cities(country).getOrDefault(emptyList())
            _options.update { it.copy(cities = cities) }
        }
    }

    fun onCitySelected(city: Destination) {
        _condition.update { it.copy(destination = city) }
    }

    /**
     * 목적지 검색어가 바뀌었다.
     *
     * 글자마다 검색을 보내도 되는 이유: 목록이 메모리에 있어 파일·네트워크를 타지 않는다.
     * 다만 긴 목록을 훑는 동안 다음 글자가 들어오면 이전 검색은 쓸모없어지므로 취소한다.
     */
    fun onSearchQueryChange(query: String) {
        _search.update { it.copy(query = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val results = destinations.searchCities(query).getOrDefault(emptyList())
            _search.update { it.copy(results = results) }
        }
    }

    /** 시트를 닫을 때. 다음에 열면 빈 검색창으로 시작해야 한다. */
    fun onSearchClosed() {
        searchJob?.cancel()
        _search.value = DestinationSearchState()
    }

    /** 시트에서 "나라 다시 고르기". 도시 목록은 남겨둬도 보이지 않으므로 비우지 않는다. */
    fun onCountryCleared() {
        _selectedCountry.value = null
    }

    /**
     * 기간을 한 번에 받는다.
     *
     * 출발일·귀국일을 따로 받던 것을 합친 이유: 기간 선택 달력이 두 날짜를 같이 돌려주고,
     * 따로 받으면 "출발일이 귀국일보다 뒤"인 중간 상태가 생겨 그걸 매번 되돌려야 했다.
     */
    fun onDateRangeChange(departure: LocalDate?, returnDate: LocalDate?) {
        _condition.update { it.copy(departureDate = departure, returnDate = returnDate) }
    }

    /**
     * 다이얼로그에서 직접 입력한 예산.
     *
     * 숫자 키보드라도 붙여넣기로 다른 문자가 들어올 수 있어 숫자만 남긴다.
     * 자릿수를 제한해 Long 범위를 넘는 입력을 막고, 빈 입력이나 0은 무시한다 —
     * 예산 0원은 어떤 추천도 못 하게 만든다.
     */
    fun onBudgetTyped(input: String) {
        val digits = input.filter { it.isDigit() }.take(MAX_BUDGET_DIGITS)
        val won = digits.toLongOrNull()?.takeIf { it > 0 } ?: return
        _condition.update { it.copy(budgetWon = won) }
    }

    /**
     * 슬라이더·칩에서 고른 예산.
     *
     * 직접 입력과 같은 값을 쓰므로 세 입력이 서로를 덮어쓴다 — 슬라이더를 움직이면 숫자가 따라오고,
     * 숫자를 고치면 슬라이더가 따라온다. 셋이 다른 값을 들고 있으면 어느 게 진짜인지 알 수 없다.
     */
    fun onBudgetSelected(won: Long) {
        _condition.update { it.copy(budgetWon = won) }
    }

    /**
     * 인원을 한 명씩 늘리거나 줄인다.
     *
     * 범위를 모델의 상수로 막는다 — 화면에서 버튼을 비활성화해도 값 자체가 범위를 벗어나면
     * 방 수와 1인 상한 계산이 이상해진다.
     */
    fun onAdultsChange(delta: Int) {
        _condition.update {
            val next = (it.adults + delta)
                .coerceIn(TripCondition.MIN_ADULTS, TripCondition.MAX_ADULTS)

            // 예산 범위가 인원에 비례하므로 인원이 바뀌면 범위도 바뀐다.
            // 정해 둔 금액은 그대로 두고, 범위를 벗어날 때만 가장 가까운 끝으로 옮긴다.
            val budget = it.budgetWon?.let { won -> BudgetRange.coerce(won, next) }
            it.copy(adults = next, budgetWon = budget)
        }
    }

    /** 다중 선택이라 이미 골라둔 목적을 다시 누르면 해제된다. */
    fun onPurposeToggle(purpose: TravelPurpose) {
        _condition.update {
            val next = if (purpose in it.purposes) it.purposes - purpose else it.purposes + purpose
            it.copy(purposes = next)
        }
    }

    /**
     * "계획 만들기" 버튼 활성화 조건.
     * 출발지·목적지·날짜가 없으면 검색 자체가 불가능하다. 예산·목적은 없어도 추천은 가능하다.
     */
    val canCreatePlan: StateFlow<Boolean> = _condition
        .map {
            it.origin != null && it.destination != null &&
                it.departureDate != null && it.returnDate != null
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 아직 안 채운 것을 알려줄 문구. 다 채웠으면 null이다.
     *
     * 버튼만 흐리게 해두면 무엇이 모자란지 알 수 없어 사용자가 화면을 위아래로 훑게 된다.
     * 빠진 항목만 짚어 준다.
     */
    val missingHint: StateFlow<String?> = _condition
        .map { condition ->
            val missing = buildList {
                if (condition.origin == null) add("출발지")
                if (condition.destination == null) add("목적지")
                if (condition.departureDate == null || condition.returnDate == null) add("여행 날짜")
            }
            if (missing.isEmpty()) null else "${missing.joinToString(", ")}를 골라주세요"
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        private const val MAX_BUDGET_DIGITS = 12

        /**
         * 처음 보여줄 예산. 슬라이더 가운데쯤에 오도록 흔한 금액으로 골랐다.
         *
         * 화면도 이 값을 쓴다 — 모델의 `budgetWon`은 null이 될 수 있는 타입이라
         * 화면이 다른 기본값을 적으면 두 곳이 어긋난다.
         */
        const val DEFAULT_BUDGET_WON = 1_000_000L
    }
}
