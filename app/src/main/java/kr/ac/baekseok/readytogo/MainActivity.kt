package kr.ac.baekseok.readytogo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.ui.navigation.ReadytogoNavHost
import kr.ac.baekseok.readytogo.ui.theme.ReadytogoTheme
import kr.ac.baekseok.readytogo.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    // 전에는 여기서 로케일을 한국어로 고정했다. 이제 설정의 "앱 언어"가 정하고,
    // 시스템의 앱별 언어(LocaleManager)로 적용한다 — AppLocale 참고.
    // 날짜 선택 달력의 요일·월 이름도 같은 로케일을 따라가므로 화면이 한 언어로 맞는다.

    /**
     * Compose가 첫 화면을 그렸는지.
     *
     * 시스템 시작 화면을 언제 걷을지 정하는 데만 쓴다 — 아래 [onCreate] 주석 참고.
     */
    private var contentReady = false

    /**
     * "일정 완성" 알림을 눌러 들어왔을 때 열어야 할 결과 화면의 조건.
     *
     * 바로 이동하지 않고 담아만 두는 이유: 앱이 새로 뜬 경우 아직 스플래시·로그인을 지나는
     * 중이라, 이동은 NavHost가 그 흐름이 끝난 뒤에 한다. 처리하면 null로 비운다.
     */
    private val openPlanRequest = MutableStateFlow<TripCondition?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // **super.onCreate보다 먼저 불러야 한다.** 시스템 시작 화면을 가로채는 일이라
        // 창이 만들어진 뒤에는 늦는다. 여기서 Theme.Readytogo.Starting이 본 테마로 갈린다.
        val splash = installSplashScreen()

        // 첫 화면이 그려질 때까지 시스템 시작 화면을 붙잡아 둔다.
        //
        // 없으면 콜드 스타트에서 우리 스플래시가 한 프레임도 안 나온다 — 시스템 화면이
        // 떠 있는 3초 동안 Compose 쪽은 이미 composition이 끝나 최소 표시 시간을 다 세고
        // 다음 화면으로 넘어가버린다. 실제로 연속 캡처에 한 장도 안 잡혔다.
        splash.setKeepOnScreenCondition { !contentReady }

        super.onCreate(savedInstanceState)

        // 화면 회전 등으로 다시 만들어질 때는 같은 Intent가 또 들어온다. 처음 한 번만 본다.
        if (savedInstanceState == null) handleOpenPlan(intent)

        setContent {
            // **상태바 글자 색은 창 설정이라 Compose가 아니라 Activity가 정한다.**
            // enableEdgeToEdge의 기본값은 기기 다크 모드만 보기 때문에, 앱에서만 다크를
            // 고른 경우 어두운 배경에 어두운 글자가 남아 상태바가 사라진다.
            // 설정이 바뀔 때마다 다시 적용한다.
            val dark = isAppInDarkTheme()
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }

            val pendingPlan by openPlanRequest.collectAsState()

            ReadytogoTheme {
                // 화면 구성과 이동 규칙은 ReadytogoNavHost가 전부 맡는다.
                // 기기 뒤로 가기도 NavHost가 처리하므로 BackHandler가 필요 없다.
                ReadytogoNavHost(
                    openPlanRequest = pendingPlan,
                    onOpenPlanHandled = { openPlanRequest.value = null },
                )
            }

            // 여기까지 왔으면 그릴 것이 준비됐다. 다음 프레임에 시작 화면이 걷힌다.
            contentReady = true
        }
    }

    /** 앱이 떠 있는 상태에서 알림을 눌렀다. singleTop이라 새 화면 대신 여기로 온다. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenPlan(intent)
    }

    override fun onStart() {
        super.onStart()
        AppContainer.isAppVisible = true
    }

    override fun onStop() {
        AppContainer.isAppVisible = false
        super.onStop()
    }

    private fun handleOpenPlan(intent: Intent?) {
        val condition = intent?.getSerializableExtra(EXTRA_OPEN_PLAN, TripCondition::class.java)
            ?: return
        openPlanRequest.value = condition
    }

    companion object {
        /** 알림이 결과 화면을 열 때 조건을 담아 보내는 키. */
        const val EXTRA_OPEN_PLAN = "open_plan"
    }
}
