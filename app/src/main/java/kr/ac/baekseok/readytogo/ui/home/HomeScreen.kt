package kr.ac.baekseok.readytogo.ui.home

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

            // 새 여행 만들기는 이 화면에서 가장 자주 하는 일이라 목록과 같이 스크롤되면 안 된다.
            // 목록 맨 위 버튼을 FAB로 옮겨 어디까지 내려가도 손에 닿게 한다.
            ExtendedFloatingActionButton(
                onClick = onCreateTrip,
                // 코랄은 "지금 눌러야 할 것"에 쓴다. 터쿼이즈로 두면 탭 바와 같은 색이라
                // 바닥에 버튼이 하나 더 있는 것처럼 보인다.
                containerColor = LocalPalette.current.Accent,
                contentColor = LocalPalette.current.OnAccent,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_create_trip)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Tokens.Space.lg),
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
            top = Tokens.Space.lg,
            // FAB에 가려지지 않게 아래를 더 비운다.
            bottom = Tokens.Space.xxl * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.lg),
    ) {
        item { Greeting(state.authState, state.nickname) }

        // 다가오는 여행이 없으면 카드를 아예 내린다. 빈 카드는 자리만 차지한다.
        state.upcoming?.let { trip ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm)) {
                    SectionHeader(title = stringResource(R.string.home_upcoming))
                    UpcomingTripCard(
                        trip = trip,
                        today = state.today,
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
                    // (모노크롬용 drawable은 <bitmap> XML이라 painterResource로 못 읽는다 —
                    //  그걸 넣었다가 "Only VectorDrawables and rasterized asset types" 로 죽었다.)
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
            items(state.recent, key = { it.id }) { trip ->
                RecentTripRow(trip = trip, today = state.today, onClick = { onOpenTrip(trip) })
            }
        }
    }
}

@Composable
private fun Greeting(authState: AuthState, nickname: String?) {
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
            style = MaterialTheme.typography.titleLarge,
        )
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
 * 다가오는 여행 카드. D-day를 사진 위 오른쪽에 배지로 올린다.
 *
 * D-day를 제목보다 눈에 띄게 두는 이유: 이 카드를 보는 이유가 "며칠 남았나"다.
 */
@Composable
private fun UpcomingTripCard(trip: SavedTrip, today: LocalDate, onClick: () -> Unit) {
    val daysLeft = trip.daysUntil(today)

    PhotoCard(
        key = trip.destinationName,
        title = savedTripTitle(trip),
        subtitle = stringResource(
            R.string.fmt_date_range_days,
            dateShort(trip.departureDate),
            dateShort(trip.returnDate),
            daysText(trip.dayCount),
        ),
        // 목적지가 어디든 이 카드만은 브랜드 색으로 둔다. 홈에서 가장 먼저 보이는 면이다.
        colors = LocalPalette.current.HeroGradient,
        // 터쿼이즈 자체가 충분히 어두워서 덮개 없이도 흰 글자가 읽힌다.
        scrim = false,
        height = Tokens.Size.heroCardHeight,
        onClick = onClick,
    ) {
        Surface(
            color = LocalPalette.current.Background,
            shape = Tokens.Radius.chip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Tokens.Space.md),
        ) {
            Text(
                text = when {
                    daysLeft > 0 -> stringResource(R.string.home_dday, daysLeft.toInt())
                    // 떠난 뒤로는 "3일차". 끝난 여행은 이 카드에 오지 않는다(isUpcoming).
                    //
                    // **출발일도 1일차로 센다.** 첫날만 "출발일"로 두면 카드가 날마다
                    // 다른 종류의 말을 하게 되고, 여행 중 모드가 이미 "1일차"로 열려 있는데
                    // 홈만 다른 말을 쓰면 같은 날을 두 이름으로 부르는 셈이 된다.
                    //
                    // 일정표 날짜 칩과 같은 문구를 쓴다 — 두 곳이 다른 말로 같은 것을 가리키면 헷갈린다.
                    else -> stringResource(R.string.plan_day_tab, trip.dayNumberOn(today) ?: trip.dayCount)
                },
                style = MaterialTheme.typography.labelLarge,
                // D-day는 "며칠 남았는지"를 눈에 걸리게 하는 자리라 코랄을 쓴다.
                // 터쿼이즈로 두면 카드 배경과 같은 계열이라 배지가 배경에 묻힌다.
                color = LocalPalette.current.Accent,
                modifier = Modifier.padding(
                    horizontal = Tokens.Space.md,
                    vertical = Tokens.Space.sm,
                ),
            )
        }
    }
}

/**
 * 최근 일정 한 줄. 작은 사진 + 제목·날짜.
 *
 * **지난 여행도 섞어 둔다.** "최근"은 최근에 저장한 것이지 앞으로 갈 것이 아니다 —
 * 다녀온 일정을 다시 열어보는 경우가 많다. 대신 다녀온 것에는 칩을 붙여 구분한다.
 */
@Composable
private fun RecentTripRow(trip: SavedTrip, today: LocalDate, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .clickable(onClick = onClick)
            .background(LocalPalette.current.SurfaceMuted)
            .padding(Tokens.Space.md),
    ) {
        // 목록 줄의 썸네일은 사진 자리가 아니라 **표식**이다. 목적지마다 색이 달라지면
        // 다섯 줄이 알록달록해져 제목이 안 읽힌다. 한 가지 바탕에 작은 아이콘만 얹는다.
        //
        // **다크에서는 연노랑을 쓰지 않는다.** 어두운 카키로 내려가 탁해지고, 그 위에 얹힌
        // 여러 색 아이콘이 흙빛으로 섞인다. 그때는 면을 가라앉히고 포인트 색 아이콘
        // 하나로 표식을 삼는다 — 라이트에서 연노랑이 하던 일을 색 대비가 대신한다.
        // 대표 장소 사진이 있으면 그것을 쓴다. 사진은 목적지마다 달라도 알록달록하지 않다 —
        // 위 걱정은 **색 그라데이션**을 썼을 때의 이야기였다. 사진이 없거나 설정에서 껐으면
        // 아래 표식이 그대로 남는다.
        val photo = trip.coverPlaceId?.let { rememberPlacePhoto(it) }
        val dark = isAppInDarkTheme()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(THUMB_SIZE)
                .clip(Tokens.Radius.button)
                .background(
                    if (dark) LocalPalette.current.SurfaceMuted else LocalPalette.current.Secondary,
                ),
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_art),
                    contentDescription = null,
                    colorFilter = if (dark) ColorFilter.tint(LocalPalette.current.Point) else null,
                    modifier = Modifier.size(THUMB_ICON_SIZE),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = savedTripTitle(trip),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.home_recent_subtitle,
                    savedTripDestination(trip),
                    daysText(trip.dayCount),
                    dateShort(trip.departureDate),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!trip.isUpcoming(today)) {
            PastTripChip()
        }
    }
}

private val THUMB_SIZE = Tokens.Space.xxl * 2

/** 썸네일 안 아이콘. 네모를 꽉 채우면 표식이 아니라 잘린 그림처럼 보인다. */
private val THUMB_ICON_SIZE = Tokens.Space.xxl * 1.4f

/** 빈 목록 자리의 그림 진하기. 글자보다 눈에 들어오면 주인공이 바뀐다. */
private const val EMPTY_STATE_ART_ALPHA = 0.35f
