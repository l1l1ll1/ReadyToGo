package kr.ac.baekseok.readytogo.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/** 하단 탭 네 개. */
enum class MainTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME("tab/home", "홈", Icons.Filled.Home, Icons.Outlined.Home),
    TRIPS("tab/trips", "여행", Icons.Outlined.Place, Icons.Outlined.Place),
    SAVED("tab/saved", "저장", Icons.Filled.Favorite, Icons.Outlined.Favorite),
    PROFILE("tab/profile", "프로필", Icons.Filled.Person, Icons.Outlined.Person),
}

/**
 * 하단 탭 바를 두르는 껍데기.
 *
 * 탭 네 개는 **같은 NavController의 형제 화면**으로 두고, 바 표시 여부만 현재 주소로 판단한다.
 * 중첩 NavHost를 쓰면 탭 안에서 결과 화면으로 들어갈 때 바가 그 위에 남거나,
 * 바를 숨기려고 다시 바깥 NavController로 올려보내는 코드가 필요해진다.
 *
 * @param content 탭 내용. Scaffold가 준 여백을 그대로 넘겨줘야 바에 가려지지 않는다.
 */
@Composable
fun MainScaffold(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // 탭 화면일 때만 바를 보여준다. 결과 화면이나 설정 위에 바가 남으면
    // "저장" 탭을 눌렀을 때 어디로 가야 하는지가 애매해진다.
    val onTab = currentDestination?.hierarchy
        ?.any { route -> MainTab.entries.any { it.route == route.route } } == true

    Scaffold(
        containerColor = Tokens.Palette.Background,
        // 상단 여백을 여기서 주지 않는다. 결과·입력·지도 화면은 자기 TopAppBar가 이미
        // 상태바 여백을 먹기 때문에, 바깥에서 한 번 더 주면 제목 위가 두 배로 벌어진다.
        // 상단 여백이 필요한 탭 화면은 ReadytogoNavHost가 statusBarsPadding으로 직접 준다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!onTab) return@Scaffold
            NavigationBar(containerColor = Tokens.Palette.Background) {
                MainTab.entries.forEach { tab ->
                    // hierarchy로 보는 이유: 탭 주소가 중첩 그래프 안에 있어도 선택 상태가 유지된다.
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == tab.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) navController.switchTab(tab)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        // 하단 바 높이만 넘긴다. 바가 없는 화면에서는 0이 되어 영향이 없다.
        content(Modifier.padding(bottom = padding.calculateBottomPadding()))
    }
}

/**
 * 탭을 바꾼다.
 *
 * `launchSingleTop`과 `popUpTo(startDestination)`이 둘 다 필요하다 —
 * 없으면 탭을 왕복할 때마다 back stack이 쌓여서 뒤로 가기를 열 번 눌러야 앱이 닫힌다.
 * `saveState`/`restoreState`는 탭을 옮겼다 돌아왔을 때 스크롤 위치를 되살린다.
 */
private fun NavHostController.switchTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}