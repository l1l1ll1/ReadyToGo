package kr.ac.baekseok.readytogo.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.auth.AuthViewModel
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 스플래시 — 저장된 로그인 세션을 확인하는 동안 보여준다.
 *
 * **고정 시간 대기가 아니다.** `delay(2000)` 같은 식으로 만들면 세션 확인이 빨리 끝나도
 * 2초를 기다려야 하고, 느릴 때는 확인이 안 끝난 채로 넘어간다. 상태가 정해지면 바로 넘긴다.
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

    Surface(color = LocalPalette.current.Point, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
            ) {
                // 로고 이미지가 없어 글자로 둔다. 로고를 만들면 이 자리만 바꾸면 된다.
                Text(
                    text = "Readytogo",
                    color = LocalPalette.current.Background,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "조건만 넣으면 일정까지",
                    color = LocalPalette.current.Background.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(Tokens.Size.icon),
                    color = LocalPalette.current.Background,
                    strokeWidth = Tokens.Size.loadingStroke,
                )
            }
        }
    }
}