package kr.ac.baekseok.readytogo.ui.home

import kr.ac.baekseok.readytogo.ui.theme.pressClickable
import kr.ac.baekseok.readytogo.ui.component.FitText
import kr.ac.baekseok.readytogo.ui.format.nightsText
import kr.ac.baekseok.readytogo.ui.component.DashedDivider
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.PastTripChip
import kr.ac.baekseok.readytogo.ui.component.PhotoCard
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.component.rememberPlacePhoto
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.isAppInDarkTheme
import kr.ac.baekseok.readytogo.ui.format.dateShort
import kr.ac.baekseok.readytogo.ui.format.savedTripDestination
import kr.ac.baekseok.readytogo.ui.format.savedTripTitle
import kr.ac.baekseok.readytogo.ui.format.daysText
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import java.time.LocalDate

/**
 * 홈 — 새 여행 만들기, 다가오는 여행 D-day, 최근 일정.
 *
 * 맨 위에 인사말과 CTA를 두는 이유: 이 앱에서 사용자가 가장 자주 하는 일이 "새 일정 만들기"다.
 * 저장된 일정이 없는 첫 사용자에게는 그것 말고 할 일이 없다.
 */
@Composable
fun HomeScreen(
    onCreateTrip: () -> Unit,
    onOpenTrip: (SavedTrip) -> Unit,
    onSeeAllTrips: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Surface(color = LocalPalette.current.Background) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeList(
                state = state,
                onCreateTrip = onCreateTrip,
                onOpenTrip = onOpenTrip,
                onSeeAllTrips = onSeeAllTrips,
            )
        }
    }
}

@Composable
private fun HomeList(
    state: HomeUiState,
    onCreateTrip: () -> Unit,
    onOpenTrip: (SavedTrip) -> Unit,
    onSeeAllTrips: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Tokens.Space.screenHorizontal,
            end = Tokens.Space.screenHorizontal,
            top = Tokens.Space.xl,
            bottom = Tokens.Space.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
    ) {
        item {
            Greeting(
                authState = state.authState,
                nickname = state.nickname,
                hasUpcoming = state.upcoming != null,
                onCreateTrip = onCreateTrip,
            )
        }

        // 다가오는 여행이 없으면 카드를 아예 내린다. 빈 카드는 자리만 차지한다.
        state.upcoming?.let { trip ->
            item {
                HeroTripCard(trip = trip, today = state.today, onClick = { onOpenTrip(trip) })
            }
            // 검토 0건이거나 계획을 아직 만들지 않았으면 카드가 없다(HomeViewModel.reviewOf).
            state.review?.let { review ->
                item {
                    HomeReviewCard(
                        review = review,
                        city = savedTripDestination(trip),
                        onClick = { onOpenTrip(trip) },
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.home_recent),
                actionLabel = stringResource(R.string.common_see_all)
                    .takeIf { state.recent.isNotEmpty() },
                onAction = onSeeAllTrips.takeIf { state.recent.isNotEmpty() },
            )
        }

        if (state.recent.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 빈 화면에 글자만 있으면 로딩이 덜 끝난 것처럼 보인다.
                    // 앱 그림을 옅게 깔아 "아직 아무것도 없는 자리"로 읽히게 한다.
                    //
                    // **단색으로 칠하지 않고 알파만 낮춘다.** 그림이 여러 색이라 tint를 걸면
                    // 알파가 있는 곳이 전부 한 색으로 뭉개져 덩어리가 된다.
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_art),
                        contentDescription = null,
                        alpha = EMPTY_STATE_ART_ALPHA,
                        modifier = Modifier
                            .padding(top = Tokens.Space.xl)
                            .size(Tokens.Size.emptyStateSymbol),
                    )

                    // 둘러보기 중이면 "만든 일정이 없다"가 아니라 "저장이 안 된다"가 맞는 설명이다.
                    EmptyState(
                        title = stringResource(
                            if (state.authState is AuthState.Guest) {
                                R.string.home_empty_guest_title
                            } else {
                                R.string.home_empty_title
                            },
                        ),
                        description = stringResource(R.string.home_empty_body),
                        actionLabel = stringResource(R.string.home_empty_action),
                        onAction = onCreateTrip,
                    )
                }
            }
        } else {
            // 2열 카드(design/mockup/home). LazyColumn 안이라 격자 대신 두 장씩 한 줄로 묶는다.
            items(state.recent.chunked(2), key = { row -> row.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md)) {
                    row.forEachIndexed { index, trip ->
                        RecentTripCard(
                            trip = trip,
                            today = state.today,
                            // 왼쪽·오른쪽 블록 색을 엇갈려 칠한다. 같은 색이 나란히 서면 한 덩어리로 보인다.
                            alternate = index == 1,
                            onClick = { onOpenTrip(trip) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 맨 위 — 작은 인사말과 큰 제목(design/mockup/home).
 *
 * 다가오는 여행이 있으면 큰 제목이 "다음 여행"이 되어 바로 아래 히어로 카드의 머리 역할을 한다.
 * 없으면 "어디로 갈까요?"로 새 여행 만들기를 권한다.
 *
 * 제목 줄 오른쪽의 "새 여행 만들기" pill: 예전에는 떠 있는 버튼(FAB)이었는데 2열 카드가 생기며
 * 오른쪽 카드를 가렸다. 제목 줄에 두면 아무것도 가리지 않고, 홈을 열자마자 눈에 들어온다.
 */
@Composable
private fun Greeting(
    authState: AuthState,
    nickname: String?,
    hasUpcoming: Boolean,
    onCreateTrip: () -> Unit,
) {
    val name = when (authState) {
        // 사용자가 정한 닉네임이 먼저다. 아직 없으면(옛 계정) Google 계정 이름으로 부른다.
        is AuthState.SignedIn -> nickname ?: authState.user.displayName
        // 둘러보기 중에는 이름이 없다. "여행자님"처럼 부르면 로그인한 것처럼 보여 혼란스럽다.
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Text(
            text = if (name != null) {
                stringResource(R.string.home_greeting_named, name)
            } else {
                stringResource(R.string.home_greeting)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPalette.current.TextSecondary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            Text(
                text = stringResource(if (hasUpcoming) R.string.home_upcoming else R.string.home_title_no_trip),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
                modifier = Modifier
                    .clip(Tokens.Radius.button)
                    .background(LocalPalette.current.Point)
                    .pressClickable(onClick = onCreateTrip)
                    .padding(horizontal = Tokens.Space.md, vertical = Tokens.Space.sm),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = LocalPalette.current.Surface,
                    modifier = Modifier.size(Tokens.Size.inlineIcon),
                )
                Text(
                    text = stringResource(R.string.home_create_trip),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalPalette.current.Surface,
                )
            }
        }
        if (authState is AuthState.Guest) {
            Text(
                text = stringResource(R.string.home_guest_note),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
            )
        }
    }
}

/**
 * 다음 여행 히어로 카드(design/mockup/home). 위에 칩 두 개(D-day, 기간·인원), 아래에 기간과 도시명을
 * 크게, 오른쪽 아래에 여는 화살표. 사진이 있으면 사진, 없으면 깊은 터쿼이즈 단색 + 원 무늬.
 *
 * 시안의 예산 칸은 넣지 않는다 — 새 기능 범위 밖이다. 화살표는 카드를 누른 것과 같다.
 */
@Composable
private fun HeroTripCard(trip: SavedTrip, today: LocalDate, onClick: () -> Unit) {
    val daysLeft = trip.daysUntil(today)
    val nights = trip.dayCount - 1
    val days = daysText(trip.dayCount)
    val length = if (nights > 0) "${nightsText(nights)} $days" else days
    val people = pluralStringResource(R.plurals.people_count, trip.condition.adults, trip.condition.adults)

    PhotoCard(
        key = trip.destinationName,
        title = "",
        // 대표 장소 사진. 없거나 설정에서 껐으면 아래 단색 + 원 무늬가 보인다.
        photoPlaceId = trip.coverPlaceId,
        // 그라데이션 대신 **단색**이고, 깊은 쪽 터쿼이즈를 쓴다 — 포인트 원색 위 흰 글자는 대비가
        // 모자라다. HeroGradient의 끝 색이 모드·고대비마다 이미 그 역할로 맞춰져 있다.
        colors = LocalPalette.current.HeroGradient.last().let { listOf(it, it) },
        scrim = false,
        overlayHasText = true,
        height = Tokens.Size.heroCardHeight,
        shape = Tokens.Radius.hero,
        decoration = { HeroRings() },
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Tokens.Space.lg),
        ) {
            HeroChip(
                when {
                    daysLeft > 0 -> stringResource(R.string.home_dday, daysLeft.toInt())
                    // 떠난 뒤로는 "3일차". 끝난 여행은 이 카드에 오지 않는다(isUpcoming).
                    // **출발일도 1일차로 센다.** 여행 중 모드·일정표 날짜 칩과 같은 이름을 쓴다.
                    else -> stringResource(R.string.plan_day_tab, trip.dayNumberOn(today) ?: trip.dayCount)
                },
            )
            HeroChip("$length · $people")
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = Tokens.Space.lg,
                    bottom = Tokens.Space.lg,
                    end = Tokens.Size.heroArrow + Tokens.Space.xl,
                ),
        ) {
            Text(
                text = stringResource(
                    R.string.fmt_date_range,
                    dateShort(trip.departureDate),
                    dateShort(trip.returnDate),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = HERO_SUBTEXT_ALPHA),
            )
            FitText(
                text = savedTripDestination(trip),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Tokens.Space.lg)
                .size(Tokens.Size.heroArrow)
                .clip(Tokens.Radius.card)
                .background(LocalPalette.current.Surface)
                .pressClickable(onClick = onClick),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.plan_title),
                tint = LocalPalette.current.TextPrimary,
            )
        }
    }
}

/** 히어로 위 반투명 칩. 바탕 색·사진이 비치게 해 카드와 한 덩어리로 보인다. */
@Composable
private fun HeroChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(Tokens.Radius.chip)
            .background(Color.White.copy(alpha = HERO_CHIP_ALPHA))
            .padding(horizontal = Tokens.Space.md, vertical = Tokens.Space.sm),
    )
}

/**
 * 홈 검토 카드(design/mockup/home). 결과 화면 요약 탭 카드의 요약판 — 첫 문제 한 줄과 건수만.
 * 누르면 그 여행 결과 화면으로 간다(고치기는 거기서).
 */
@Composable
private fun HomeReviewCard(review: HomeReview, city: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .background(palette.Surface)
            .pressClickable(onClick = onClick)
            .padding(Tokens.Space.lg),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Tokens.Size.reviewIcon)
                    .clip(CircleShape)
                    .background(palette.PointLight),
            ) {
                Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = palette.PointDark)
            }
            Text(
                text = stringResource(R.string.review_card_title, review.count),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(text = city, style = MaterialTheme.typography.bodyMedium, color = palette.TextSecondary)
        }
        DashedDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            FitText(
                text = review.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = palette.TextPrimary,
            )
        }
    }
}

/**
 * 최근 일정 2열 카드(design/mockup/home). 위에 색 블록(사진 자리), 아래 제목과 날짜.
 *
 * 블록은 사진이 있으면 사진, 없으면 연한 민트·크림 단색이다. 예전 목록 줄에 넣던 앱 그림 표식은
 * 시안에 없어 뺐다 — 카드가 두 장씩 서면 그림이 반복돼 오히려 산만했다.
 */
@Composable
private fun RecentTripCard(
    trip: SavedTrip,
    today: LocalDate,
    alternate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val photo = trip.coverPlaceId?.let { rememberPlacePhoto(it) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        modifier = modifier
            .clip(Tokens.Radius.card)
            .background(palette.Surface)
            .pressClickable(onClick = onClick)
            .padding(Tokens.Space.md),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Tokens.Size.recentBlockHeight)
                .clip(Tokens.Radius.image)
                .background(if (alternate) palette.Secondary else palette.PointLight),
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (!trip.isUpcoming(today)) {
                PastTripChip(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Tokens.Space.sm),
                )
            }
        }
        FitText(text = savedTripTitle(trip), style = MaterialTheme.typography.titleSmall)
        FitText(
            text = "${dateShort(trip.departureDate)} · ${daysText(trip.dayCount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.TextSecondary,
        )
    }
}

/** 히어로 칩 바탕의 흰색 진하기. 시안처럼 바탕이 비치되 글자가 떠 보일 만큼. */
private const val HERO_CHIP_ALPHA = 0.20f

/** 히어로 기간 줄. 도시명보다 한 단계 흐리게 둬 도시명이 주인공이 되게 한다. */
private const val HERO_SUBTEXT_ALPHA = 0.85f

/** 빈 목록 자리의 그림 진하기. 글자보다 눈에 들어오면 주인공이 바뀐다. */
private const val EMPTY_STATE_ART_ALPHA = 0.35f

/**
 * 히어로 카드 오른쪽의 원 무늬(design/mockup/home). 사진이 없을 때 단색 면이 밋밋하지 않게 한다.
 *
 * 흰색을 아주 옅게 겹쳐 그린다 — 브랜드 색이 바뀌어도 무늬가 그 색을 따라간다.
 * 크기는 카드 높이에 비례해서, 카드 높이를 바꿔도 구도가 유지된다.
 */
@Composable
private fun BoxScope.HeroRings() {
    Canvas(modifier = Modifier.matchParentSize()) {
        val center = Offset(size.width * RING_CENTER_X, size.height * RING_CENTER_Y)
        val h = size.height
        drawCircle(Color.White.copy(alpha = RING_FILL_ALPHA), radius = h * RING_OUTER, center = center)
        drawCircle(
            Color.White.copy(alpha = RING_LINE_ALPHA),
            radius = h * RING_MIDDLE,
            center = center,
            style = Stroke(width = h * RING_STROKE),
        )
        drawCircle(Color.White.copy(alpha = RING_FILL_ALPHA), radius = h * RING_INNER, center = center)
    }
}

// 원 무늬 비율. 카드 폭·높이에 대한 비율이라 화면 크기와 무관하다. 시안을 보고 맞춘 값이다.
private const val RING_CENTER_X = 0.82f
private const val RING_CENTER_Y = 0.42f
private const val RING_OUTER = 0.72f
private const val RING_MIDDLE = 0.48f
private const val RING_INNER = 0.30f
private const val RING_STROKE = 0.09f
private const val RING_FILL_ALPHA = 0.10f
private const val RING_LINE_ALPHA = 0.14f
