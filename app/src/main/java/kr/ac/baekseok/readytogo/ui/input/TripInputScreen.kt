package kr.ac.baekseok.readytogo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 여행 조건 입력 화면.
 *
 * 8단계에서 전면 개편했다. 전에는 입력 칸 일곱 개가 세로로 쌓여 있어 무엇을 채워야 하는지
 * 한눈에 안 보였고, 나라를 고른 뒤에야 도시를 고를 수 있었다.
 *
 * 지금은 **네 덩어리**다 — 어디서 어디로 / 언제 / 예산 / 무엇을 하러.
 * 출발지·목적지는 "인천 → 도쿄" 한 장의 카드로 합쳐 여행의 모양이 먼저 보이게 했다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInputScreen(
    onCreatePlan: (TripCondition) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripInputViewModel = viewModel(),
) {
    val condition by viewModel.condition.collectAsState()
    val options by viewModel.options.collectAsState()
    val search by viewModel.search.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val canCreatePlan by viewModel.canCreatePlan.collectAsState()
    val missingHint by viewModel.missingHint.collectAsState()

    // 지금 열려 있는 시트. null이면 아무것도 안 열려 있다.
    var openSheet by remember { mutableStateOf<DestinationTarget?>(null) }
    var showDateSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Tokens.Palette.Background,
        topBar = {
            TopAppBar(
                title = { Text("새 여행") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Tokens.Palette.Background,
                ),
            )
        },
        bottomBar = {
            // CTA를 하단에 고정한다. 스크롤 끝까지 내려가야 버튼이 나오면 못 찾는 사람이 생긴다.
            CreatePlanBar(
                enabled = canCreatePlan,
                missingHint = missingHint,
                onClick = { onCreatePlan(condition) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Tokens.Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xl),
        ) {
            RouteCard(
                originName = condition.origin?.shortName,
                destinationName = condition.destination?.shortName,
                onOriginClick = { openSheet = DestinationTarget.ORIGIN },
                onDestinationClick = { openSheet = DestinationTarget.DESTINATION },
            )

            DateCard(
                departure = condition.departureDate,
                returnDate = condition.returnDate,
                onClick = { showDateSheet = true },
            )

            BudgetSection(
                budgetWon = condition.budgetWon ?: TripInputViewModel.DEFAULT_BUDGET_WON,
                onBudgetSelected = viewModel::onBudgetSelected,
                onBudgetTyped = viewModel::onBudgetTyped,
            )

            PurposeSection(
                selected = condition.purposes,
                onToggle = viewModel::onPurposeToggle,
            )
        }
    }

    openSheet?.let { target ->
        DestinationSheet(
            target = target,
            options = options,
            search = search,
            selectedCountry = selectedCountry,
            onQueryChange = viewModel::onSearchQueryChange,
            onCountrySelected = viewModel::onCountrySelected,
            onBackToCountries = { viewModel.onCountryCleared() },
            onPicked = { destination ->
                if (target == DestinationTarget.ORIGIN) {
                    viewModel.onOriginSelected(destination)
                } else {
                    viewModel.onCitySelected(destination)
                }
                viewModel.onSearchClosed()
                openSheet = null
            },
            onDismiss = {
                viewModel.onSearchClosed()
                openSheet = null
            },
        )
    }

    if (showDateSheet) {
        DateRangeSheet(
            initialDeparture = condition.departureDate,
            initialReturn = condition.returnDate,
            onConfirm = { departure, returnDate ->
                viewModel.onDateRangeChange(departure, returnDate)
                showDateSheet = false
            },
            onDismiss = { showDateSheet = false },
        )
    }
}

/**
 * "인천 → 도쿄" 카드.
 *
 * 출발지와 목적지를 한 장에 둔 이유: 둘은 따로 고르는 값이 아니라 "이 여행의 노선"
 * 하나다. 칸을 둘로 나눠 두면 화면에서 둘의 관계가 보이지 않는다.
 */
@Composable
private fun RouteCard(
    originName: String?,
    destinationName: String?,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(Tokens.Palette.SurfaceMuted)
            .padding(vertical = Tokens.Space.lg),
    ) {
        RouteEnd(
            label = "출발",
            value = originName,
            placeholder = "출발지",
            onClick = onOriginClick,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Tokens.Palette.TextSecondary,
            modifier = Modifier.size(Tokens.Size.icon),
        )
        RouteEnd(
            label = "도착",
            value = destinationName,
            placeholder = "목적지",
            onClick = onDestinationClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RouteEnd(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.Space.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Tokens.Palette.TextSecondary,
        )
        Text(
            text = value ?: placeholder,
            style = MaterialTheme.typography.titleLarge,
            // 아직 안 고른 자리는 흐리게 둬서 "눌러야 하는 곳"으로 보이게 한다.
            color = if (value != null) Tokens.Palette.TextPrimary else Tokens.Palette.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DateCard(
    departure: java.time.LocalDate?,
    returnDate: java.time.LocalDate?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .clickable(onClick = onClick)
            .background(Tokens.Palette.SurfaceMuted)
            .padding(Tokens.Space.lg),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = Tokens.Palette.TextSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
            Text(
                text = "여행 날짜",
                style = MaterialTheme.typography.labelMedium,
                color = Tokens.Palette.TextSecondary,
            )
            Text(
                text = if (departure != null && returnDate != null) {
                    formatTripRange(departure, returnDate)
                } else {
                    "날짜를 고르세요"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (departure != null && returnDate != null) {
                    Tokens.Palette.TextPrimary
                } else {
                    Tokens.Palette.TextSecondary
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurposeSection(
    selected: Set<TravelPurpose>,
    onToggle: (TravelPurpose) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.md)) {
        Text(text = "무엇을 하러 가세요?", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "고른 목적에 따라 예산 배분과 하루 일정 밀도가 달라집니다. 여러 개 고를 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.Palette.TextSecondary,
        )
        // FlowRow를 쓰는 이유: 칩 네 개를 Row로 균등 분할하면 "액티비티"가 "액티"로 잘린다.
        // 글자 길이에 맞춰 두고, 한 줄에 안 들어가면 다음 줄로 넘긴다.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TravelPurpose.entries.forEach { purpose ->
                FilterChip(
                    selected = purpose in selected,
                    onClick = { onToggle(purpose) },
                    label = { Text(text = purpose.label, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            imageVector = purpose.icon,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    shape = Tokens.Radius.chip,
                )
            }
        }
    }
}

/** 목적별 아이콘. 글자만 있는 칩보다 눈에 먼저 들어온다. */
private val TravelPurpose.icon: ImageVector
    get() = when (this) {
        TravelPurpose.REST -> Icons.Filled.Spa
        TravelPurpose.SIGHTSEEING -> Icons.Filled.Museum
        TravelPurpose.FOOD -> Icons.Filled.Restaurant
        TravelPurpose.ACTIVITY -> Icons.Filled.Hiking
    }

/** 하단 고정 CTA. 못 누르는 이유를 버튼 위에 적는다. */
@Composable
private fun CreatePlanBar(
    enabled: Boolean,
    missingHint: String?,
    onClick: () -> Unit,
) {
    Surface(color = Tokens.Palette.Background) {
        Column(
            modifier = Modifier.padding(
                start = Tokens.Space.screenHorizontal,
                end = Tokens.Space.screenHorizontal,
                top = Tokens.Space.md,
                bottom = Tokens.Space.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        ) {
            if (missingHint != null) {
                Text(
                    text = missingHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = Tokens.Palette.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryButton(text = "계획 만들기", enabled = enabled, onClick = onClick)
        }
    }
}
