package kr.ac.baekseok.readytogo.data

import android.content.Context
import android.util.Log
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.repository.FavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.SavedTripRepository
import kr.ac.baekseok.readytogo.data.repository.SettingsRepository
import kr.ac.baekseok.readytogo.data.repository.fake.FakeSettingsRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirebaseAuthRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirestoreFavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirestoreSavedTripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 전체가 공유하는 Repository 보관소.
 *
 * **왜 필요한가**: 로그인 상태·저장된 일정·즐겨찾기는 화면마다 따로 들고 있으면 안 된다.
 * 결과 화면에서 ♥를 눌렀는데 "저장" 탭에 안 나타나면 앱이 고장난 것으로 보인다.
 * ViewModel이 각자 Repository를 새로 만들면 상태가 화면마다 갈라지므로 여기서 하나만 만든다.
 *
 * **왜 Hilt·Koin이 아닌가**: 공유할 객체가 네 개다. 의존성 주입 라이브러리를 넣으면
 * 빌드 설정과 애노테이션이 늘어나는데 얻는 게 없다(CLAUDE.md — 과도하게 설계하지 않는다).
 * 개수가 늘어 손으로 엮기 어려워지면 그때 바꾼다.
 *
 * Firebase를 쓰려면 Application Context가 필요해서 [initialize]로 한 번 받는다.
 * `FirebaseApp`은 google-services 플러그인이 자동으로 초기화하므로 따로 부르지 않는다.
 */
object AppContainer {

    private lateinit var appContext: Context

    /**
     * 화면보다 오래 살아야 하는 일을 돌리는 범위.
     *
     * **왜 필요한가**: 보류 중인 저장은 로그인 화면이 사라진 **뒤에** 끝난다.
     * ViewModel의 viewModelScope에서 돌리면 화면이 back stack에서 빠질 때 코루틴이
     * 취소되어 Firestore 쓰기가 중간에 끊긴다. 앱이 살아 있는 동안 유지되는 범위가 필요하다.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Application.onCreate에서 한 번 부른다. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        observeSignInForPendingTrip()
    }

    val auth: FirebaseAuthRepository by lazy { FirebaseAuthRepository(appContext) }

    // 저장·즐겨찾기는 계정에 딸린 것이라 로그인 상태를 알아야 한다.
    // AuthRepository 전체가 아니라 상태 Flow만 넘겨 서로를 참조하지 않게 한다.
    val savedTrips: SavedTripRepository by lazy { FirestoreSavedTripRepository(auth.authState) }
    val favorites: FavoriteRepository by lazy { FirestoreFavoriteRepository(auth.authState) }

    /** 설정은 기기에 남는 값이라 Firestore를 쓰지 않는다. 8단계 3번에서 실제 저장으로 바꾼다. */
    val settings: SettingsRepository by lazy { FakeSettingsRepository() }

    /**
     * 온보딩을 본 적 있는지.
     *
     * 1단계에서는 메모리에만 둔다 — 앱을 껐다 켜면 온보딩이 다시 나오므로 확인하기 쉽다.
     * 8단계 3번에서 [settings]와 같이 SharedPreferences로 옮긴다.
     */
    var onboardingSeen: Boolean = false

    /**
     * 둘러보기 중에 저장하려 했던 일정.
     *
     * 둘러보기는 저장이 막혀 있어서, 저장을 누르면 그 일정을 여기 기억해 두고 로그인을 권한다.
     * 로그인이 끝나면 [consumePendingTrip]으로 꺼내 저장한다 — 사용자가 같은 조건을
     * 처음부터 다시 입력하지 않아도 된다.
     *
     * 메모리에만 둔다. 로그인 화면을 거치는 동안 앱이 죽을 일은 드물고,
     * 그 경우 일정을 다시 만드는 것이 안전한 쪽이다(저장 의도를 확신할 수 없으므로).
     */
    private var pendingTrip: SavedTrip? = null

    fun rememberPendingTrip(trip: SavedTrip) {
        pendingTrip = trip
    }

    /** 꺼내면서 비운다. 두 번 저장되는 것을 막는다. */
    private fun consumePendingTrip(): SavedTrip? = pendingTrip.also { pendingTrip = null }

    /**
     * 로그인이 끝나면 보류 중인 일정을 자동으로 저장한다.
     *
     * 화면이 아니라 여기서 감시하는 이유: 저장을 누른 화면(결과)과 로그인이 끝나는 화면(로그인)이
     * 달라서, 어느 한쪽에 맡기면 "로그인 화면을 지나쳐 홈으로 갔다" 같은 경로에서 빠진다.
     * 로그인 상태 하나만 보면 경로와 무관하게 한 번만 저장된다.
     */
    private fun observeSignInForPendingTrip() {
        appScope.launch {
            auth.authState.collect { state ->
                if (state !is AuthState.SignedIn) return@collect
                val trip = consumePendingTrip() ?: return@collect
                savedTrips.save(trip)
                    .onSuccess { Log.d(TAG, "둘러보기 일정을 이어받아 저장: ${trip.title}") }
                    // 실패하면 보류 목록으로 되돌린다. 다음 로그인에서 다시 시도한다.
                    .onFailure {
                        Log.w(TAG, "이어받기 저장 실패", it)
                        pendingTrip = trip
                    }
            }
        }
    }

    private const val TAG = "AppContainer"
}
