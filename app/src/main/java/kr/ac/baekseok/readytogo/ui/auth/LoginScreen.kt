package kr.ac.baekseok.readytogo.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.component.SecondaryButton
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 로그인 화면.
 *
 * **"둘러보기"를 로그인과 나란히 둔다.** 로그인을 강제하면 앱이 무엇을 해주는지 보기도 전에
 * 계정을 만들어야 해서 대부분 닫는다. 저장만 막으면 충분하다(CLAUDE.md 앱 구조 → 로그인).
 *
 * Google 로그인은 Firebase Auth + Credential Manager로 실제 동작한다(8단계 2번).
 *
 * @param onSignedIn 로그인 또는 둘러보기로 들어갔을 때.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val authState by viewModel.authState.collectAsState()
    val signingIn by viewModel.signingIn.collectAsState()
    val error by viewModel.error.collectAsState()

    // 계정 선택창은 이 화면의 Context로 띄운다 — AuthViewModel.signInWithGoogle 주석 참고.
    val context = LocalContext.current

    // 로그인이 끝나면 넘긴다.
    //
    // **둘러보기(Guest)는 여기서 보지 않는다.** 전에는 Guest도 "들어갈 수 있는 상태"로 보고
    // 넘겼는데, 그러면 둘러보기 중인 사용자가 로그인하러 이 화면에 와도 화면이 뜨자마자
    // 다시 홈으로 튕겨서 로그인할 방법이 없었다(결과 화면의 "로그인" 스낵바, 프로필의 로그인 버튼).
    // 둘러보기로 들어가는 것은 아래 버튼이 직접 처리한다.
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) onSignedIn()
    }

    Surface(color = LocalPalette.current.Background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Tokens.Space.screenHorizontal),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Readytogo",
                    style = MaterialTheme.typography.displaySmall,
                    color = LocalPalette.current.Point,
                )
                Spacer(modifier = Modifier.height(Tokens.Space.sm))
                Text(
                    text = stringResource(R.string.login_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalPalette.current.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalPalette.current.Danger,
                    modifier = Modifier.padding(bottom = Tokens.Space.md),
                )
            }

            // Google 로고 이미지는 쓰지 않는다 — 브랜드 가이드라인이 따라붙고,
            // 비슷하게 그려 넣는 것은 더 나쁘다. 글자로만 표기한다.
            PrimaryButton(
                text = stringResource(R.string.login_google),
                onClick = { viewModel.signInWithGoogle(context) },
                loading = signingIn,
            )

            Spacer(modifier = Modifier.height(Tokens.Space.md))

            SecondaryButton(
                text = stringResource(R.string.login_guest),
                onClick = {
                    viewModel.continueAsGuest()
                    onSignedIn()
                },
            )

            Spacer(modifier = Modifier.height(Tokens.Space.md))

            Text(
                text = stringResource(R.string.login_guest_note),
                style = MaterialTheme.typography.labelMedium,
                color = LocalPalette.current.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Tokens.Space.xxl))
        }
    }
}

/** 프로필 사진이 없을 때 쓰는 이니셜 원. 설정·프로필 화면이 같이 쓴다. */
@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Tokens.Size.avatar)
            .clip(CircleShape)
            .background(LocalPalette.current.PointLight),
    ) {
        Text(
            // 한글은 첫 글자, 영문도 첫 글자. 두 글자를 쓰면 한글에서 "여행"처럼 단어가 되어 어색하다.
            text = name.take(1).ifBlank { "?" },
            color = LocalPalette.current.PointDark,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}