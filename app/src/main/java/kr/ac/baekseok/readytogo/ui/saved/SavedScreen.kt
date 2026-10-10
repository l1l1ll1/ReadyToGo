package kr.ac.baekseok.readytogo.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.FavoriteKind
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.FavoriteButton
import kr.ac.baekseok.readytogo.ui.component.PhotoCard
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * "저장" 탭 — 즐겨찾기한 장소.
 *
 * 종류(식당·관광지·숙소) 필터를 위에 둔다. 식당만 보려는데 관광지가 섞여 있으면
 * 여행 중에 "어디서 먹지"를 찾는 데 쓸 수 없다.
 */
@Composable
fun SavedScreen(
    onOpenPlace: (FavoritePlace) -> Unit,
    viewModel: SavedViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Surface(color = LocalPalette.current.Background) {
        if (state.all.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                // 둘러보기 중에는 ♥ 자체를 쓸 수 없으므로 "♥를 누르면 모인다"는 안내가 거짓이 된다.
                if (state.canSave) {
                    EmptyState(
                        title = stringResource(R.string.saved_empty_title),
                        description = stringResource(R.string.saved_empty_body),
                    )
                } else {
                    EmptyState(
                        title = stringResource(R.string.saved_guest_title),
                        description = stringResource(R.string.saved_guest_body),
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
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            ) {
                item {
                    KindFilter(
                        selected = state.filter,
                        counts = state.countsByKind,
                        total = state.all.size,
                        onSelect = viewModel::setFilter,
                    )
                }

                if (state.visible.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.saved_filter_empty_title),
                            description = stringResource(R.string.saved_filter_empty_body),
                        )
                    }
                } else {
                    items(state.visible, key = { it.placeId }) { place ->
                        FavoriteRow(
                            place = place,
                            onClick = { onOpenPlace(place) },
                            onRemove = { viewModel.remove(place.placeId) },
                        )
                    }
                }
            }
        }
    }
}

/** 종류 필터. 개수를 함께 보여줘야 비어 있는 종류를 괜히 눌러보지 않는다. */
@Composable
private fun KindFilter(
    selected: FavoriteKind?,
    counts: Map<FavoriteKind, Int>,
    total: Int,
    onSelect: (FavoriteKind?) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        modifier = Modifier.padding(bottom = Tokens.Space.sm),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.saved_filter_all, total)) },
            shape = Tokens.Radius.chip,
        )
        FavoriteKind.entries.forEach { kind ->
            val count = counts[kind] ?: 0
            FilterChip(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                label = { Text("${kind.label} $count") },
                shape = Tokens.Radius.chip,
            )
        }
    }
}

@Composable
private fun FavoriteRow(
    place: FavoritePlace,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
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
            key = place.placeId,
            title = "",
            height = THUMB_SIZE,
            modifier = Modifier.width(THUMB_SIZE),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${place.category} · ${place.cityName}",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 저장 목록에서는 ♥가 항상 채워져 있다. 누르면 해제된다.
        FavoriteButton(favorite = true, onToggle = onRemove)
    }
}

private val THUMB_SIZE = Tokens.Space.xxl * 2