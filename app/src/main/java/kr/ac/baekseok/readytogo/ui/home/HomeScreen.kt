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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.PhotoCard
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                containerColor = LocalPalette.current.Point,
                contentColor = Color.White,
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
                    // 앱 아이콘 심볼을 옅게 깔아 "아직 아무것도 없는 자리"로 읽히게 한다.
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(LocalPalette.current.PointLight),
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
                RecentTripRow(trip = trip, onClick = { onOpenTrip(trip) })
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
        title = trip.title,
        subtitle = "${trip.departureDate.format(DATE_FORMAT)} – " +
            "${trip.returnDate.format(DATE_FORMAT)} · ${trip.dayCount}일",
        // 목적지가 어디든 이 카드만은 브랜드 색으로 둔다. 홈에서 가장 먼저 보이는 면이다.
        colors = LocalPalette.current.Sunset,
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
                // 출발일이 오늘이면 "D-0"이 아니라 "출발일"이 읽기 낫다.
                text = when {
                    daysLeft > 0 -> stringResource(R.string.home_dday, daysLeft.toInt())
                    daysLeft == 0L -> stringResource(R.string.home_departure_day)
                    else -> stringResource(R.string.home_travelling)
                },
                style = MaterialTheme.typography.labelLarge,
                color = LocalPalette.current.Point,
                modifier = Modifier.padding(
                    horizontal = Tokens.Space.md,
                    vertical = Tokens.Space.sm,
                ),
            )
        }
    }
}

/** 최근 일정 한 줄. 작은 사진 + 제목·날짜. */
@Composable
private fun RecentTripRow(trip: SavedTrip, onClick: () -> Unit) {
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
        PhotoCard(
            key = trip.destinationName,
            title = "",
            height = THUMB_SIZE,
            modifier = Modifier.width(THUMB_SIZE),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${trip.destinationName} · ${trip.dayCount}일 · " +
                    trip.departureDate.format(DATE_FORMAT),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val THUMB_SIZE = Tokens.Space.xxl * 2
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M.d")