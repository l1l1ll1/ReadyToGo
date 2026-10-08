package kr.ac.baekseok.readytogo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.ui.auth.AuthViewModel
import kr.ac.baekseok.readytogo.ui.auth.InitialAvatar
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.SecondaryButton
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 계정 관리 — 프로필 확인, 로그아웃, 탈퇴.
 *
 * 탈퇴를 로그아웃과 멀리 떨어뜨리고 색을 다르게 둔다.
 * 나란히 같은 모양으로 두면 로그아웃하려다 탈퇴를 누르는 일이 생긴다.
 *
 * @param onSignedOut 로그아웃·탈퇴가 끝났을 때. 호출부가 로그인 화면으로 되돌린다.
 */
@Composable
fun AccountScreen(
    onSignedOut: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val authState by viewModel.authState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 로그아웃·탈퇴가 끝나면 이 화면에 머물 이유가 없다.
    if (authState is AuthState.SignedOut) {
        LaunchedEffect(Unit) { onSignedOut() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Tokens.Space.screenHorizontal),
    ) {
        Spacer(modifier = Modifier.height(Tokens.Space.lg))

        when (val state = authState) {
            is AuthState.SignedIn -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Tokens.Radius.card)
                        .background(Tokens.Palette.SurfaceMuted)
                        .padding(Tokens.Space.lg),
                ) {
                    InitialAvatar(name = state.user.displayName)
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
                        Text(
                            text = state.user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = state.user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Tokens.Palette.TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Tokens.Space.xl))
                SecondaryButton(text = "로그아웃", onClick = viewModel::signOut)

                // 탈퇴는 화면 맨 아래로 내린다.
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "회원 탈퇴",
                        style = MaterialTheme.typography.labelMedium,
                        color = Tokens.Palette.Danger,
                    )
                }
                Spacer(modifier = Modifier.height(Tokens.Space.xl))
            }

            else -> EmptyState(
                title = "로그인하지 않았습니다",
                description = "계정을 관리하려면 먼저 로그인해 주세요.",
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("정말 탈퇴할까요?") },
            text = {
                Text("저장한 일정과 즐겨찾기가 모두 지워집니다. 되돌릴 수 없습니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount()
                    showDeleteConfirm = false
                }) {
                    Text("탈퇴", color = Tokens.Palette.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            },
        )
    }
}