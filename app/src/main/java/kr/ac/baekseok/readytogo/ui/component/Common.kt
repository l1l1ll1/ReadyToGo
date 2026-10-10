package kr.ac.baekseok.readytogo.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 섹션 제목. 오른쪽에 "모두 보기" 같은 동작을 붙일 수 있다.
 * 화면마다 Row + Text를 다시 쓰면 제목 크기와 여백이 조금씩 어긋난다.
 */
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** 화면의 주요 동작 버튼. 포인트 컬러를 쓰는 곳은 화면당 하나만 둔다. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        // 누르는 동안 또 누르면 같은 동작이 두 번 나간다. 로딩 중에는 막는다.
        enabled = enabled && !loading,
        shape = Tokens.Radius.button,
        modifier = modifier
            .fillMaxWidth()
            .height(Tokens.Size.button),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Tokens.Size.icon),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = Tokens.Size.loadingStroke,
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** 테두리만 있는 보조 버튼. "둘러보기"처럼 주요 동작이 아닌 선택지에 쓴다. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = Tokens.Radius.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Tokens.Palette.SurfaceMuted,
            contentColor = Tokens.Palette.TextPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(Tokens.Size.button),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 즐겨찾기 ♥.
 *
 * 사진 위에 올라가는 경우가 많아 반투명 흰 원을 깔아둔다 —
 * 밝은 사진 위에서는 흰 하트가, 어두운 사진 위에서는 검은 하트가 안 보인다.
 */
@Composable
fun FavoriteButton(
    favorite: Boolean,
    onToggle: () -> Unit,
    onPhoto: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (onPhoto) Color.White.copy(alpha = 0.85f) else Color.Transparent,
        shape = Tokens.Radius.chip,
        modifier = modifier,
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                // 접근성 — 스크린 리더가 상태까지 읽어야 누른 건지 알 수 있다.
                contentDescription = stringResource(
                    if (favorite) R.string.common_favorite_remove else R.string.common_favorite_add,
                ),
                tint = if (favorite) Tokens.Palette.Heart else Tokens.Palette.TextSecondary,
            )
        }
    }
}

/**
 * 비어 있는 목록 자리.
 *
 * 빈 화면을 그냥 비워두면 로딩이 안 끝난 것처럼 보인다. 무엇을 하면 채워지는지 알려준다.
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.Palette.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
