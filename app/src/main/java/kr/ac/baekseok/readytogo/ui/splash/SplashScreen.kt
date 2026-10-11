package kr.ac.baekseok.readytogo.ui.splash

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.auth.AuthViewModel
import kr.ac.baekseok.readytogo.ui.theme.LightPalette

/**
 * 스플래시 — 저장된 로그인 세션을 확인하는 동안 보여준다.
 *
 * **[MIN_VISIBLE_MS]만큼은 보여준다.** 세션 확인은 보통 눈 깜짝할 새에 끝나서, 그대로 두면
 * 이 화면이 한 프레임도 그려지지 않고 지나간다. 모자란 만큼만 채우고, 확인이 오래 걸리면
 * 더 기다리지 않는다 — `delay(2000)` 같은 고정 대기가 아니다.
 *
 * **이 최소 시간이 제구실을 하려면 시스템 시작 화면을 붙잡아야 한다.** 안드로이드 12부터
 * 앱이 처음 뜨는 동안은 시스템이 자기 화면을 띄우는데, 그동안 이 화면은 이미 composition이
 * 끝나 시간을 다 세어버린다. MainActivity가 `setKeepOnScreenCondition`으로 첫 프레임까지
 * 시스템 화면을 붙잡아 두어 그 어긋남을 없앴다.
 *
 * **다크 모드에서도 흰 배경 그대로다.** 이 화면의 그림(splash_art)은 흰 바탕을 전제로
 * 그려져서, 어두운 배경에 얹으면 짙은 네이비 워드마크가 묻히고 흰 구름·종이비행기가
 * 얼룩처럼 드러난다. 시스템 시작 화면도 같은 이유로 흰색을 유지한다 — 둘이 같은 색이라야
 * 시작 화면이 걷히는 순간 경계가 보이지 않는다.
 *
 * @param onReady 세션 확인이 끝났을 때. 온보딩을 봤는지에 따라 다음 화면이 갈린다.
 */
@Composable
fun SplashScreen(
    onReady: (AuthState) -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val authState by viewModel.authState.collectAsState()
    val shownAt = remember { SystemClock.elapsedRealtime() }

    // **떠 있는 동안은 상태바 글자를 어둡게 둔다.** 배경이 흰색으로 고정이라, 다크 모드에서
    // MainActivity가 켜 둔 흰 글자를 그대로 두면 흰 바탕에 묻혀 시계와 배터리가 사라진다.
    // 화면을 떠날 때 원래대로 돌려놓는다.
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = view.context.findActivity()
            ?.let { WindowCompat.getInsetsController(it.window, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = true

        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.restoreSession() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Unknown) return@LaunchedEffect

        val shownFor = SystemClock.elapsedRealtime() - shownAt
        if (shownFor < MIN_VISIBLE_MS) delay(MIN_VISIBLE_MS - shownFor)

        onReady(authState)
    }

    // 지금 테마가 아니라 **라이트 한 벌의 배경**을 그대로 쓴다. 위 주석 참고.
    Surface(color = LightPalette.Background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.splash_art),
                contentDescription = null,
                // 세로를 정하지 않고 그림 비율에 맡긴다. 세로가 긴 그림이라 폭만 잡으면 된다.
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(ART_WIDTH_RATIO),
            )
        }
    }
}

/**
 * 이 Context를 들고 있는 Activity. 찾지 못하면 null(미리보기 등).
 *
 * Compose의 Context는 ContextWrapper로 몇 겹 싸여 있어 그냥 형변환하면 실패한다.
 */
private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * 스플래시를 최소한 이만큼은 보여준다.
 *
 * 0.8초인 이유: 더 짧으면 깜빡이는 것처럼 보이고, 1초를 넘기면 앱이 느리게 느껴진다.
 * 로고와 그림을 한 번 훑을 수 있는 가장 짧은 시간이다.
 */
private const val MIN_VISIBLE_MS = 800L

/** 그림이 차지할 화면 폭. 꽉 채우면 여백이 없어 답답하고, 작으면 스티커처럼 보인다. */
private const val ART_WIDTH_RATIO = 0.7f
