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
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.PhotoCard
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.format.dateShort
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
                        title = "저장된 일정이 없습니다",
                        description = "일정을 만들고 저장하면 여기에 모입니다.",
                        actionLabel = "새 여행 만들기",
                        onAction = onCreateTrip,
                    )
                } else {
                    EmptyState(
                        title = "둘러보기 중입니다",
                        description = "일정은 지금도 만들 수 있지만, 저장하려면 로그인이 필요합니다.",
                        actionLabel = "새 여행 만들기",
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
                        text = "카드를 길게 누르면 삭제할 수 있습니다.",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalPalette.current.TextSecondary,
                    )
                }

                if (state.upcoming.isNotEmpty()) {
                    item { SectionHeader(title = "다가오는 여행") }
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
                            title = "지난 여행",
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
            title = { Text("일정을 삭제할까요?") },
            text = { Text("\"${trip.title}\"을 삭제합니다. 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(trip.id)
                    pendingDelete = null
                }) {
                    Text("삭제", color = LocalPalette.current.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소") }
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
        subtitle = "${trip.destinationName} · " +
            "${dateShort(trip.departureDate)} – ${dateShort(trip.returnDate)} · " +
            "${trip.dayCount}일",
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
                    text = if (daysLeft == 0L) "출발일" else "D-$daysLeft",
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

