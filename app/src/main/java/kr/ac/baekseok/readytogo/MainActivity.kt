package kr.ac.baekseok.readytogo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.ac.baekseok.readytogo.ui.navigation.ReadytogoNavHost
import kr.ac.baekseok.readytogo.ui.theme.ReadytogoTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * 앱 로케일을 한국어로 고정한다.
     *
     * **왜 필요한가**: Material 3의 날짜 선택 달력은 요일·월 이름을 시스템 로케일로 그린다.
     * 기기가 영어면 한국어 화면 안에 "October 2026 / S M T W T F S"가 섞여 나온다.
     * 우리 문자열은 전부 한국어라서(CLAUDE.md) 달력만 영어로 남으면 어긋난다.
     *
     * 8단계 3번에서 설정의 언어 항목을 실제로 동작시킬 때, 고정값 대신 저장된 설정을 읽으면 된다.
     * 그때 바꿀 곳이 이 함수 하나다.
     */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.KOREAN
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

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
