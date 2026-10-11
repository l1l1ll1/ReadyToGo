package kr.ac.baekseok.readytogo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

            ReadytogoTheme {
                // 화면 구성과 이동 규칙은 ReadytogoNavHost가 전부 맡는다.
                // 기기 뒤로 가기도 NavHost가 처리하므로 BackHandler가 필요 없다.
                ReadytogoNavHost()
            }

            // 여기까지 왔으면 그릴 것이 준비됐다. 다음 프레임에 시작 화면이 걷힌다.
            contentReady = true
        }
    }
}
