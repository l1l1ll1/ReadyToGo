package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 결과 화면 오른쪽 아래의 "말로 고치기" 원형 버튼(⑫).
 *
 * 예전에는 일정표 맨 위에 입력창이 늘 떠 있어 일정 첫 줄을 한 칸 밀어냈다. 버튼으로 접어 두고
 * 필요할 때 시트로 연다. 응답을 기다리는 동안에는 아이콘 대신 진행 표시가 돈다.
 */
@Composable
fun AiEditButton(busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = palette.Point,
        contentColor = palette.Surface,
        // 그림자 없이(⑫). 바탕과 색 차이로 뜬다.
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = Tokens.Elevation.none,
            pressedElevation = Tokens.Elevation.none,
            focusedElevation = Tokens.Elevation.none,
            hoveredElevation = Tokens.Elevation.none,
        ),
        modifier = modifier.size(Tokens.Size.aiButton),
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = palette.Surface,
                strokeWidth = Tokens.Size.loadingStroke,
                modifier = Modifier.size(Tokens.Size.icon),
            )
        } else {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = stringResource(R.string.plan_text_edit_title))
        }
    }
}

/**
 * 말로 고치기 시트. 안의 입력창은 예전 일정표 위의 것([TextEditBar])을 그대로 쓴다 —
 * 보내기·로그인 안내·글자 수 제한 규칙이 한곳에 있어야 어긋나지 않는다.
 *
 * 보내면 시트를 닫는다. 결과는 결과 화면의 스낵바(되돌리기 포함)로 알리는데, 시트가 덮고 있으면 안 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditSheet(
    dayNumber: Int,
    controls: TextEditControls,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Tokens.Radius.sheet,
        containerColor = palette.Background,
        // 손잡이 대신 둥근 닫기 버튼(design/mockup/edit-sheet). 손잡이는 끌어 내리는 법을 알아야 쓴다.
        dragHandle = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = Tokens.Space.xl, bottom = Tokens.Space.xl),
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(horizontal = Tokens.Space.screenHorizontal)
                    .clip(CircleShape)
                    .background(palette.Surface),
            ) {
                Icon(Icons.Outlined.ExpandMore, contentDescription = stringResource(R.string.common_close))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
                modifier = Modifier.padding(
                    start = Tokens.Space.screenHorizontal,
                    end = Tokens.Space.screenHorizontal,
                    top = Tokens.Space.md,
                ),
            ) {
                Text(stringResource(R.string.plan_text_edit_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.plan_day_tab, dayNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.TextSecondary,
                )
            }
            TextEditBar(
                dayNumber = dayNumber,
                controls = controls.copy(
                    onSubmit = { day, sentence ->
                        controls.onSubmit(day, sentence)
                        onDismiss()
                    },
                ),
            )
        }
    }
}
