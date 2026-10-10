package kr.ac.baekseok.readytogo.ui.splash

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.auth.AuthViewModel
import kr.ac.baekseok.readytogo.ui.component.Wordmark
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 스플래시 — 저장된 로그인 세션을 확인하는 동안 보여준다.
 *
 * **[MIN_VISIBLE_MS]만큼은 보여준다.** 세션 확인은 보통 눈 깜짝할 새에 끝나서, 그대로 두면
 * 이 화면이 한 프레임도 그려지지 않고 지나간다. 모자란 만큼만 채우고, 확인이 오래 걸리면
 * 더 기다리지 않는다 — `delay(2000)` 같은 고정 대기가 아니다.
 *
 * **다만 콜드 스타트에서는 이 최소 시간이 소용없다.** 안드로이드 12부터 앱이 처음 뜨는
 * 동안은 시스템이 자기 시작 화면(배경색 + 아이콘)을 띄워 두는데, 그 시간이 에뮬레이터
 * 기준 3초가 넘는다. 그동안 이 화면은 이미 composition이 끝나 0.6초를 다 세어버려서,
 * 시스템 화면이 걷힐 때는 벌써 다음 화면으로 넘어간 뒤다. 그래서 **차갑게 시작하면
 * 사용자가 보는 시작 화면은 시스템 쪽**이고, 이 화면은 세션 확인이 느릴 때나 따뜻한
 * 재시작에서 보인다.
 *
 * 콜드 스타트에서도 이 화면을 보여주려면 시스템 시작 화면을 우리가 붙잡고 있어야 하는데
 * (androidx core-splashscreen의 `setKeepOnScreenCondition`), 그건 라이브러리가 하나 는다.
 * 시스템 시작 화면도 같은 주황에 같은 아이콘이라 지금은 그대로 둔다.
 *
 * 배경이 시스템 windowBackground(splash_background.xml)와 같은 노을이라, 앱이 뜨는
 * 순간과 이 화면 사이에 경계가 보이지 않는다.
 *
 * **로딩 표시를 두지 않는다.** 세션 확인은 보통 눈에 안 띄게 끝나는데, 돌아가는 원을 띄우면
 * 켤 때마다 "뭔가 기다리는 앱"처럼 보인다.
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

        // 모자란 만큼만 채우고 넘어간다. 확인이 오래 걸리면 더 기다리지 않는다 —
        // 고정 대기가 아니라 **최소** 표시 시간이다.
        val shownFor = SystemClock.elapsedRealtime() - shownAt
        if (shownFor < MIN_VISIBLE_MS) delay(MIN_VISIBLE_MS - shownFor)

        onReady(authState)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(LocalPalette.current.Sunset)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
        ) {
            // 앱 아이콘의 심볼을 그대로 쓴다. 런처 아이콘과 같은 그림이어야
            // "방금 누른 그 앱"이라는 게 바로 읽힌다.
            Image(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(Tokens.Size.splashSymbol),
            )

            Wordmark(width = Tokens.Size.splashWordmark)

            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Tokens.Space.sm),
            )
        }
    }
}

/**
 * 스플래시를 최소한 이만큼은 보여준다.
 *
 * 0.6초인 이유: 더 짧으면 깜빡이는 것처럼 보이고, 1초를 넘기면 앱이 느리게 느껴진다.
 * 로고를 한 번 읽을 수 있는 가장 짧은 시간이다.
 */
private const val MIN_VISIBLE_MS = 600L
