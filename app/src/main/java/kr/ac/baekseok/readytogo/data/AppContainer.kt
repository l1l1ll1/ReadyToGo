package kr.ac.baekseok.readytogo.data

import android.content.Context
import androidx.annotation.StringRes
import android.util.Log
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.repository.PlaceDetailsRepository
import kr.ac.baekseok.readytogo.data.repository.places.GooglePlaceDetailsRepository
import kr.ac.baekseok.readytogo.data.photo.PhotoBudget
import kr.ac.baekseok.readytogo.BuildConfig
import kr.ac.baekseok.readytogo.data.repository.NoPlacePhotoRepository
import kr.ac.baekseok.readytogo.data.repository.PlacePhotoRepository
import kr.ac.baekseok.readytogo.data.repository.places.GooglePlacesPhotoRepository
import kr.ac.baekseok.readytogo.data.repository.FavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.SavedTripRepository
import kr.ac.baekseok.readytogo.data.repository.UserProfileRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirestoreUserProfileRepository
import kr.ac.baekseok.readytogo.data.locale.AppLocale
import kr.ac.baekseok.readytogo.data.repository.prefs.PreferencesSettingsRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirebaseAuthRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirestoreFavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.firebase.FirestoreSavedTripRepository
import kr.ac.baekseok.readytogo.generation.PlanGenerationService
import kr.ac.baekseok.readytogo.generation.PlanGenerator
import kr.ac.baekseok.readytogo.notification.TripNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
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
     * [initialize]가 끝났는지.
     *
     * 안드로이드 스튜디오의 **미리보기**는 Application을 거치지 않고 화면만 그린다.
     * 테마가 설정을 읽으려 하면 그때 터지므로, 준비되지 않았으면 기본값으로 그린다.
     */
    val isReady: Boolean
        get() = ::appContext.isInitialized

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
        syncLanguage()
        observeSignInForPendingTrip()
        observeTripsForReminders()
        PlanGenerationService.ensureChannels(appContext)
    }

    /**
     * 일정 생성. 결과 화면이 아니라 여기 하나만 둔다 — 앱을 나갔다 와서 결과 화면이 새로
     * 만들어져도 진행 중인 생성과 그 결과를 이어받아야 하기 때문이다.
     */
    val planGenerator: PlanGenerator by lazy { PlanGenerator(appContext, appScope) }

    /**
     * 앱 화면이 지금 보이는지. MainActivity의 onStart/onStop이 바꾼다.
     *
     * 일정이 완성됐을 때 "일정 완성" 알림을 남길지 정하는 데만 쓴다. 보고 있으면 남기지 않는다.
     * Activity가 하나뿐이라 카운터 없이 참/거짓으로 충분하다.
     */
    var isAppVisible: Boolean = false

    /**
     * 저장된 일정이나 알림 설정이 바뀌면 알람을 다시 깐다.
     *
     * 화면이 아니라 여기서 보는 이유: 일정은 결과 화면에서 저장되고 여행 탭에서 지워지고
     * 로그인하면 통째로 바뀐다. 그 세 곳에 각각 예약 코드를 넣으면 한 곳을 빠뜨린다.
     * 목록과 설정 둘만 보면 어느 경로로 바뀌었든 한 번에 따라간다.
     */
    private fun observeTripsForReminders() {
        TripNotifications.ensureChannel(appContext)

        appScope.launch {
            combine(savedTrips.observeAll(), settings.settings) { trips, appSettings ->
                trips to appSettings
            }.collect { (trips, appSettings) ->
                TripNotifications.reschedule(appContext, trips, appSettings)
            }
        }
    }

    /**
     * 저장된 언어를 시스템에 맞춘다.
     *
     * **Activity가 생기기 전에 해야 한다.** 화면이 뜬 뒤에 바꾸면 시스템이 화면을 다시 만들어
     * 첫 실행마다 스플래시가 한 번 깜빡인다. Application.onCreate가 그 자리다.
     *
     * 기기 언어를 따라가지 않고 저장된 값을 쓰는 이유: 설정 화면이 "앱 언어"를 한 줄로
     * 보여주는데, 기기 언어를 따르는 상태와 직접 고른 상태를 구분하려면 "시스템 기본"이라는
     * 세 번째 상태가 필요하다. 쓰는 사람이 둘(한국어/English) 중에 고르는 편이 단순하다.
     *
     * 이후 설정에서 언어를 바꾸면 여기서 받아 적용한다. 화면(ViewModel)이 직접 하지 않는 이유는
     * 언어를 바꾸는 순간 그 화면이 다시 만들어지기 때문이다 — 사라질 객체에게 맡길 일이 아니다.
     */
    private fun syncLanguage() {
        AppLocale.apply(appContext, settings.settings.value.language)

        appScope.launch {
            settings.settings.collect { AppLocale.apply(appContext, it.language) }
        }
    }

    val auth: FirebaseAuthRepository by lazy { FirebaseAuthRepository(appContext) }

    // 저장·즐겨찾기는 계정에 딸린 것이라 로그인 상태를 알아야 한다.
    // AuthRepository 전체가 아니라 상태 Flow만 넘겨 서로를 참조하지 않게 한다.
    val savedTrips: SavedTripRepository by lazy { FirestoreSavedTripRepository(auth.authState) }
    val favorites: FavoriteRepository by lazy { FirestoreFavoriteRepository(auth.authState) }

    /** 닉네임처럼 계정에 딸린 값. 이름·이메일은 Google이 주지만 닉네임은 우리가 저장한다. */
    val userProfile: UserProfileRepository by lazy {
        FirestoreUserProfileRepository(auth.authState)
    }

    /** 설정은 계정이 아니라 기기에 딸린 값이라 Firestore를 쓰지 않는다. */
    /**
     * 장소 사진. 키가 없으면 사진 없이 돈다 — 그라데이션이 그대로 남는다.
     *
     * 화면이 직접 쓰는 유일한 Repository다. 계획을 만드는 흐름과 무관하게, 카드가 보일 때
     * 그 카드만 물어본다(지연 로드). 사진은 과금이라 미리 받아 두지 않는다.
     */
    /** 이번 달 사진 사용량. 설정 화면이 읽어 보여주고, 사진 Repository가 같은 것을 쓴다. */
    val photoBudget: PhotoBudget by lazy { PhotoBudget(appContext) }

    /**
     * 즐겨찾기를 일정에 끼워 넣을 때 그 장소를 받아 온다.
     *
     * 키가 없으면 세우지 않는다 — 그때는 화면이 즐겨찾기 목록 자체를 보여주지 않는다.
     */
    val placeDetails: PlaceDetailsRepository? by lazy {
        if (BuildConfig.MAPS_API_KEY.isBlank()) null else GooglePlaceDetailsRepository(appContext)
    }

    val placePhotos: PlacePhotoRepository by lazy {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            NoPlacePhotoRepository
        } else {
            GooglePlacesPhotoRepository(appContext, budget = photoBudget)
        }
    }

    val settings: PreferencesSettingsRepository by lazy {
        PreferencesSettingsRepository(appContext)
    }

    /**
     * 화면에 그대로 뜨는 문구를 리소스에서 꺼낸다.
     *
     * Repository는 Context를 들고 있지 않은 것이 많은데, 거기서 던지는 예외 메시지가
     * 스낵바에 그대로 올라간다. 영어로 바꿔도 "로그인하면 장소를 저장할 수 있습니다."가
     * 남았다. 통로를 여기 하나만 두고 appContext는 계속 감춰 둔다.
     */
    fun string(@StringRes resId: Int, vararg args: Any): String =
        appContext.getString(resId, *args)

    /**
     * 온보딩을 본 적 있는지.
     *
     * 기기에 저장한다. 메모리에만 두던 때는 앱을 껐다 켤 때마다 온보딩 3장이 다시 나왔다 —
     * 개발 중에는 확인하기 편했지만 사용자에게는 매번 같은 화면을 넘기게 하는 일이다.
     */
    var onboardingSeen: Boolean
        get() = settings.onboardingSeen
        set(value) {
            settings.onboardingSeen = value
        }

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
