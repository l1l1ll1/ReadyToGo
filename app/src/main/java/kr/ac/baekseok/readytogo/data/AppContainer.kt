package kr.ac.baekseok.readytogo.data

import kr.ac.baekseok.readytogo.data.repository.FavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.SavedTripRepository
import kr.ac.baekseok.readytogo.data.repository.SettingsRepository
import kr.ac.baekseok.readytogo.data.repository.fake.FakeAuthRepository
import kr.ac.baekseok.readytogo.data.repository.fake.FakeFavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.fake.FakeSavedTripRepository
import kr.ac.baekseok.readytogo.data.repository.fake.FakeSettingsRepository

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
 * `Application.onCreate`보다 먼저 쓰이는 일이 없어 Context가 필요 없는 것만 여기 둔다.
 * Context가 필요한 Repository(Places, SerpApi)는 지금처럼 ViewModel이 직접 만든다.
 */
object AppContainer {

    /** 8단계 2번에서 이 네 줄을 Firebase·SharedPreferences 구현체로 바꾸면 끝난다. */
    val auth: FakeAuthRepository by lazy { FakeAuthRepository() }

    // 저장·즐겨찾기는 계정에 딸린 것이라 로그인 상태를 알아야 한다.
    // AuthRepository 전체가 아니라 상태 Flow만 넘겨 서로를 참조하지 않게 한다.
    val savedTrips: SavedTripRepository by lazy { FakeSavedTripRepository(auth.authState) }
    val favorites: FavoriteRepository by lazy { FakeFavoriteRepository(auth.authState) }

    val settings: SettingsRepository by lazy { FakeSettingsRepository() }

    /**
     * 온보딩을 본 적 있는지.
     *
     * 1단계에서는 메모리에만 둔다 — 앱을 껐다 켜면 온보딩이 다시 나오므로 확인하기 쉽다.
     * 8단계 3번에서 [settings]와 같이 SharedPreferences로 옮긴다.
     */
    var onboardingSeen: Boolean = false
}
