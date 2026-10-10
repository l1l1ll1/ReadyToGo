package kr.ac.baekseok.readytogo.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.ui.auth.LoginScreen
import kr.ac.baekseok.readytogo.ui.home.HomeScreen
import kr.ac.baekseok.readytogo.ui.input.TripInputScreen
import kr.ac.baekseok.readytogo.ui.main.MainScaffold
import kr.ac.baekseok.readytogo.ui.main.MainTab
import kr.ac.baekseok.readytogo.ui.map.DayMapScreen
import kr.ac.baekseok.readytogo.ui.onboarding.OnboardingScreen
import kr.ac.baekseok.readytogo.ui.plan.TripPlanScreen
import kr.ac.baekseok.readytogo.ui.plan.TripPlanUiState
import kr.ac.baekseok.readytogo.ui.plan.TripPlanViewModel
import kr.ac.baekseok.readytogo.ui.profile.ProfileScreen
import kr.ac.baekseok.readytogo.ui.saved.SavedScreen
import kr.ac.baekseok.readytogo.ui.settings.AboutScreen
import kr.ac.baekseok.readytogo.ui.settings.AccountScreen
import kr.ac.baekseok.readytogo.ui.settings.SettingsScreen
import kr.ac.baekseok.readytogo.ui.settings.SupportScreen
import kr.ac.baekseok.readytogo.ui.splash.SplashScreen
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import kr.ac.baekseok.readytogo.ui.trips.TripsScreen

/** 화면 주소. 문자열을 여기저기 적지 않도록 한 곳에 모은다. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"

    /** 탭 주소는 MainTab이 들고 있다 — 바와 그래프가 같은 값을 봐야 어긋나지 않는다. */
    val HOME = MainTab.HOME.route

    const val INPUT = "input"
    const val PLAN = "plan"

    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val SUPPORT = "support"
    const val ABOUT = "about"

    // 지도 주소는 없다 — 결과 화면의 탭이라 별도 화면이 아니다.
}

/**
 * 결과 화면으로 넘길 여행 조건을 담아두는 키.
 *
 * 조건은 필드가 여섯 개라 주소 문자열에 담기 어렵다. 대신 back stack의 SavedStateHandle에 넣는다.
 * TripCondition이 Serializable이라 Bundle에 그대로 들어가고, 앱이 메모리에서 내려갔다 돌아와도 살아남는다.
 */
private const val KEY_CONDITION = "condition"

@Composable
fun ReadytogoNavHost(
    navController: NavHostController = rememberNavController(),
) {
    MainScaffold(navController = navController) { scaffoldModifier ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = scaffoldModifier.fillMaxSize(),
        ) {
            entryGraph(navController)
            tabGraph(navController)
            planGraph(navController)
            settingsGraph(navController)
        }
    }
}

/** 스플래시 → 온보딩 → 로그인. 한 번 지나가면 back stack에 남기지 않는다. */
private fun NavGraphBuilder.entryGraph(navController: NavHostController) {
    composable(Routes.SPLASH) {
        SplashScreen(
            onReady = { authState ->
                val next = when {
                    // 온보딩을 안 봤으면 로그인보다 먼저 보여준다.
                    !AppContainer.onboardingSeen -> Routes.ONBOARDING
                    authState is AuthState.SignedIn || authState is AuthState.Guest -> Routes.HOME
                    else -> Routes.LOGIN
                }
                // 스플래시를 지워야 뒤로 가기로 다시 돌아오지 않는다.
                navController.navigate(next) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            },
        )
    }

    composable(Routes.ONBOARDING) {
        TabScreen {
            OnboardingScreen(
                onFinish = {
                    AppContainer.onboardingSeen = true
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
    }

    composable(Routes.LOGIN) {
        TabScreen {
            LoginScreen(
                onSignedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
    }
}

/**
 * 하단 탭 네 개.
 *
 * 탭 화면은 TopAppBar가 없어 상태바 여백을 스스로 받아야 한다 — [TabScreen]이 그 일을 한다.
 * 바깥 Scaffold에서 일괄로 주면 자기 TopAppBar를 가진 결과·입력 화면이 여백을 두 번 받는다.
 */
private fun NavGraphBuilder.tabGraph(navController: NavHostController) {
    composable(MainTab.HOME.route) {
        TabScreen {
            HomeScreen(
                onCreateTrip = { navController.navigate(Routes.INPUT) },
                onOpenTrip = { trip -> navController.openPlan(trip.condition) },
                onSeeAllTrips = { navController.navigate(MainTab.TRIPS.route) },
            )
        }
    }

    composable(MainTab.TRIPS.route) {
        TabScreen {
            TripsScreen(
                onOpenTrip = { trip -> navController.openPlan(trip.condition) },
                onCreateTrip = { navController.navigate(Routes.INPUT) },
            )
        }
    }

    composable(MainTab.SAVED.route) {
        TabScreen {
            // 장소 상세 화면은 아직 없다. 지도 앱으로 넘기는 것은 결과 화면 카드가 이미 한다.
            SavedScreen(onOpenPlace = {})
        }
    }

    composable(MainTab.PROFILE.route) {
        TabScreen {
            ProfileScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenSaved = { navController.navigate(MainTab.SAVED.route) },
                onSignIn = { navController.navigate(Routes.LOGIN) },
            )
        }
    }
}

/**
 * 제목 줄이 없는 화면에 상태바 여백을 준다.
 *
 * TopAppBar가 있는 화면(입력·결과·지도·설정)은 그쪽이 이미 여백을 먹으므로 쓰지 않는다.
 * 스플래시도 쓰지 않는다 — 배경을 화면 끝까지 채워야 해서 여백을 주면 위에 흰 띠가 생긴다.
 */
@Composable
private fun TabScreen(content: @Composable () -> Unit) {
    Box(modifier = Modifier.statusBarsPadding()) { content() }
}

/** 조건 입력 → 결과 → 지도. */
private fun NavGraphBuilder.planGraph(navController: NavHostController) {
    composable(Routes.INPUT) {
        // 이 화면은 자기 TopAppBar를 들고 있어 TitledScreen으로 감싸지 않는다 — 제목이 두 줄로 겹친다.
        TripInputScreen(
            onCreatePlan = { condition -> navController.openPlan(condition) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(Routes.PLAN) { entry ->
        // 조건을 이 화면 자신의 SavedStateHandle로 한 번 옮겨 담는다.
        //
        // previousBackStackEntry는 "NavController의 현재 화면 바로 이전"이라 위치에 따라 달라진다.
        // 지도 화면을 이 위에 쌓는 순간 previous가 입력 화면이 아니라 결과 화면 자신이 되어
        // 조건을 못 찾고 입력 화면으로 되돌아가버렸다. 자기 entry에 두면 그런 일이 없다.
        val condition = remember(entry) {
            entry.savedStateHandle.get<TripCondition>(KEY_CONDITION)
                ?: navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<TripCondition>(KEY_CONDITION)
                    ?.also { entry.savedStateHandle[KEY_CONDITION] = it }
        }

        if (condition == null) {
            // 조건 없이 결과 화면에 올 방법은 없지만, 복원 과정에서 유실되면 홈으로 되돌린다.
            // 화면을 그리는 도중에 이동시키면 안 되므로 LaunchedEffect로 미룬다.
            LaunchedEffect(Unit) {
                navController.popBackStack(Routes.HOME, inclusive = false)
            }
        } else {
            TripPlanScreen(
                condition = condition,
                onBack = { navController.popBackStack() },
                // 둘러보기 중 저장을 눌렀을 때. 로그인 화면을 이 위에 쌓는다 —
                // 로그인이 끝나면 보류해둔 일정이 자동으로 저장된다(AppContainer).
                onSignIn = { navController.navigate(Routes.LOGIN) },
            )
        }
    }

    // 지도는 더 이상 별도 화면이 아니다. 결과 화면의 "지도" 탭이 일차를 골라 보여준다.
    // 화면을 따로 띄우면 탭을 오가다 back stack이 꼬이고, 뒤로 가기가 어디로 가는지 애매해진다.
}

/** 설정과 그 하위 화면. 모두 뒤로 가기만 있으면 되는 단순 화면이다. */
private fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    composable(Routes.SETTINGS) {
        TitledScreen(
            title = stringResource(R.string.settings_title),
            onBack = { navController.popBackStack() },
        ) {
            SettingsScreen(
                onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
    }

    composable(Routes.ACCOUNT) {
        TitledScreen(
            title = stringResource(R.string.account_title),
            onBack = { navController.popBackStack() },
        ) {
            AccountScreen(
                onSignedOut = {
                    // 로그아웃하면 탭을 모두 비우고 로그인 화면으로 돌아간다.
                    // 탭을 남겨두면 로그인 뒤 뒤로 가기로 로그아웃된 화면에 들어갈 수 있다.
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }

    composable(Routes.SUPPORT) {
        TitledScreen(
            title = stringResource(R.string.support_title),
            onBack = { navController.popBackStack() },
        ) {
            SupportScreen()
        }
    }

    composable(Routes.ABOUT) {
        TitledScreen(
            title = stringResource(R.string.about_title),
            onBack = { navController.popBackStack() },
        ) {
            AboutScreen()
        }
    }
}

/**
 * 제목 + 뒤로 가기만 있는 화면 껍데기.
 *
 * 설정 하위 화면이 네 개라 각자 TopAppBar를 쓰면 제목 크기와 뒤로 가기 위치가 조금씩 어긋난다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitledScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = Tokens.Palette.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Tokens.Palette.Background,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) { content() }
    }
}

/**
 * 결과 화면으로 간다.
 *
 * 조건을 **출발한 화면**의 SavedStateHandle에 넣는다 — 결과 화면이 자기 entry로 옮겨 담는다.
 * 홈·여행 탭·입력 화면 세 곳에서 같은 일을 하므로 함수로 묶었다.
 */
private fun NavHostController.openPlan(condition: TripCondition) {
    currentBackStackEntry?.savedStateHandle?.set(KEY_CONDITION, condition)
    navigate(Routes.PLAN)
}