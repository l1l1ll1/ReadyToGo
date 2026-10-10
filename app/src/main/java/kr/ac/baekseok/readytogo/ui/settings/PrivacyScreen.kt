package kr.ac.baekseok.readytogo.ui.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette

/**
 * 개인정보 처리 방침.
 *
 * **Compose 화면이 아니라 WebView인 이유**: 같은 문서를 스토어 등록용 공개 주소로도 써야 한다.
 * 문서를 Kotlin 문자열로 적으면 웹용으로 한 벌을 더 만들어야 하고, 두 벌은 반드시 어긋난다.
 * HTML 한 장을 앱과 웹이 같이 쓴다([PRIVACY_URL] 주석 참고).
 *
 * 새 라이브러리는 쓰지 않는다 — WebView는 프레임워크에 들어 있다.
 */
@Composable
fun PrivacyScreen() {
    val background = LocalPalette.current.Background.value.toInt()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                // 링크(문의 메일)를 눌렀을 때 외부 브라우저로 튀지 않게 한다.
                // mailto:는 WebView가 처리하지 못하므로 기본 동작에 맡긴다.
                webViewClient = WebViewClient()

                // 자바스크립트를 켜지 않는다. 이 문서에는 없고, 켜면 공격 면만 늘어난다.
                settings.javaScriptEnabled = false

                setBackgroundColor(background)
                loadUrl(PRIVACY_URL.ifEmpty { LOCAL_PRIVACY_URL })
            }
        },
    )
}

/**
 * 공개된 방침 주소.
 *
 * 비워 두면 앱에 들어 있는 사본을 띄운다. Firebase Hosting에 올린 뒤 그 주소를 여기 적으면
 * 앱을 새로 올리지 않고도 문구를 고칠 수 있다 — 스토어 등록에도 이 주소가 필요하다.
 */
private const val PRIVACY_URL = ""

/** 저장소의 hosting/privacy.html이 에셋으로 들어온다(app/build.gradle.kts 참고). */
private const val LOCAL_PRIVACY_URL = "file:///android_asset/privacy.html"
