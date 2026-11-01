package kr.ac.baekseok.readytogo.ui.plan

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import kr.ac.baekseok.readytogo.ui.component.DashedDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.FavoriteKind
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.Flight
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.plan.LodgingDistanceCheck
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.checkLodgingDistance
import kr.ac.baekseok.readytogo.domain.plan.endsOf
import kr.ac.baekseok.readytogo.domain.recommend.medianCenter
import kr.ac.baekseok.readytogo.domain.purpose.PurposeProfiles
import kr.ac.baekseok.readytogo.ui.component.FavoriteButton
import kr.ac.baekseok.readytogo.ui.component.PhotoAttributionLine
import kr.ac.baekseok.readytogo.ui.component.PlacePhotoThumb
import kr.ac.baekseok.readytogo.ui.component.rememberPlacePhoto
import kr.ac.baekseok.readytogo.ui.format.money
import kr.ac.baekseok.readytogo.ui.input.DateRangeSheet
import kr.ac.baekseok.readytogo.ui.share.placeShareText
import kr.ac.baekseok.readytogo.ui.share.shareText
import kr.ac.baekseok.readytogo.ui.share.tripShareText
import kr.ac.baekseok.readytogo.ui.format.moneyRange
import kr.ac.baekseok.readytogo.ui.format.travelLegText
import kr.ac.baekseok.readytogo.ui.map.DayMapContent
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.ReadytogoTheme
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import java.time.LocalDate
import kr.ac.baekseok.readytogo.domain.weather.WeatherForecast
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** 출발일·귀국일을 모두 넣어 센 여행 일수. 기간 변경이 "날짜만 옮김"인지 가르는 기준이다. */
private fun dayCountOf(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to) + 1

private fun dayCountOf(condition: TripCondition): Long? {
    val from = condition.departureDate ?: return null
    val to = condition.returnDate ?: return null
    return dayCountOf(from, to)
}


/** 결과 화면의 탭. 순서가 화면에 나오는 순서다. */
private enum class PlanTab(@StringRes val labelRes: Int) {
    SUMMARY(R.string.plan_tab_summary),
    FLIGHT(R.string.plan_tab_flight),
    LODGING(R.string.plan_tab_lodging),
    ITINERARY(R.string.plan_tab_itinerary),
    MAP(R.string.plan_tab_map),
}

/**
 * 로드맵 4단계 — 결과 화면. 8단계에서 **탭 구조로 재배치**했다.
 *
 * 전에는 항공·숙소·일자별 일정을 세로 하나로 펼쳤는데, 5일 일정에서 스크롤이 너무 길어
 * 숙소를 다시 보려면 한참 올려야 했다. 보고 싶은 것만 골라 보게 탭으로 나눈다.
 */
@Composable
fun TripPlanScreen(
    condition: TripCondition,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    /** 기간을 바꿔 조건이 새로 정해졌다. 내비게이션이 담아 둔 조건을 갈아 끼운다. */
    onConditionChange: (TripCondition) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TripPlanViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val message by viewModel.message.collectAsState()
    val signInRequested by viewModel.signInRequested.collectAsState()
    val edits by viewModel.edits.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val pickedPlaces by viewModel.pickedPlaces.collectAsState()
    val canChangeDates by viewModel.canChangeDates.collectAsState()
    val conditionChange by viewModel.conditionChange.collectAsState()
    val canEditByText by viewModel.canEditByText.collectAsState()
    val editingByText by viewModel.editingByText.collectAsState()
    val reviewItems by viewModel.reviewItems.collectAsState()
    val reviewLoading by viewModel.reviewLoading.collectAsState()
    val reviewExpanded by viewModel.reviewExpanded.collectAsState()
    val weather by viewModel.weather.collectAsState()

    LaunchedEffect(conditionChange) {
        val next = conditionChange ?: return@LaunchedEffect
        onConditionChange(next)
        viewModel.clearConditionChange()
    }

    // 조건이 바뀔 때만 다시 만든다. 중복 호출 방지는 ViewModel이 한 번 더 막는다.
    LaunchedEffect(condition) { viewModel.load(condition) }

    TripPlanContent(
        uiState = uiState,
        favoriteIds = favoriteIds,
        favorites = favorites,
        pickedPlaces = pickedPlaces,
        saved = saved,
        message = message,
        signInRequested = signInRequested,
        edits = edits,
        canUndoDelete = canUndo,
        canChangeDates = canChangeDates,
        onChangeDates = viewModel::changeDates,
        onBack = onBack,
        onSignIn = onSignIn,
        onSignInRequestShown = viewModel::clearSignInRequest,
        onRetry = viewModel::retry,
        onToggleSaved = viewModel::toggleSaved,
        onToggleFavorite = viewModel::toggleFavorite,
        onMessageShown = viewModel::clearMessage,
        onDeleteItem = viewModel::deleteTimelineItem,
        onToggleDone = viewModel::toggleDone,
        textEdit = TextEditControls(
            enabled = canEditByText,
            busy = editingByText,
            onSubmit = viewModel::editByText,
            onSignIn = onSignIn,
        ),
        weather = weather,
        review = ReviewControls(
            items = reviewItems,
            loading = reviewLoading,
            expanded = reviewExpanded,
            onToggle = viewModel::toggleReview,
            onFix = viewModel::fixIssue,
        ),
        onReorder = viewModel::reorderDay,
        onReorderCommit = viewModel::commitEdits,
        onPickMeal = viewModel::pickMeal,
        onPickFavorite = viewModel::pickFavorite,
        onUndoDelete = viewModel::undoEdit,
        onUndoExpired = viewModel::clearUndo,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripPlanContent(
    uiState: TripPlanUiState,
    favoriteIds: Set<String> = emptySet(),
    favorites: List<FavoritePlace> = emptyList(),
    pickedPlaces: PickedPlaces = PickedPlaces(),
    saved: Boolean = false,
    message: String? = null,
    signInRequested: Boolean = false,
    edits: TimelineEdits = TimelineEdits(),
    canUndoDelete: Boolean = false,
    canChangeDates: Boolean = false,
    onChangeDates: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit = {},
    onSignInRequestShown: () -> Unit = {},
    onToggleSaved: () -> Unit = {},
    onToggleFavorite: (FavoritePlace) -> Unit = {},
    onMessageShown: () -> Unit = {},
    onDeleteItem: (Int, String) -> Unit = { _, _ -> },
    onToggleDone: (Int, String) -> Unit = { _, _ -> },
    textEdit: TextEditControls = TextEditControls(),
    review: ReviewControls = ReviewControls(),
    weather: WeatherForecast = WeatherForecast(),
    onReorder: (Int, List<String>) -> Unit = { _, _ -> },
    onReorderCommit: () -> Unit = {},
    onPickMeal: (Int, String, String?) -> Unit = { _, _, _ -> },
    onPickFavorite: (Int, String, FavoritePlace) -> Unit = { _, _, _ -> },
    onUndoDelete: () -> Unit = {},
    onUndoExpired: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(PlanTab.SUMMARY) }

    // 일정표에서 "지도로 보기"를 누르면 지도 탭으로 넘어가되 그 날짜가 이미 골라져 있어야 한다.
    // 따로 화면을 띄우던 것을 탭으로 흡수했으므로 선택한 일차를 여기서 들고 있는다.
    // 일정표 탭의 날짜 칩도 같은 값을 쓴다 — 두 탭이 다른 날을 보여주면 혼란스럽다.
    var mapDayNumber by rememberSaveable { mutableStateOf(1) }

    /**
     * 오늘이 이 여행의 몇 일차인지. 여행 기간이 아니면 null.
     *
     * 저장된 일정이 아니라 **계획의 날짜**로 따진다 — 둘러보기로 막 만든 일정에도
     * 여행 중 모드가 붙어야 하고, 그런 일정은 저장돼 있지 않아 SavedTrip이 없다.
     */
    val todayDayNumber = (uiState as? TripPlanUiState.Success)?.plan?.dailyPlans
        ?.firstOrNull { it.date == LocalDate.now() }?.dayNumber

    // 여행 중이면 일정표가 오늘 일차부터 열려야 한다. 여행 중에 이 화면을 여는 이유는
    // 예산을 다시 보려는 것이 아니라 오늘 어디로 가는지를 보려는 것이다.
    //
    // 계획이 도착해야 날짜를 알 수 있어 초기값으로는 못 정하고, 도착한 뒤 **한 번만**
    // 맞춘다 — 매번 맞추면 사용자가 고른 탭과 날짜가 화면이 다시 만들어질 때마다 되돌아간다.
    var openedOnToday by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(todayDayNumber) {
        if (todayDayNumber != null && !openedOnToday) {
            mapDayNumber = todayDayNumber
            selectedTab = PlanTab.ITINERARY
            openedOnToday = true
        }
    }

    // 기간 변경. 시트에서 고른 날짜가 일수를 바꾸면 확인을 먼저 받는다 — 편집이 사라지기 때문이다.
    var dateSheetOpen by remember { mutableStateOf(false) }
    var pendingDates by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }

    // 상세 시트를 띄울 항목. 일차 번호를 같이 들고 있어야 어느 날의 항목인지 알 수 있다.
    var openedItem by remember { mutableStateOf<Pair<Int, TimelineItem>?>(null) }

    // 공유를 **누른 순간에만** 글을 만든다. 글 짓는 함수가 @Composable이라 눌렀을 때
    // 부를 수 없어서, 깃발을 세우고 그다음 composition에서 만들어 바로 내보낸다.
    // 매 recomposition마다 닷새치 글을 새로 짜지 않으려는 것이다.
    var shareTripRequested by remember { mutableStateOf(false) }
    var sharePlace by remember { mutableStateOf<TimelineItem?>(null) }

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        // 방금 뺀 항목이 있으면 되돌리기 버튼을 같이 띄운다.
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = if (canUndoDelete) context.getString(R.string.plan_undo) else null,
            withDismissAction = false,
        )
        if (result == SnackbarResult.ActionPerformed) onUndoDelete() else onUndoExpired()
        onMessageShown()
    }

    // 둘러보기 중 저장을 눌렀을 때. 안내만 띄우면 어디서 로그인하는지 알 수 없어 버튼을 같이 둔다.
    LaunchedEffect(signInRequested) {
        if (!signInRequested) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.plan_sign_in_to_save),
            actionLabel = context.getString(R.string.common_sign_in),
            withDismissAction = false,
        )
        onSignInRequestShown()
        if (result == SnackbarResult.ActionPerformed) onSignIn()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.plan_title)) },
                    // 바탕과 같은 색. 기본값(흰색)이면 민트그레이 화면 위에 흰 띠가 뜬다.
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalPalette.current.Background),
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
                    },
                    actions = {
                        // 계획이 완성되기 전에는 공유할 내용도 저장할 내용도 없다.
                        if (uiState is TripPlanUiState.Success) {
                            IconButton(onClick = { shareTripRequested = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = stringResource(R.string.share_trip),
                                )
                            }
                            TextButton(onClick = onToggleSaved) {
                                Text(
                                    stringResource(
                                        if (saved) R.string.plan_unsave else R.string.plan_save,
                                    ),
                                )
                            }
                        }
                    },
                )

                if (uiState is TripPlanUiState.Success) {
                    // 탭 다섯 개가 좁은 화면에서 글자가 잘리므로 스크롤되는 탭 줄을 쓴다.
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                        containerColor = LocalPalette.current.Background,
                    ) {
                        PlanTab.entries.forEach { tab ->
                            Tab(
                                selected = tab == selectedTab,
                                onClick = { selectedTab = tab },
                                text = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is TripPlanUiState.Loading -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.plan_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is TripPlanUiState.Failure -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
                    modifier = Modifier.padding(Tokens.Space.xl),
                ) {
                    Text(uiState.message, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onRetry) { Text(stringResource(R.string.plan_retry)) }
                }

                is TripPlanUiState.Success -> PlanTabContent(
                    tab = selectedTab,
                    plan = uiState.plan,
                    favoriteIds = favoriteIds,
                    edits = edits,
                    pickedPlaces = pickedPlaces,
                    mapDayNumber = mapDayNumber,
                    todayDayNumber = todayDayNumber,
                    onToggleFavorite = onToggleFavorite,
                    onOpenMapForDay = { dayNumber ->
                        mapDayNumber = dayNumber
                        selectedTab = PlanTab.MAP
                    },
                    onSelectMapDay = { mapDayNumber = it },
                    onTimelineItemClick = { dayNumber, item ->
                        openedItem = dayNumber to item
                    },
                    onReorder = onReorder,
                    onReorderCommit = onReorderCommit,
                    onDeleteItem = onDeleteItem,
                    onToggleDone = onToggleDone,
                    textEdit = textEdit,
                    review = review,
                    weather = weather,
                    onEditDates = if (canChangeDates) ({ dateSheetOpen = true }) else null,
                )
            }
        }
    }

    val planCondition = (uiState as? TripPlanUiState.Success)?.plan?.condition
    if (dateSheetOpen && planCondition != null) {
        DateRangeSheet(
            initialDeparture = planCondition.departureDate,
            initialReturn = planCondition.returnDate,
            onConfirm = { from, to ->
                dateSheetOpen = false
                if (dayCountOf(from, to) == dayCountOf(planCondition)) {
                    onChangeDates(from, to)
                } else {
                    pendingDates = from to to
                }
            },
            onDismiss = { dateSheetOpen = false },
        )
    }

    pendingDates?.let { (from, to) ->
        AlertDialog(
            onDismissRequest = { pendingDates = null },
            title = { Text(stringResource(R.string.plan_dates_rebuild_title)) },
            text = { Text(stringResource(R.string.plan_dates_rebuild_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDates = null
                    onChangeDates(from, to)
                }) { Text(stringResource(R.string.plan_dates_rebuild_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDates = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // 깃발이 서 있는 동안에만 글을 만들고, 만들자마자 공유 시트를 띄운 뒤 내린다.
    if (shareTripRequested) {
        val plan = (uiState as? TripPlanUiState.Success)?.plan
        if (plan == null) {
            shareTripRequested = false
        } else {
            val text = tripShareText(plan, edits, pickedPlaces)
            LaunchedEffect(text) {
                context.shareText(text)
                shareTripRequested = false
            }
        }
    }

    sharePlace?.let { item ->
        val text = placeShareText(item)
        LaunchedEffect(text) {
            context.shareText(text)
            sharePlace = null
        }
    }

    openedItem?.let { (dayNumber, item) ->
        val plan = (uiState as? TripPlanUiState.Success)?.plan
        val planCity = plan?.condition?.destination?.city.orEmpty()

        PlaceDetailSheet(
            item = item,
            onOpenMap = { url ->
                // 지도 앱이 없는 기기도 있다. 예외가 나면 앱이 죽으므로 삼켜 둔다.
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
            // 후보가 하나뿐인 끼니와 관광지는 고를 것이 없다. 목록을 숨긴다.
            onPick = (item as? TimelineItem.Meal)
                ?.takeIf { it.options.size > 1 }
                ?.let { meal -> { placeId: String -> onPickMeal(dayNumber, meal.key, placeId) } },
            // 같은 도시, 같은 종류만 보여준다. 도쿄 일정에 오사카에서 담은 곳이 뜨면
            // 고를 수 있는 것처럼 보이지만 실제로는 갈 수 없다.
            //
            // 지금 그 자리에 들어 있는 곳도 뺀다. 골라 봐야 자기 자신으로 바뀌어
            // 아무 일도 일어나지 않는데, 목록에 있으면 바꿀 수 있는 것처럼 보인다.
            favorites = favorites.filter {
                it.kind == item.favoriteKind &&
                    it.cityName == planCity &&
                    it.placeId != item.placeId
            },
            dayCenter = dayCenterOf(plan, dayNumber),
            // 추천을 못 찾은 끼니 자리에도 끼울 수 있다. 오히려 그 자리에 가장 필요하다.
            onPickFavorite = { favorite: FavoritePlace ->
                onPickFavorite(dayNumber, item.key, favorite)
            },
            // 추천을 못 찾은 끼니는 보낼 장소가 없다.
            onShare = if (item is TimelineItem.NoMeal) null else ({ sharePlace = item }),
            onDelete = { onDeleteItem(dayNumber, item.key) },
            onDismiss = { openedItem = null },
        )
    }
}

@Composable
private fun PlanTabContent(
    tab: PlanTab,
    plan: TripPlan,
    favoriteIds: Set<String>,
    edits: TimelineEdits,
    pickedPlaces: PickedPlaces,
    mapDayNumber: Int,
    /** 오늘이 몇 일차인지. 여행 기간이 아니면 null. */
    todayDayNumber: Int?,
    onToggleFavorite: (FavoritePlace) -> Unit,
    onOpenMapForDay: (Int) -> Unit,
    onSelectMapDay: (Int) -> Unit,
    onTimelineItemClick: (Int, TimelineItem) -> Unit,
    onReorder: (Int, List<String>) -> Unit,
    onReorderCommit: () -> Unit,
    onDeleteItem: (Int, String) -> Unit,
    onToggleDone: (Int, String) -> Unit,
    textEdit: TextEditControls,
    /** 요약 탭의 검토 카드. 문제가 없으면 카드가 없다. */
    review: ReviewControls,
    /** 날짜별 예보. 일정표 날짜 칩 아이콘에 쓴다. */
    weather: WeatherForecast,
    /** 요약 머리의 기간을 눌렀을 때. null이면 바꿀 수 없는 일정이다. */
    onEditDates: (() -> Unit)?,
) {
    // **편집이 바뀔 때마다 다시 잰다.** 멀어서 지운 관광지가 계속 거리 계산에 들어가면
    // 경고가 사라지지 않는다. 계획 하나에 날짜 몇 개라 다시 계산해도 값이 싸다.
    val distanceCheck = remember(plan, edits, pickedPlaces) {
        plan.checkLodgingDistance(edits, pickedPlaces)
    }

    when (tab) {
        PlanTab.SUMMARY -> TabColumn {
            item { SummaryHeader(plan, onEditDates) }
            // 머리 바로 아래 — 예산보다 먼저 봐야 한다. 쉬는 날 가는 곳은 예산과 상관없이 헛걸음이다.
            if (review.items.isNotEmpty()) {
                item { ReviewCard(review) }
            }
            item { SectionTitle(stringResource(R.string.plan_section_budget)) }
            item { BudgetBreakdown(plan) }

            // 고른 항공·숙소를 요약에서도 보여준다. 탭을 옮겨 확인하지 않아도
            // "이 계획이 무엇인지"가 한 화면에 들어온다.
            plan.flights.firstOrNull()?.let { cheapest ->
                item { SectionTitle(stringResource(R.string.plan_section_flight)) }
                if (cheapest.isSample) {
                    item { SampleDataNotice() }
                }
                item { FlightCard(cheapest) }
            }

            plan.lodging?.let { pair ->
                item { SectionTitle(stringResource(R.string.plan_section_lodging)) }
                item { LodgingCard(pair) }

                // 숙소가 일정에서 먼 경우에만 한 줄 나온다. 자세한 날짜별 거리는 숙소 탭에 있다.
                distanceCheck?.let { check ->
                    item { LodgingDistanceLine(check) }
                }
            }

            item { SectionTitle(stringResource(R.string.plan_section_highlights)) }
            item { ItineraryHighlights(plan) }
        }

        PlanTab.FLIGHT -> TabColumn {
            item { SectionTitle(stringResource(R.string.plan_section_flights_cheapest)) }
            // 실제 검색이 안 됐을 때 예시 가격을 진짜로 오해하면 잘못된 예산을 세우게 된다.
            if (plan.flights.firstOrNull()?.isSample == true) {
                item { SampleDataNotice() }
            }
            items(plan.flights.take(FLIGHTS_SHOWN), key = { it.id }) { FlightCard(it) }
        }

        PlanTab.LODGING -> TabColumn {
            val pair = plan.lodging
            if (pair == null) {
                item {
                    Text(
                        text = stringResource(R.string.plan_lodging_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                item { SectionTitle(stringResource(R.string.plan_section_lodging)) }
                item { LodgingCard(pair) }

                // 편집(교체·삭제·순서)이 바뀌면 distanceCheck가 다시 계산돼 여기도 갱신된다.
                distanceCheck?.let { check ->
                    item { SectionTitle(stringResource(R.string.plan_lodging_distance_title)) }
                    item { LodgingDistanceCard(check) }
                }
            }
        }

        PlanTab.ITINERARY -> TimelineTab(
            plan = plan,
            selectedDayNumber = mapDayNumber,
            edits = edits,
            pickedPlaces = pickedPlaces,
            favoriteIds = favoriteIds,
            farDayNumbers = distanceCheck?.farDays?.map { it.dayNumber }?.toSet().orEmpty(),
            todayDayNumber = todayDayNumber,
            onSelectDay = onSelectMapDay,
            onOpenMapForDay = onOpenMapForDay,
            onItemClick = onTimelineItemClick,
            onReorder = onReorder,
            onReorderCommit = onReorderCommit,
            onDelete = onDeleteItem,
            onToggleDone = onToggleDone,
            textEdit = textEdit,
            weather = weather,
            onToggleFavorite = onToggleFavorite,
        )

        PlanTab.MAP -> MapTab(
            plan = plan,
            edits = edits,
            pickedPlaces = pickedPlaces,
            selectedDayNumber = mapDayNumber,
            onSelectDay = onSelectMapDay,
        )
    }
}

/** 탭 본문이 쓰는 스크롤 목록. 탭마다 같은 여백과 간격을 쓰게 묶었다. */
@Composable
private fun TabColumn(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Tokens.Space.lg),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        content = content,
    )
}

/**
 * 지도 탭 — 일차를 고르면 그날 동선을 보여준다.
 *
 * 전에는 "지도로 보기"가 별도 화면을 띄웠다. 지도가 탭이 되면서 그 화면을 흡수했고,
 * 지도를 그리는 코드는 [DayMapContent] 한 곳에만 둔다.
 */
@Composable
private fun MapTab(
    plan: TripPlan,
    edits: TimelineEdits,
    pickedPlaces: PickedPlaces,
    selectedDayNumber: Int,
    onSelectDay: (Int) -> Unit,
) {
    val day = plan.dailyPlans.firstOrNull { it.dayNumber == selectedDayNumber }
        ?: plan.dailyPlans.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = (day?.dayNumber ?: 1) - 1,
            edgePadding = 0.dp,
            containerColor = LocalPalette.current.Background,
        ) {
            plan.dailyPlans.forEach { candidate ->
                Tab(
                    selected = candidate.dayNumber == day?.dayNumber,
                    onClick = { onSelectDay(candidate.dayNumber) },
                    text = {
                        Text(stringResource(R.string.plan_day_tab, candidate.dayNumber))
                    },
                )
            }
        }

        if (day == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.plan_map_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            DayMapContent(
                day = day,
                // 아직 사용자가 고르기 전이라 "추천" 쪽을 대표로 찍는다.
                lodging = plan.lodging?.qualityFirst?.item,
                // 지도가 일정표와 같은 순서·장소를 보여야 한다. 편집을 빼면 지운 곳이 지도에 남는다.
                edits = edits,
                picked = pickedPlaces,
                // 도착일은 공항에서 시작, 출국일은 공항에서 끝. 끼니 식당을 고른 기준과 같은 규칙이다.
                ends = plan.endsOf(day),
                airport = plan.airport,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = Tokens.Space.sm),
    )
}

@Composable
private fun BudgetBreakdown(plan: TripPlan) {
    val b = plan.budget
    if (b.totalWon == null) {
        Text(
            text = stringResource(R.string.plan_budget_no_limit),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        ) {
            PurposeShareBar(plan)

            BudgetRow(stringResource(R.string.plan_budget_flight), b.flightWon)
            // 단위를 적어 둔다. 인원이 늘면 이 값이 내려가는데, 무엇의 상한인지 모르면
            // "예산이 줄었다"로 읽힌다 — 실제로는 1인·1실 몫이 줄어든 것이다.
            BudgetRow(
                label = if (b.roomCount > 1) {
                    stringResource(R.string.plan_budget_lodging_cap_rooms, b.roomCount)
                } else {
                    stringResource(R.string.plan_budget_lodging_cap)
                },
                amount = b.lodgingPerNightWon,
            )
            BudgetRow(
                label = if (b.adults > 1) {
                    stringResource(R.string.plan_budget_meal_cap_adults, b.adults)
                } else {
                    stringResource(R.string.plan_budget_meal_cap)
                },
                amount = b.mealWon,
            )
            BudgetRow(stringResource(R.string.plan_budget_other), b.otherWon)

            if (b.flightAloneExceedsBudget) {
                Text(
                    text = stringResource(R.string.plan_budget_flight_exceeds),
                    style = MaterialTheme.typography.bodySmall,
                    // 예산 초과는 코랄이다. 붉은 Danger는 삭제·탈퇴처럼 되돌릴 수 없는
                    // 일에만 쓴다 — 예산을 넘은 건 잘못이 아니라 눈에 걸려야 할 것이다.
                    color = LocalPalette.current.Accent,
                )
            }
        }
    }
}

/**
 * 목적별 배분 막대 — 아래 금액 네 줄이 **왜 그 금액인지**를 설명하는 그림.
 *
 * 금액만 나열하면 "숙소 상한 18만원"이 어디서 나온 수인지 알 수 없다. 고른 목적이
 * 항공료를 뺀 나머지를 어떤 비율로 갈랐는지 보여주면, 목적을 바꿀 때 무엇이 달라질지도
 * 같이 읽힌다(CLAUDE.md 여행 목적별 설정).
 *
 * **요약 헤더가 아니라 이 카드 안에 둔다.** 헤더에는 이미 "예산 사용" 막대가 있는데,
 * 성격이 다른 막대 두 개가 붙어 있으면 어느 쪽이 무엇인지 헷갈린다.
 */
@Composable
private fun PurposeShareBar(plan: TripPlan) {
    // 기준 금액이 없으면 막대의 설명이 성립하지 않는다. 예산을 안 넣었으면 호출부가
    // 여기까지 오지 않지만, 값이 비는 경우에 빈 막대만 남는 것보다 안 그리는 쪽이 낫다.
    val remaining = plan.budget.remainingWon ?: return

    val profile = PurposeProfiles.of(plan.condition.purposes)
    val palette = LocalPalette.current

    // 색은 브랜드 세 색을 그대로 쓴다. 막대마다 새 색을 만들면 화면에 색이 늘어난다.
    val shares = listOf(
        Triple(stringResource(R.string.plan_share_lodging), profile.lodgingShare, palette.Point),
        Triple(stringResource(R.string.plan_share_food), profile.foodShare, palette.Accent),
        Triple(
            stringResource(R.string.plan_share_other),
            profile.otherShare,
            palette.SecondaryBold,
        ),
    )

    Text(
        text = stringResource(R.string.plan_share_title),
        style = MaterialTheme.typography.labelMedium,
        color = palette.TextSecondary,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Tokens.Space.sm)
            .clip(Tokens.Radius.chip),
    ) {
        shares.forEach { (_, share, color) ->
            Box(
                modifier = Modifier
                    .weight(share.toFloat())
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }

    // 막대만 두면 어느 칸이 무엇인지 알 수 없다. 색과 비율을 글자로 한 번 더 적는다.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        modifier = Modifier.padding(top = Tokens.Space.xs),
    ) {
        shares.forEach { (label, share, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Tokens.Space.sm)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = " " + stringResource(
                        R.string.plan_share_legend,
                        label,
                        (share * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.TextSecondary,
                )
            }
        }
    }

    // 목적 이름은 buildString 밖에서 미리 푼다 — joinToString의 람다는 inline이 아니라
    // 그 안에서 stringResource를 부를 수 없다.
    val purposeNames = plan.condition.purposes.map { stringResource(it.labelRes) }

    Text(
        text = if (purposeNames.isEmpty()) {
            stringResource(R.string.plan_share_note_default, money(remaining))
        } else {
            stringResource(
                R.string.plan_share_note,
                money(remaining),
                purposeNames.joinToString("·"),
            )
        },
        style = MaterialTheme.typography.labelMedium,
        color = palette.TextSecondary,
        modifier = Modifier.padding(bottom = Tokens.Space.xs),
    )

    DashedDivider(color = palette.Divider)
}

@Composable
private fun BudgetRow(label: String, amount: Long?) {
    if (amount == null) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(money(amount), style = MaterialTheme.typography.bodyMedium)
    }
}

/** 항공권이 실제 검색 결과가 아닐 때 그 사실을 분명히 알린다. */
@Composable
private fun SampleDataNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // 연노랑 바탕. 붉은 오류 색을 쓰면 앱이 고장 난 것처럼 보이는데,
            // 실제로는 "이 가격은 참고용"이라는 주의지 실패가 아니다.
            containerColor = LocalPalette.current.Secondary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        ) {
            Text(stringResource(R.string.plan_sample_title), fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.plan_sample_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FlightCard(flight: Flight) {
    val open = rememberUrlOpener(flight.bookingUrl.takeUnless { flight.isSample })
    Card(modifier = Modifier.fillMaxWidth(), onClick = open ?: {}, enabled = open != null) {
        Column(
            modifier = Modifier.padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        ) {
            // "가는 편 · 왕복 총액"을 적는다. SerpApi 1차 호출은 가는 편만 주는데
            // 가격은 왕복 총액이다. 라벨이 없으면 편도 가격으로 오해한다.
            Text(
                text = stringResource(R.string.plan_flight_outbound_total),
                style = MaterialTheme.typography.labelMedium,
                color = LocalPalette.current.Point,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${flight.airline} ${flight.flightNumber}", fontWeight = FontWeight.Bold)
                Text(money(flight.priceWon), fontWeight = FontWeight.Bold)
            }
            Text(
                text = stringResource(
                    R.string.plan_flight_route,
                    flight.departureAirport,
                    flight.departureAt.format(TIME),
                    flight.arrivalAirport,
                    flight.arrivalAt.format(TIME),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (flight.stops == 0) {
                    stringResource(R.string.plan_flight_direct)
                } else {
                    pluralStringResource(R.plurals.stops_count, flight.stops, flight.stops)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LodgingCard(pair: RecommendationPair<Lodging>) {
    val alternative = pair.withinBudget

    // 두 선택지의 1박가가 같으면 가격으로는 고를 수 없다. Places가 요금을 안 줘서
    // 등급에서 추정한 값이라 같은 등급이면 같은 금액이 나온다 — 자주 생기는 일이다.
    // 그때는 가격을 흐리게 내리고 **리뷰 수**를 굵게 올려 판단 근거를 바꿔 준다.
    val samePrice = alternative != null &&
        pair.qualityFirst.item.priceWon == alternative.item.priceWon

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            if (samePrice) {
                Text(
                    text = stringResource(R.string.plan_lodging_same_price),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPalette.current.TextSecondary,
                )
            }

            ChoiceRow(
                label = pair.labelOfFirst(),
                overBudget = pair.qualityFirst.overBudget,
                title = pair.qualityFirst.item.name,
                price = pair.qualityFirst.item.priceText(),
                detail = pair.qualityFirst.item.detailLine(),
                extra = pair.qualityFirst.distanceLine(),
                url = pair.qualityFirst.item.bookingUrl,
                emphasis = if (samePrice) RowEmphasis.REVIEWS else RowEmphasis.PRICE,
                reviewCount = pair.qualityFirst.item.reviewCount,
                placeId = pair.qualityFirst.item.id,
            )
            if (alternative == null) {
                NoAlternativeNotice()
            } else {
                ChoiceRow(
                    label = pair.labelOfSecond(),
                    overBudget = alternative.overBudget,
                    title = alternative.item.name,
                    price = alternative.item.priceText(),
                    detail = alternative.item.detailLine(),
                    extra = alternative.distanceLine(),
                    url = alternative.item.bookingUrl,
                    emphasis = if (samePrice) RowEmphasis.REVIEWS else RowEmphasis.PRICE,
                    reviewCount = alternative.item.reviewCount,
                    placeId = alternative.item.id,
                )
            }
        }
    }
}

/**
 * 숙소 거리 한 줄 요약. 요약 탭과 숙소 탭이 같은 문장을 쓴다.
 *
 * **전부 가까우면 아무것도 띄우지 않는다.** 멀쩡한데 뜨는 경고는 다음부터 읽히지 않는다.
 */
@Composable
private fun LodgingDistanceLine(check: LodgingDistanceCheck) {
    val palette = LocalPalette.current

    when {
        check.mostlyFar -> Text(
            text = stringResource(
                R.string.plan_lodging_mostly_far,
                check.days.size,
                check.farDays.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.Danger,
        )

        check.farDays.isNotEmpty() -> Text(
            // 날짜 이름을 먼저 만들어 둔다. joinToString 안에서는 리소스를 못 읽는다.
            text = stringResource(
                R.string.plan_lodging_far_days,
                check.farDays
                    .map { stringResource(R.string.plan_day_tab, it.dayNumber) }
                    .joinToString(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.TextSecondary,
        )
    }
}

/**
 * 숙소가 날짜별 일정에서 얼마나 떨어져 있는지.
 *
 * 먼 날이 없으면 날짜 목록만 담담하게 보여준다.
 */
@Composable
private fun LodgingDistanceCard(check: LodgingDistanceCheck) {
    val palette = LocalPalette.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Tokens.Space.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            LodgingDistanceLine(check)

            check.days.forEach { day ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.plan_day_tab, day.dayNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.TextPrimary,
                    )
                    Text(
                        text = "${travelLegText(day.travel)} · ${"%.1f".format(day.travel.distanceKm)}km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (day.isFar) {
                        Text(
                            text = stringResource(R.string.plan_lodging_far),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.Danger,
                        )
                    }
                }
            }

            // 과반이 멀 때만 나온다. 바꾸지는 않고 "이런 곳도 있다"만 보여준다 —
            // 숙소를 바꾸려면 편집에 숙소 칸을 새로 만들어야 해서 다음으로 미뤘다.
            check.better?.let { better ->
                DashedDivider(color = palette.Divider)
                ChoiceRow(
                    label = stringResource(R.string.plan_lodging_better_label),
                    overBudget = false,
                    title = better.lodging.name,
                    price = better.lodging.priceText(),
                    detail = better.lodging.detailLine(),
                    extra = stringResource(
                        R.string.plan_lodging_worst_day,
                        "%.1f".format(better.worstDistanceKm),
                    ),
                    url = better.lodging.bookingUrl,
                    reviewCount = better.lodging.reviewCount,
                )
            }
        }
    }
}

/** 선택지 한 줄에서 오른쪽에 무엇을 강조할지. */
enum class RowEmphasis { PRICE, REVIEWS }

/** 후보가 하나뿐이라 대안을 만들 수 없을 때. 같은 곳을 두 번 보여주지 않는다. */
@Composable
private fun NoAlternativeNotice() {
    Text(
        text = stringResource(R.string.plan_no_alternative),
        style = MaterialTheme.typography.bodySmall,
        color = LocalPalette.current.TextSecondary,
    )
}

/**
 * 즐겨찾기에 담을 모양으로 바꾼다.
 *
 * 가격·평점은 담지 않는다 — 시간이 지나면 틀려지는 값이라 열어볼 때 새로 받는 게 맞다
 * ([FavoritePlace] 주석 참고).
 */
internal fun Restaurant.toFavorite(cityName: String) = FavoritePlace(
    placeId = id,
    name = name,
    category = category,
    kind = FavoriteKind.RESTAURANT,
    location = location,
    cityName = cityName,
    mapUrl = mapUrl,
)

internal fun Attraction.toFavorite(cityName: String) = FavoritePlace(
    placeId = id,
    name = name,
    // 관광지는 Places 카테고리 표시명을 따로 들고 있지 않다. 비워 두면 화면이
    // 종류 이름("관광지")으로 대신 채운다 — 번역된 글자를 저장하면 나중에 언어를
    // 바꿔도 저장될 때의 언어가 그대로 남는다.
    category = "",
    kind = FavoriteKind.ATTRACTION,
    location = location,
    cityName = cityName,
    mapUrl = mapUrl,
)

/**
 * 식당 가격 표기.
 *
 * 가격대를 알면 범위를 그대로 보여준다 — 단일 금액만 보여주면 사용자가 그 값만 믿게 되는데,
 * 실제로는 범위 안에서 움직인다. 예산 판정은 상한 기준이라 모자랄 일은 없다.
 * 현지 통화를 같이 적는 이유는 메뉴판과 바로 대조하기 위함이다.
 */
@Composable
internal fun Restaurant.priceText(): String {
    // 즐겨찾기를 끼워 넣었는데 가격대를 못 받은 경우. 0원을 보여주면 공짜로 읽힌다.
    if (priceUnknown) return stringResource(R.string.plan_price_unknown)

    val krw = when {
        priceIsUpperBound -> stringResource(R.string.plan_price_upper_bound, money(priceWon))
        minPriceWon != null && minPriceWon != priceWon -> moneyRange(minPriceWon, priceWon)
        else -> money(priceWon)
    }
    return when {
        priceEstimated -> stringResource(R.string.plan_price_estimated, krw)
        localPriceText != null -> "$krw\n$localPriceText"
        else -> krw
    }
}

/**
 * 선택지 한 줄.
 *
 * 카드를 합치면서 예산 초과를 카드 배경색으로 알릴 수 없게 됐다.
 * 대신 이름표와 가격을 오류 색으로 칠해 한 줄 단위로 구분한다.
 */
@Composable
private fun ChoiceRow(
    label: String,
    overBudget: Boolean,
    title: String,
    price: String,
    detail: String,
    extra: String? = null,
    url: String? = null,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    emphasis: RowEmphasis = RowEmphasis.PRICE,
    /** [RowEmphasis.REVIEWS]일 때 가격 대신 굵게 보여줄 리뷰 수. */
    reviewCount: Int = 0,
    /**
     * 사진을 붙일 장소의 id. 비우면 사진 없이 글자만 나온다.
     *
     * 모든 선택지에 붙이지 않는 이유는 과금이다 — 사진을 보여줄 자리를 좁게 고른
     * 배경은 GooglePlacesPhotoRepository 주석에 있다.
     */
    placeId: String? = null,
) {
    val accent: Color =
        // 예산 초과는 코랄. 붉은 Danger는 되돌릴 수 없는 일에만 쓴다(SummaryTab 주석 참고).
        if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point
    val open = rememberUrlOpener(url)
    val photo = placeId?.let { rememberPlacePhoto(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // ♥는 줄 전체의 클릭과 겹치지 않는다 — Row의 clickable은 ♥ 아래로 전달되지 않는다.
            .then(if (open != null) Modifier.clickable(onClick = open) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
    ) {
        if (placeId != null) {
            PlacePhotoThumb(placeId = placeId, photo = photo, size = Tokens.Size.placeThumb)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            if (extra != null) {
                Text(
                    extra,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPalette.current.TextSecondary,
                )
            }
            PhotoAttributionLine(photo)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (emphasis == RowEmphasis.REVIEWS) {
                Text(
                    text = stringResource(R.string.plan_reviews, "%,d".format(reviewCount)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) accent else Color.Unspecified,
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPalette.current.TextSecondary,
                )
            } else {
                Text(
                    price,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) accent else Color.Unspecified,
                )
            }
        }
        if (onToggleFavorite != null) {
            FavoriteButton(
                favorite = favorite,
                onToggle = onToggleFavorite,
            )
        }
    }
}

/**
 * 숙소 가격 표기.
 *
 * Places는 호텔 요금을 주지 않아 등급에서 추정한 값이다. 확정 금액으로 오해하지 않도록
 * "예상"을 앞에 붙인다.
 */
@Composable
private fun Lodging.priceText(): String = stringResource(
    if (priceEstimated) R.string.plan_lodging_price_estimated else R.string.plan_lodging_price,
    money(priceWon),
)

@Composable
private fun Lodging.detailLine(): String = "${ratingReviews(rating, reviewCount)} · $address"

@Composable
internal fun Restaurant.detailLine(): String =
    "$category · ${ratingReviews(rating, reviewCount)} · $address"

/** "★4.2 (리뷰 15,935)" — 숙소·식당이 같은 모양으로 적는다. */
@Composable
private fun ratingReviews(rating: Double, reviewCount: Int): String =
    stringResource(R.string.plan_rating_reviews, rating.toString(), "%,d".format(reviewCount))

/**
 * 두 선택지의 이름표.
 *
 * 두 번째 칸은 상황마다 다른 기준으로 뽑히므로 "다른 선택" 같은 모호한 이름 대신
 * 어떤 기준이었는지를 그대로 적는다 — 예산 내 / 가성비 / 동선 우선.
 */
@Composable
private fun RecommendationPair<*>.labelOfFirst(): String = stringResource(
    if (qualityFirst.overBudget) {
        R.string.plan_choice_recommended_over
    } else {
        R.string.plan_choice_recommended
    },
)

@Composable
private fun RecommendationPair<*>.labelOfSecond(): String = stringResource(alternativeKind.labelRes)

/**
 * 링크를 브라우저·지도 앱으로 여는 동작을 만든다. 열 수 없는 주소면 null을 돌려준다.
 *
 * 예약 기능은 넣지 않는다(CLAUDE.md 범위). 항공권은 Google Flights, 숙소·식당은
 * Google Maps로 넘겨 사용자가 직접 확인하게만 한다.
 *
 * Mock 데이터의 example.com은 실제로 열리지 않는 주소라 누를 수 없게 막는다 —
 * 눌렀는데 빈 페이지가 뜨는 것이 아예 안 눌리는 것보다 나쁘다.
 */
@Composable
private fun rememberUrlOpener(url: String?): (() -> Unit)? {
    val context = LocalContext.current
    val usable = url?.takeIf { it.startsWith("http") && !it.contains("example.com") }
        ?: return null

    return remember(usable) {
        {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(usable))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 열 수 있는 앱이 없으면 그냥 아무 일도 일어나지 않게 둔다. 크래시보다는 낫다.
            runCatching { context.startActivity(intent) }
            Unit
        }
    }
}

/** 왜 이 곳이 추천됐는지 알 수 있게 동선 근거를 한 줄 덧붙인다. */
@Composable
private fun Recommendation<*>.distanceLine(): String? {
    val km = distanceFromCenterKm ?: return null
    return stringResource(R.string.plan_distance_from_center, "%.1f".format(km))
}

private const val FLIGHTS_SHOWN = 3

@Preview(showBackground = true)
@Composable
private fun TripPlanLoadingPreview() {
    ReadytogoTheme {
        Surface { TripPlanContent(TripPlanUiState.Loading, onBack = {}, onRetry = {}) }
    }
}
/** 이 자리에 끼울 수 있는 즐겨찾기 종류. 끼니 자리에는 식당, 관광지 자리에는 관광지다. */
private val TimelineItem.favoriteKind: FavoriteKind
    get() = when (this) {
        is TimelineItem.Visit -> FavoriteKind.ATTRACTION
        is TimelineItem.Meal, is TimelineItem.NoMeal -> FavoriteKind.RESTAURANT
    }

/**
 * 그날 동선의 중심점. "거리 먼 곳" 표시의 기준이다.
 *
 * 숙소·식당 추천과 같은 기준(위·경도 중앙값)을 쓴다 — 화면마다 다른 중심을 쓰면
 * 같은 장소가 어디서는 멀고 어디서는 가까운 곳이 된다.
 */
private fun dayCenterOf(plan: TripPlan?, dayNumber: Int): LatLng? {
    val day = plan?.dailyPlans?.firstOrNull { it.dayNumber == dayNumber } ?: return null
    return medianCenter(day.attractions.map { it.location })
}
