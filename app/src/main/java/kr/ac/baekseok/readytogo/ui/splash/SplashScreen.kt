package kr.ac.baekseok.readytogo.ui.splash

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
 * **고정 시간 대기가 아니다.** `delay(2000)` 같은 식으로 만들면 세션 확인이 빨리 끝나도
 * 2초를 기다려야 하고, 느릴 때는 확인이 안 끝난 채로 넘어간다. 상태가 정해지면 바로 넘긴다.
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

    LaunchedEffect(Unit) { viewModel.restoreSession() }

    // Unknown이 아니게 되는 순간 넘어간다.
    LaunchedEffect(authState) {
        if (authState !is AuthState.Unknown) onReady(authState)
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
