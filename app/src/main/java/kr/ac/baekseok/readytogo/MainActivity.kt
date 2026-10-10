package kr.ac.baekseok.readytogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.ac.baekseok.readytogo.ui.navigation.ReadytogoNavHost
import kr.ac.baekseok.readytogo.ui.theme.ReadytogoTheme

class MainActivity : ComponentActivity() {

    // 전에는 여기서 로케일을 한국어로 고정했다. 이제 설정의 "앱 언어"가 정하고,
    // 시스템의 앱별 언어(LocaleManager)로 적용한다 — AppLocale 참고.
    // 날짜 선택 달력의 요일·월 이름도 같은 로케일을 따라가므로 화면이 한 언어로 맞는다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadytogoTheme {
                // 화면 구성과 이동 규칙은 ReadytogoNavHost가 전부 맡는다.



                // 기기 뒤로 가기는 NavHost가 알아서 처리하므로 BackHandler가 필요 없다.
                ReadytogoNavHost()
            }
        }
    }
}
