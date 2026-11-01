package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import kr.ac.baekseok.readytogo.ui.theme.pressClickable

/** 흰 원 안의 아이콘 버튼(시안의 뒤로·공유·저장, 시트 닫기). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Tokens.Size.roundButton)
            .clip(CircleShape)
            .background(LocalPalette.current.Surface)
            .pressClickable(role = Role.Button, onClickLabel = description, onClick = onClick),
    ) {
        Icon(icon, contentDescription = description, tint = LocalPalette.current.TextPrimary)
    }
}

/**
 * 뒤로 + 가운데 제목만 있는 화면 머리(입력 폼·설정과 그 하위 화면).
 *
 * 결과 화면 머리(PlanTopBar)와 같은 둥근 뒤로 버튼을 쓴다 — M3 TopAppBar의 맨 화살표를 두면
 * 화면을 넘나들 때 뒤로 버튼의 모양과 자리가 바뀐다.
 */
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Tokens.Space.screenHorizontal, vertical = Tokens.Space.md),
    ) {
        RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back), onBack)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
            FitText(text = title, style = MaterialTheme.typography.titleMedium)
        }
        // 제목이 가운데에 오도록 오른쪽에 뒤로 버튼만큼 비워 둔다.
        Box(Modifier.size(Tokens.Size.roundButton))
    }
}
