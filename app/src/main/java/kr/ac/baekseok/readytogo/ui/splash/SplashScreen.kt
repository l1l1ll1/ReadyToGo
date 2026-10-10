package kr.ac.baekseok.readytogo.ui.splash

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.auth.AuthViewModel
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette

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
 * 배경이 시스템 시작 화면과 같은 흰색이라 둘 사이에 경계가 보이지 않는다.
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

    LaunchedEffect(Unit) { viewModel.restoreSession() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Unknown) return@LaunchedEffect

        val shownFor = SystemClock.elapsedRealtime() - shownAt
        if (shownFor < MIN_VISIBLE_MS) delay(MIN_VISIBLE_MS - shownFor)

        onReady(authState)
    }

    Surface(color = LocalPalette.current.Background, modifier = Modifier.fillMaxSize()) {
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
 * 스플래시를 최소한 이만큼은 보여준다.
 *
 * 0.8초인 이유: 더 짧으면 깜빡이는 것처럼 보이고, 1초를 넘기면 앱이 느리게 느껴진다.
 * 로고와 그림을 한 번 훑을 수 있는 가장 짧은 시간이다.
 */
private const val MIN_VISIBLE_MS = 800L

/** 그림이 차지할 화면 폭. 꽉 채우면 여백이 없어 답답하고, 작으면 스티커처럼 보인다. */
private const val ART_WIDTH_RATIO = 0.7f
