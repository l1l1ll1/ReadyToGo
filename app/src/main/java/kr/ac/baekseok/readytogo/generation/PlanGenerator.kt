package kr.ac.baekseok.readytogo.generation

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kr.ac.baekseok.readytogo.data.USE_REAL_API
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.data.repository.flight.SerpApiFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockFlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockItineraryRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockPlaceRepository
import kr.ac.baekseok.readytogo.data.repository.places.GooglePlacesLodgingRepository
import kr.ac.baekseok.readytogo.data.repository.places.GooglePlacesRepository
import kr.ac.baekseok.readytogo.domain.plan.TripPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 일정 생성이 어디까지 왔는지. 화면과 알림 서비스가 같은 값을 본다. */
sealed interface PlanGeneration {
    data object Idle : PlanGeneration
    data class Running(val condition: TripCondition) : PlanGeneration
    data class Done(val condition: TripCondition, val result: Result<TripPlan>) : PlanGeneration
}

/**
 * 일정 생성을 화면 밖에서 돌린다.
 *
 * **왜 ViewModel에서 꺼냈나**: 전에는 결과 화면의 viewModelScope에서 돌았다. 앱을 나가 있는
 * 동안 시스템이 프로세스를 내리면 돌아왔을 때 ViewModel이 새로 생겨 처음부터 다시 만들었다.
 * 여기서는 앱 범위에서 돌리고, 도는 동안 [PlanGenerationService]가 포그라운드로 프로세스를 붙잡는다.
 *
 * 결과는 마지막 한 건만 들고 있다. 알림을 눌러 결과 화면이 다시 열릴 때 꺼내 쓰기 위해서다.
 * 결과 화면을 뒤로 가기로 나가도 생성을 취소하지 않는다 — 몇 초면 끝나는 일이고,
 * 같은 조건으로 다시 들어오면 호출 없이 바로 보여줄 수 있다.
 */
class PlanGenerator(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    /**
     * 실제 API로 갈아끼우는 곳. 화면은 TripPlanner만 알고 있어서 어느 구현체가 붙었는지 모른다.
     *
     * 키 문제나 할당량 초과로 되돌려야 하면 해당 줄을 Mock 구현체로 바꾸면 된다.
     * (항공은 SerpApiFlightRepository가 실패 시 알아서 Mock으로 넘어간다.)
     */
    private val planner: TripPlanner by lazy {
        Log.d(TAG, if (USE_REAL_API) "실제 API 사용" else "Mock 사용 — 외부 호출 없음")
        TripPlanner(
            // USE_REAL_API가 false면 호출이 한 번도 나가지 않는다. 기본값이 false다 — 이유는
            // ApiMode.kt 주석 참고. 켜면 이 세 줄이 전부 실제 구현체로 바뀐다.
            flightRepository = if (USE_REAL_API) {
                SerpApiFlightRepository(context)
            } else {
                MockFlightRepository()
            },
            lodgingRepository = if (USE_REAL_API) {
                GooglePlacesLodgingRepository(context)
            } else {
                MockLodgingRepository()
            },
            placeRepository = if (USE_REAL_API) {
                GooglePlacesRepository(context)
            } else {
                MockPlaceRepository()
            },
            itineraryRepository = MockItineraryRepository(),
            // Mock 식당은 15곳뿐이라 5일 일정(30칸)에서 반드시 소진된다. 테스트 중 화면이 비지 않게
            // 재사용을 허용한다. 실제 API 경로에서는 같은 집을 두 번 추천하지 않는다.
            allowRestaurantReuse = !USE_REAL_API,
        )
    }

    private val _state = MutableStateFlow<PlanGeneration>(PlanGeneration.Idle)
    val state: StateFlow<PlanGeneration> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * [condition]으로 일정을 만든다.
     *
     * 같은 조건이 이미 돌고 있거나 성공해 있으면 아무것도 하지 않는다 — 결과 화면이 다시
     * 열릴 때마다 부르므로, 여기서 막지 않으면 앱으로 돌아올 때마다 호출이 또 나간다.
     * 실패한 결과는 다시 만든다(다시 시도).
     */
    fun start(condition: TripCondition) {
        when (val current = _state.value) {
            is PlanGeneration.Running -> if (current.condition == condition) return
            is PlanGeneration.Done ->
                if (current.condition == condition && current.result.isSuccess) return
            PlanGeneration.Idle -> Unit
        }

        // 다른 조건이 돌고 있었다면 버린다. 결과를 한 건만 들고 있으므로 둘을 함께 돌릴 수 없다.
        job?.cancel()
        _state.value = PlanGeneration.Running(condition)
        PlanGenerationService.start(context)

        job = scope.launch {
            // 계획 하나를 만드는 데 걸리는 시간. 검색을 병렬로 바꾸는 등의 개선 효과를
            // 눈이 아니라 숫자로 확인하기 위해 남긴다.
            val startedAt = SystemClock.elapsedRealtime()
            val result = planner.createPlan(condition)
            Log.d(TAG, "계획 생성 ${SystemClock.elapsedRealtime() - startedAt}ms")

            // createPlan은 runCatching이라 취소돼도 실패로 돌아온다. 그 사이 새 조건이
            // 시작됐다면 그 Running을 덮어쓰면 안 되므로 여기서 멈춘다.
            ensureActive()
            _state.value = PlanGeneration.Done(condition, result)
        }
    }

    private companion object {
        const val TAG = "PlanGenerator"
    }
}
