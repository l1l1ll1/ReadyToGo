package kr.ac.baekseok.readytogo.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.auth.AuthViewModel
import kr.ac.baekseok.readytogo.ui.auth.InitialAvatar
import kr.ac.baekseok.readytogo.ui.component.SecondaryButton
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * "프로필" 탭 — 계정 정보 + 설정 진입.
 *
 * 둘러보기 중인 사용자에게는 프로필 대신 로그인 권유를 보여준다.
 * 빈 프로필 카드를 보여주면 로그인이 안 된 것인지 정보가 없는 것인지 알 수 없다.
 */
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSaved: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val authState by viewModel.authState.collectAsState()

    Surface(color = Tokens.Palette.Background) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = Tokens.Space.screenHorizontal,
                end = Tokens.Space.screenHorizontal,
                top = Tokens.Space.lg,
                bottom = Tokens.Space.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            item {
                when (val state = authState) {
                    is AuthState.SignedIn -> SignedInHeader(
                        name = state.user.displayName,
                        email = state.user.email,
                    )
                    else -> GuestHeader(onSignIn = onSignIn)
                }
            }

            item {
                MenuRow(
                    icon = Icons.Outlined.Favorite,
                    label = stringResource(R.string.profile_saved_places),
                    onClick = onOpenSaved,
                )
            }
            item {
                MenuRow(
                    icon = Icons.Outlined.Settings,
                    label = stringResource(R.string.profile_settings),
                    onClick = onOpenSettings,
                )
            }
            item {
                MenuRow(
                    icon = Icons.Outlined.MailOutline,
                    label = stringResource(R.string.profile_support),
                    onClick = onOpenSupport,
                )
            }
            item {
                MenuRow(
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.profile_about),
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun SignedInHeader(name: String, email: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(Tokens.Palette.SurfaceMuted)
            .padding(Tokens.Space.lg),
    ) {
        InitialAvatar(name = name)
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.Palette.TextSecondary,
            )
        }
    }
}

@Composable
private fun GuestHeader(onSignIn: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(Tokens.Palette.SurfaceMuted)
            .padding(Tokens.Space.lg),
    ) {
        Text(
            text = stringResource(R.string.profile_guest_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.profile_guest_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.Palette.TextSecondary,
        )
        SecondaryButton(text = stringResource(R.string.common_sign_in), onClick = onSignIn)
    }
}

/** 설정·고객센터 같은 진입 줄. 아이콘 + 글자 + 꺾쇠. */
@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .clickable(onClick = onClick)
            .padding(Tokens.Space.lg),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Tokens.Palette.TextSecondary)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = Tokens.Palette.TextSecondary,
        )
    }
}