package kr.ac.baekseok.readytogo.ui.plan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.ui.component.FavoriteButton
import kr.ac.baekseok.readytogo.ui.map.DayMapContent
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.ReadytogoTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

// 요일이 기기 언어를 따라가면 한국어 화면에 "9.26(Sat)"이 섞인다. 한국어로 고정한다.
internal val DATE = DateTimeFormatter.ofPattern("M.d(E)", Locale.KOREAN)
private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** 금액 표기를 한 곳에서 맞춘다. */
internal fun Long.won(): String = "%,d원".format(this)

/** 결과 화면의 탭. 순서가 화면에 나오는 순서다. */
private enum class PlanTab(val label: String) {
    SUMMARY("요약"),
    FLIGHT("항공"),
    LODGING("숙소"),
    ITINERARY("일정표"),
    MAP("지도"),
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
    modifier: Modifier = Modifier,
    viewModel: TripPlanViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val message by viewModel.message.collectAsState()
    val signInRequested by viewModel.signInRequested.collectAsState()
    val edits by viewModel.edits.collectAsState()
    val lastDeleted by viewModel.lastDeleted.collectAsState()

    // 조건이 바뀔 때만 다시 만든다. 중복 호출 방지는 ViewModel이 한 번 더 막는다.
    LaunchedEffect(condition) { viewModel.load(condition) }

    TripPlanContent(
        uiState = uiState,
        favoriteIds = favoriteIds,
        saved = saved,
        message = message,
        signInRequested = signInRequested,
        edits = edits,
        canUndoDelete = lastDeleted != null,
        onBack = onBack,
        onSignIn = onSignIn,
        onSignInRequestShown = viewModel::clearSignInRequest,
        onRetry = viewModel::retry,
        onToggleSaved = viewModel::toggleSaved,
        onToggleFavorite = viewModel::toggleFavorite,
        onMessageShown = viewModel::clearMessage,
        onDeleteItem = viewModel::deleteTimelineItem,
        onSwapMeal = viewModel::toggleMealSwap,
        onUndoDelete = viewModel::undoDelete,
        onUndoExpired = viewModel::clearLastDeleted,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripPlanContent(
    uiState: TripPlanUiState,
    favoriteIds: Set<String> = emptySet(),
    saved: Boolean = false,
    message: String? = null,
    signInRequested: Boolean = false,
    edits: TimelineEdits = TimelineEdits(),
    canUndoDelete: Boolean = false,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit = {},
    onSignInRequestShown: () -> Unit = {},
    onToggleSaved: () -> Unit = {},
    onToggleFavorite: (FavoritePlace) -> Unit = {},
    onMessageShown: () -> Unit = {},
    onDeleteItem: (Int, String) -> Unit = { _, _ -> },
    onSwapMeal: (Int, String) -> Unit = { _, _ -> },
    onUndoDelete: () -> Unit = {},
    onUndoExpired: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(PlanTab.SUMMARY) }

    // 일정표에서 "지도로 보기"를 누르면 지도 탭으로 넘어가되 그 날짜가 이미 골라져 있어야 한다.
    // 따로 화면을 띄우던 것을 탭으로 흡수했으므로 선택한 일차를 여기서 들고 있는다.
    // 일정표 탭의 날짜 칩도 같은 값을 쓴다 — 두 탭이 다른 날을 보여주면 혼란스럽다.
    var mapDayNumber by rememberSaveable { mutableStateOf(1) }

    // 상세 시트를 띄울 항목. 일차 번호를 같이 들고 있어야 어느 날의 항목인지 알 수 있다.
    var openedItem by remember { mutableStateOf<Pair<Int, TimelineItem>?>(null) }
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        // 방금 뺀 항목이 있으면 되돌리기 버튼을 같이 띄운다.
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = if (canUndoDelete) "되돌리기" else null,
            withDismissAction = false,
        )
        if (result == SnackbarResult.ActionPerformed) onUndoDelete() else onUndoExpired()
        onMessageShown()
    }

    // 둘러보기 중 저장을 눌렀을 때. 안내만 띄우면 어디서 로그인하는지 알 수 없어 버튼을 같이 둔다.
    LaunchedEffect(signInRequested) {
        if (!signInRequested) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "로그인하면 이 일정을 저장할 수 있습니다.",
            actionLabel = "로그인",
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
                    title = { Text("여행 계획") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
                    actions = {
                        // 계획이 완성되기 전에는 저장할 내용이 없다.
                        if (uiState is TripPlanUiState.Success) {
                            TextButton(onClick = onToggleSaved) {
                                Text(if (saved) "저장 해제" else "저장")
                            }
                        }
                    },
                )

                if (uiState is TripPlanUiState.Success) {
                    // 탭 다섯 개가 좁은 화면에서 글자가 잘리므로 스크롤되는 탭 줄을 쓴다.
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                    ) {
                        PlanTab.entries.forEach { tab ->
                            Tab(
                                selected = tab == selectedTab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.label) },
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text("계획을 만드는 중입니다", style = MaterialTheme.typography.bodyMedium)
                }

                is TripPlanUiState.Failure -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(uiState.message, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onRetry) { Text("다시 시도") }
                }

                is TripPlanUiState.Success -> PlanTabContent(
                    tab = selectedTab,
                    plan = uiState.plan,
                    favoriteIds = favoriteIds,
                    edits = edits,
                    mapDayNumber = mapDayNumber,
                    onToggleFavorite = onToggleFavorite,
                    onOpenMapForDay = { dayNumber ->
                        mapDayNumber = dayNumber
                        selectedTab = PlanTab.MAP
                    },
                    onSelectMapDay = { mapDayNumber = it },
                    onTimelineItemClick = { dayNumber, item ->
                        openedItem = dayNumber to item
                    },
                )
            }
        }
    }

    openedItem?.let { (dayNumber, item) ->
        PlaceDetailSheet(
            item = item,
            onOpenMap = { url ->
                // 지도 앱이 없는 기기도 있다. 예외가 나면 앱이 죽으므로 삼켜 둔다.
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
            // 대안이 없는 끼니와 관광지는 교체할 것이 없다. 버튼을 숨긴다.
            onSwap = (item as? TimelineItem.Meal)
                ?.takeIf { it.other != null }
                ?.let { meal -> { onSwapMeal(dayNumber, meal.key) } },
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
    mapDayNumber: Int,
    onToggleFavorite: (FavoritePlace) -> Unit,
    onOpenMapForDay: (Int) -> Unit,
    onSelectMapDay: (Int) -> Unit,
    onTimelineItemClick: (Int, TimelineItem) -> Unit,
) {
    when (tab) {
        PlanTab.SUMMARY -> TabColumn {
            item { SummaryHeader(plan) }
            item { SectionTitle("예산 분해") }
            item { BudgetBreakdown(plan) }

            // 고른 항공·숙소를 요약에서도 보여준다. 탭을 옮겨 확인하지 않아도
            // "이 계획이 무엇인지"가 한 화면에 들어온다.
            plan.flights.firstOrNull()?.let { cheapest ->
                item { SectionTitle("항공") }
                if (cheapest.isSample) {
                    item { SampleDataNotice() }
                }
                item { FlightCard(cheapest) }
            }

            plan.lodging?.let { pair ->
                item { SectionTitle("숙소") }
                item { LodgingCard(pair) }
            }

            item { SectionTitle("일정 하이라이트") }
            item { ItineraryHighlights(plan) }
        }

        PlanTab.FLIGHT -> TabColumn {
            item { SectionTitle("항공권 (최저가순)") }
            // 실제 검색이 안 됐을 때 예시 가격을 진짜로 오해하면 잘못된 예산을 세우게 된다.
            if (plan.flights.firstOrNull()?.isSample == true) {
                item { SampleDataNotice() }
            }
            items(plan.flights.take(FLIGHTS_SHOWN), key = { it.id }) { FlightCard(it) }
        }

        PlanTab.LODGING -> TabColumn {
            val pair = plan.lodging
            if (pair == null) {
                item { Text("숙소를 찾지 못했습니다", style = MaterialTheme.typography.bodyMedium) }
            } else {
                item { SectionTitle("숙소") }
                item { LodgingCard(pair) }
            }
        }

        PlanTab.ITINERARY -> TimelineTab(
            plan = plan,
            selectedDayNumber = mapDayNumber,
            edits = edits,
            favoriteIds = favoriteIds,
            onSelectDay = onSelectMapDay,
            onOpenMapForDay = onOpenMapForDay,
            onItemClick = onTimelineItemClick,
            onToggleFavorite = onToggleFavorite,
        )

        PlanTab.MAP -> MapTab(
            plan = plan,
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
private fun MapTab(plan: TripPlan, selectedDayNumber: Int, onSelectDay: (Int) -> Unit) {
    val day = plan.dailyPlans.firstOrNull { it.dayNumber == selectedDayNumber }
        ?: plan.dailyPlans.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = (day?.dayNumber ?: 1) - 1,
            edgePadding = 0.dp,
        ) {
            plan.dailyPlans.forEach { candidate ->
                Tab(
                    selected = candidate.dayNumber == day?.dayNumber,
                    onClick = { onSelectDay(candidate.dayNumber) },
                    text = { Text("${candidate.dayNumber}일차") },
                )
            }
        }

        if (day == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("표시할 일정이 없습니다", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            DayMapContent(
                day = day,
                // 아직 사용자가 고르기 전이라 "추천" 쪽을 대표로 찍는다.
                lodging = plan.lodging?.qualityFirst?.item,
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
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun BudgetBreakdown(plan: TripPlan) {
    val b = plan.budget
    if (b.totalWon == null) {
        Text("예산을 입력하지 않아 상한 없이 추천했습니다", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BudgetRow("항공", b.flightWon)
            // 단위를 적어 둔다. 인원이 늘면 이 값이 내려가는데, 무엇의 상한인지 모르면
            // "예산이 줄었다"로 읽힌다 — 실제로는 1인·1실 몫이 줄어든 것이다.
            BudgetRow(
                label = if (b.roomCount > 1) "숙소 1박 상한 (1실 × ${b.roomCount}실)" else "숙소 1박 상한",
                amount = b.lodgingPerNightWon,
            )
            BudgetRow(
                label = if (b.adults > 1) "식사 1끼 상한 (1인 × ${b.adults}명)" else "식사 1끼 상한",
                amount = b.mealWon,
            )
            BudgetRow("교통·입장료·예비비", b.otherWon)

            if (b.flightAloneExceedsBudget) {
                Text(
                    "항공료만으로 예산을 넘습니다. 예산을 올리거나 날짜를 바꿔보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(label: String, amount: Long?) {
    if (amount == null) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(amount.won(), style = MaterialTheme.typography.bodyMedium)
    }
}

/** 항공권이 실제 검색 결과가 아닐 때 그 사실을 분명히 알린다. */
@Composable
private fun SampleDataNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("예시 데이터", fontWeight = FontWeight.Bold)
            Text(
                "항공권을 실제로 검색하지 못해 예시 가격을 보여주고 있습니다. " +
                    "실제 요금과 다를 수 있습니다.",
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // "가는 편 · 왕복 총액"을 적는다. SerpApi 1차 호출은 가는 편만 주는데
            // 가격은 왕복 총액이다. 라벨이 없으면 편도 가격으로 오해한다.
            Text(
                text = "가는 편 · 왕복 총액",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${flight.airline} ${flight.flightNumber}", fontWeight = FontWeight.Bold)
                Text(flight.priceWon.won(), fontWeight = FontWeight.Bold)
            }
            Text(
                "${flight.departureAirport} ${flight.departureAt.format(TIME)}" +
                    " → ${flight.arrivalAirport} ${flight.arrivalAt.format(TIME)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (flight.stops == 0) "직항" else "경유 ${flight.stops}회",
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (samePrice) {
                Text(
                    text = "두 곳의 예상 1박가가 같습니다. 리뷰 수를 기준으로 보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
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
        text = "조건에 맞는 다른 후보가 없습니다",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun DayCard(
    day: DayPlan,
    cityName: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (FavoritePlace) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AttractionBlock(
                attractions = day.attractions,
                cityName = cityName,
                favoriteIds = favoriteIds,
                onToggleFavorite = onToggleFavorite,
            )

            MealTime.entries.forEach { meal ->
                val pair = day.meals[meal] ?: return@forEach
                HorizontalDivider()
                MealBlock(
                    meal = meal,
                    pair = pair,
                    cityName = cityName,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun AttractionBlock(
    attractions: List<Attraction>,
    cityName: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (FavoritePlace) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("가볼 곳", fontWeight = FontWeight.Bold)
        if (attractions.isEmpty()) {
            Text("이동·휴식에 쓰는 날입니다", style = MaterialTheme.typography.bodyMedium)
        } else {
            attractions.forEach { attraction ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${attraction.name} · ${attraction.stayMinutes}분 · ★${attraction.rating}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    FavoriteButton(
                        favorite = attraction.id in favoriteIds,
                        onToggle = { onToggleFavorite(attraction.toFavorite(cityName)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MealBlock(
    meal: MealTime,
    pair: RecommendationPair<Restaurant>,
    cityName: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (FavoritePlace) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(meal.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        ChoiceRow(
            label = pair.labelOfFirst(),
            overBudget = pair.qualityFirst.overBudget,
            title = pair.qualityFirst.item.name,
            price = pair.qualityFirst.item.priceText(),
            detail = pair.qualityFirst.item.detailLine(),
            extra = pair.qualityFirst.distanceLine(),
            url = pair.qualityFirst.item.mapUrl,
            favorite = pair.qualityFirst.item.id in favoriteIds,
            onToggleFavorite = { onToggleFavorite(pair.qualityFirst.item.toFavorite(cityName)) },
        )
        val alternative = pair.withinBudget
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
                url = alternative.item.mapUrl,
                favorite = alternative.item.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(alternative.item.toFavorite(cityName)) },
            )
        }
    }
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
    // 관광지는 Places 카테고리 표시명을 따로 들고 있지 않다. 종류 이름으로 대신한다.
    category = FavoriteKind.ATTRACTION.label,
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
internal fun Restaurant.priceText(): String {
    val krw = when {
        priceIsUpperBound -> "${priceWon.won()} 이하"
        minPriceWon != null && minPriceWon != priceWon ->
            "${"%,d".format(minPriceWon)}~${priceWon.won()}"
        else -> priceWon.won()
    }
    return when {
        priceEstimated -> "$krw (추정)"
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
) {
    val accent: Color =
        // 예산 초과는 코랄. 붉은 Danger는 되돌릴 수 없는 일에만 쓴다(SummaryTab 주석 참고).
        if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point
    val open = rememberUrlOpener(url)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // ♥는 줄 전체의 클릭과 겹치지 않는다 — Row의 clickable은 ♥ 아래로 전달되지 않는다.
            .then(if (open != null) Modifier.clickable(onClick = open) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            if (extra != null) {
                Text(
                    extra,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (emphasis == RowEmphasis.REVIEWS) {
                Text(
                    text = "리뷰 ${"%,d".format(reviewCount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (overBudget) accent else Color.Unspecified,
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
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
private fun Lodging.priceText(): String =
    if (priceEstimated) "예상 1박 약 ${priceWon.won()}" else "${priceWon.won()} / 1박"

private fun Lodging.detailLine(): String =
    "★$rating (리뷰 ${"%,d".format(reviewCount)}) · $address"

internal fun Restaurant.detailLine(): String =
    "$category · ★$rating (리뷰 ${"%,d".format(reviewCount)}) · $address"

/**
 * 두 선택지의 이름표.
 *
 * 두 번째 칸은 상황마다 다른 기준으로 뽑히므로 "다른 선택" 같은 모호한 이름 대신
 * 어떤 기준이었는지를 그대로 적는다 — 예산 내 / 가성비 / 동선 우선.
 */
private fun RecommendationPair<*>.labelOfFirst(): String =
    if (qualityFirst.overBudget) "추천 · 예산 초과" else "추천"

private fun RecommendationPair<*>.labelOfSecond(): String = alternativeKind.label

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
private fun Recommendation<*>.distanceLine(): String? =
    distanceFromCenterKm?.let { "일정 중심에서 %.1fkm".format(it) }

private const val FLIGHTS_SHOWN = 3

@Preview(showBackground = true)
@Composable
private fun TripPlanLoadingPreview() {
    ReadytogoTheme {
        Surface { TripPlanContent(TripPlanUiState.Loading, onBack = {}, onRetry = {}) }
    }
}