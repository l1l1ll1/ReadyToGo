package kr.ac.baekseok.readytogo.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 화면 끝에서 띄운 pill 모양 탭 바(⑫, design/mockup).
 *
 * Material NavigationBar는 화면 폭을 꽉 채운 띠라 민트그레이 바탕 위 흰 카드들과 따로 논다.
 * 바깥 여백을 두고 흰 pill로 띄워 카드와 같은 "떠 있는 면"으로 맞춘다.
 *
 * 선택 표시는 탭 칸 뒤에 깔린 pill 하나다. 칸마다 따로 그리지 않고 하나를 옮기는 구조라
 * 나중에 위치만 애니메이션하면 미끄러지는 효과가 된다(⑫-5).
 *
 * 라우트·탭 전환 규칙은 그대로다 — [onSelect]가 기존 switchTab을 부른다.
 */
@Composable
fun FloatingTabBar(
    selectedIndex: Int,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val tabs = MainTab.entries

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // 제스처 바 위로 띄운다. 띄우지 않으면 pill 아래쪽이 시스템 바에 깔린다.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Tokens.Space.lg, vertical = Tokens.Space.md)
            .height(Tokens.Size.tabBarHeight)
            .clip(Tokens.Radius.chip)
            .background(palette.Surface)
            .padding(Tokens.Space.xs),
    ) {
        val slot = maxWidth / tabs.size

        if (selectedIndex >= 0) {
            Box(
                modifier = Modifier
                    .offset(x = slot * selectedIndex)
                    .width(slot)
                    .fillMaxHeight()
                    .clip(Tokens.Radius.chip)
                    .background(palette.PointLight),
            )
        }

        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val label = stringResource(tab.labelRes)
                val tint = if (selected) palette.PointDark else palette.TextSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(Tokens.Radius.chip)
                        .semantics { this.selected = selected }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) { if (!selected) onSelect(tab) },
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        // 글자가 바로 아래 있어 아이콘 설명은 비운다. 둘 다 읽으면 "홈 홈"이 된다.
                        contentDescription = null,
                        tint = tint,
                    )
                    Text(text = label, style = MaterialTheme.typography.labelMedium, color = tint)
                }
            }
        }
    }
}
