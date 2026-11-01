package kr.ac.baekseok.readytogo.ui.input

import kr.ac.baekseok.readytogo.ui.component.ScreenTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import kr.ac.baekseok.readytogo.ui.component.PillChip
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.format.tripRangeText
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
        containerColor = LocalPalette.current.Background,
        topBar = { ScreenTopBar(title = stringResource(R.string.input_title), onBack = onBack) },
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

            FormCard {
                PartySection(
                    adults = condition.adults,
                    roomCount = condition.roomCount,
                    onChange = viewModel::onAdultsChange,
                )
            }

            FormCard {
                BudgetSection(
                    budgetWon = condition.budgetWon ?: TripInputViewModel.DEFAULT_BUDGET_WON,
                    adults = condition.adults,
                    onBudgetSelected = viewModel::onBudgetSelected,
                    onBudgetTyped = viewModel::onBudgetTyped,
                )
            }

            FormCard {
                PurposeSection(
                    selected = condition.purposes,
                    onToggle = viewModel::onPurposeToggle,
                )
            }

            // 마지막 칩이 하단 바에 딱 붙어 빽빽해 보인다. 영어처럼 칩이 두 줄로
            // 넘어가는 언어에서 더 심하다. 스크롤 끝에 숨 쉴 자리를 둔다.
            Spacer(modifier = Modifier.height(Tokens.Space.sm))
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
            .background(LocalPalette.current.Surface)
            .padding(vertical = Tokens.Space.lg),
    ) {
        RouteEnd(
            label = stringResource(R.string.input_origin_label),
            value = originName,
            placeholder = stringResource(R.string.input_origin_placeholder),
            onClick = onOriginClick,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = LocalPalette.current.TextSecondary,
            modifier = Modifier.size(Tokens.Size.icon),
        )
        RouteEnd(
            label = stringResource(R.string.input_destination_label),
            value = destinationName,
            placeholder = stringResource(R.string.input_destination_placeholder),
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
            color = LocalPalette.current.TextSecondary,
        )
        Text(
            text = value ?: placeholder,
            style = MaterialTheme.typography.titleLarge,
            // 아직 안 고른 자리는 흐리게 둬서 "눌러야 하는 곳"으로 보이게 한다.
            color = if (value != null) LocalPalette.current.TextPrimary else LocalPalette.current.TextSecondary,
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
            .background(LocalPalette.current.Surface)
            .padding(Tokens.Space.lg),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = LocalPalette.current.TextSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
            Text(
                text = stringResource(R.string.input_dates),
                style = MaterialTheme.typography.labelMedium,
                color = LocalPalette.current.TextSecondary,
            )
            Text(
                text = if (departure != null && returnDate != null) {
                    tripRangeText(departure, returnDate)
                } else {
                    stringResource(R.string.input_dates_placeholder)
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (departure != null && returnDate != null) {
                    LocalPalette.current.TextPrimary
                } else {
                    LocalPalette.current.TextSecondary
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
/**
 * 인원 선택.
 *
 * 숫자를 직접 입력받지 않고 −/+ 로 둔 이유: 1~6명이라 범위가 좁고, 키보드를 띄우면
 * 아래 섹션이 가려진다. 범위 끝에서는 버튼을 눌러도 안 되는 게 보이도록 흐리게 둔다.
 *
 * 방 수를 같이 보여주는 이유: 3명이면 2실이 되는데, 숙소 예산이 왜 그렇게 잡혔는지
 * 결과 화면에서 처음 알게 되면 늦다.
 */
@Composable
private fun PartySection(
    adults: Int,
    roomCount: Int,
    onChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.input_people),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onChange(-1) },
                enabled = adults > TripCondition.MIN_ADULTS,
            ) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = stringResource(R.string.input_people_decrease))
            }
            Text(
                text = stringResource(R.string.input_people_count, adults),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(Tokens.Size.avatar),
            )
            IconButton(
                onClick = { onChange(1) },
                enabled = adults < TripCondition.MAX_ADULTS,
            ) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = stringResource(R.string.input_people_increase))
            }
        }
        Text(
            text = pluralStringResource(R.plurals.input_people_note, roomCount, roomCount),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalPalette.current.TextSecondary,
        )
    }
}

@Composable
private fun PurposeSection(
    selected: Set<TravelPurpose>,
    onToggle: (TravelPurpose) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.md)) {
        Text(
            text = stringResource(R.string.input_purpose_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.input_purpose_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalPalette.current.TextSecondary,
        )
        // FlowRow를 쓰는 이유: 칩 네 개를 Row로 균등 분할하면 "액티비티"가 "액티"로 잘린다.
        // 글자 길이에 맞춰 두고, 한 줄에 안 들어가면 다음 줄로 넘긴다.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TravelPurpose.entries.forEach { purpose ->
                PillChip(
                    text = stringResource(purpose.labelRes),
                    selected = purpose in selected,
                    onClick = { onToggle(purpose) },
                    icon = purpose.icon,
                    container = LocalPalette.current.Background,
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
    Surface(color = LocalPalette.current.Background) {
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
                    color = LocalPalette.current.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryButton(
                text = stringResource(R.string.input_create_plan),
                enabled = enabled,
                onClick = onClick,
            )
        }
    }
}

/**
 * 인원·예산·목적을 담는 흰 카드. 위의 노선·날짜 카드와 같은 바탕에 올려 다섯 덩어리가
 * 같은 무게로 읽히게 한다 — 앞 둘만 카드면 아래 셋이 배경에 떠 있는 것처럼 보였다.
 */
@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(LocalPalette.current.Surface)
            .padding(Tokens.Space.lg),
    ) {
        content()
    }
}
