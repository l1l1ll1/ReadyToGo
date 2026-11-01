package kr.ac.baekseok.readytogo.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.domain.review.reviewPlan
import kr.ac.baekseok.readytogo.generation.PlanGeneration
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.domain.review.ReviewIssue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * 홈 화면 상태.
 *
 * @param upcoming 가장 가까운 다가오는 여행. 히어로 카드에 쓴다. 없으면 카드를 숨긴다.
 * @param recent 최근 저장한 일정. 지난 여행도 포함한다 — 다시 쓰려고 열어보는 경우가 많다.
 * @param review 다가오는 여행의 검토 요약. 0건이거나 계획이 아직 없으면 null — 카드를 숨긴다.
 */
data class HomeUiState(
    val authState: AuthState = AuthState.Unknown,
    /** 사용자가 정한 이름. 아직 없으면 null — 인사말이 Google 계정 이름으로 내려간다. */
    val nickname: String? = null,
    val upcoming: SavedTrip? = null,
    val recent: List<SavedTrip> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val review: HomeReview? = null,
)

/** 홈 검토 카드 한 장. 문제가 여럿이면 첫 문제의 종류만 짧게 보여주고 건수로 나머지를 알린다. */
data class HomeReview(val count: Int, val title: String)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<HomeUiState> = combine(
        AppContainer.auth.authState,
        AppContainer.savedTrips.observeAll(),
        AppContainer.userProfile.observeNickname(),
        AppContainer.planGenerator.state,
    ) { auth, trips, nickname, generation ->
        val today = LocalDate.now()
        // 다가오는 여행이 여럿이면 가장 가까운 것 하나만 보여준다.
        // 카드를 여러 개 쌓으면 "다음에 갈 곳"이라는 의미가 흐려진다.
        val upcoming = trips.filter { it.isUpcoming(today) }.minByOrNull { it.departureDate }
        HomeUiState(
            authState = auth,
            nickname = nickname,
            upcoming = upcoming,
            recent = trips.sortedByDescending { it.savedAt }.take(RECENT_COUNT),
            today = today,
            review = upcoming?.let { reviewOf(it, generation) },
        )
    }.stateIn(
        scope = viewModelScope,
        // 화면을 잠깐 벗어났다 돌아올 때 목록을 다시 만들지 않도록 잠시 유지한다.
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    /**
     * 다가오는 여행의 검토 요약. **이미 만든 계획이 있을 때만** 계산한다 — 홈에서 계획을 새로 만들면
     * 캐시가 지난 경우 Places·SerpApi 호출이 나간다. 결과 화면을 한 번 열면 생성기가 들고 있다.
     *
     * 날씨는 넣지 않는다(홈에서 따로 받지 않는다). 그래서 비 예보 문제는 결과 화면에서만 보인다.
     */
    private fun reviewOf(trip: SavedTrip, generation: PlanGeneration): HomeReview? {
        val done = generation as? PlanGeneration.Done ?: return null
        if (done.condition != trip.condition) return null
        val plan = done.result.getOrNull() ?: return null
        val issues = reviewPlan(plan, trip.edits)
        if (issues.isEmpty()) return null
        return HomeReview(issues.size, getApplication<Application>().getString(shortTitleOf(issues.first())))
    }

    /**
     * 규칙별 짧은 제목. 홈은 "무슨 종류의 문제가 있다"만 알리고, 장소 이름이 든 긴 사실 문장은
     * 결과 화면 요약 탭에 둔다 — 홈 카드에 두 줄짜리 문장이 잘려 보이면 무엇이 문제인지 오히려 안 읽혔다.
     */
    private fun shortTitleOf(issue: ReviewIssue): Int = when (issue) {
        is ReviewIssue.LongDay -> R.string.review_short_long
        is ReviewIssue.SameKind -> R.string.review_short_same_kind
        is ReviewIssue.Closed -> R.string.review_short_closed
        is ReviewIssue.Rainy -> R.string.review_short_rainy
    }

    private companion object {
        const val RECENT_COUNT = 5
        const val FLOW_TIMEOUT_MS = 5_000L
    }
}
