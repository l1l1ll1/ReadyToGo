package kr.ac.baekseok.readytogo.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.PhotoCard
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.format.dateShort
import kr.ac.baekseok.readytogo.ui.format.daysText
import kr.ac.baekseok.readytogo.ui.theme.Tokens
import java.time.LocalDate

/**
 * "여행" 탭 — 저장된 일정 목록.
 *
 * 다가오는 여행과 지난 여행을 나눠 보여준다. 섞어 놓으면 날짜순 정렬만으로는
 * 어느 것이 아직 안 간 여행인지 한눈에 안 보인다.
 */
@Composable
fun TripsScreen(
    onOpenTrip: (SavedTrip) -> Unit,
    onCreateTrip: () -> Unit,
    viewModel: TripsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<SavedTrip?>(null) }

    Surface(color = LocalPalette.current.Background) {
        if (state.upcoming.isEmpty() && state.past.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                // 비어 있는 이유가 "아직 안 만들었다"와 "로그인을 안 했다" 두 가지다.
                // 같은 문구를 쓰면 둘러보기 중인 사용자가 저장했는데 사라졌다고 생각한다.
                if (state.canSave) {
                    EmptyState(
                        title = stringResource(R.string.trips_empty_title),
                        description = stringResource(R.string.trips_empty_body),
                        actionLabel = stringResource(R.string.home_create_trip),
                        onAction = onCreateTrip,
                    )
                } else {
                    EmptyState(
                        title = stringResource(R.string.trips_guest_title),
                        description = stringResource(R.string.trips_guest_body),
                        actionLabel = stringResource(R.string.home_create_trip),
                        onAction = onCreateTrip,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Tokens.Space.screenHorizontal,
                    end = Tokens.Space.screenHorizontal,
                    top = Tokens.Space.lg,
                    bottom = Tokens.Space.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
            ) {
                // 길게 누르기는 눌러보지 않으면 알 수 없다. 한 줄로 알려준다.
                item {
                    Text(
                        text = stringResource(R.string.trips_long_press_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalPalette.current.TextSecondary,
                    )
                }

                if (state.upcoming.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.home_upcoming)) }
                    items(state.upcoming, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            today = state.today,
                            onClick = { onOpenTrip(trip) },
                            onLongClick = { pendingDelete = trip },
                        )
                    }
                }

                if (state.past.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.trips_past),
                            modifier = Modifier.padding(top = Tokens.Space.lg),
                        )
                    }
                    items(state.past, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            today = state.today,
                            onClick = { onOpenTrip(trip) },
                            onLongClick = { pendingDelete = trip },
                        )
                    }
                }
            }
        }
    }

    // 삭제는 되돌릴 수 없으므로 반드시 한 번 묻는다.
    pendingDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.trips_delete_title)) },
            text = { Text(stringResource(R.string.trips_delete_body, trip.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(trip.id)
                    pendingDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = LocalPalette.current.Danger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/**
 * 일정 카드.
 *
 * 삭제는 **길게 누르기**로만 할 수 있다. 휴지통 버튼을 카드 위에 올려 두었더니
 * 카드를 열려고 누를 때 같이 걸리는 자리였다. 길게 누른 뒤에도 확인을 한 번 더 받는다.
 */
@Composable
private fun TripCard(
    trip: SavedTrip,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val daysLeft = trip.daysUntil(today)

    PhotoCard(
        key = trip.destinationName,
        title = trip.title,
        subtitle = stringResource(
            R.string.trips_subtitle,
            trip.destinationName,
            dateShort(trip.departureDate),
            dateShort(trip.returnDate),
            daysText(trip.dayCount),
        ),
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        if (daysLeft >= 0) {
            Surface(
                color = LocalPalette.current.Background,
                shape = Tokens.Radius.chip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Tokens.Space.md),
            ) {
                Text(
                    text = if (daysLeft == 0L) {
                        stringResource(R.string.home_departure_day)
                    } else {
                        stringResource(R.string.home_dday, daysLeft.toInt())
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
}

