package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import kr.ac.baekseok.readytogo.ui.component.DashedDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kr.ac.baekseok.readytogo.ui.component.PlacePhotoThumb
import kr.ac.baekseok.readytogo.ui.component.TagChip
import kr.ac.baekseok.readytogo.ui.component.rememberPlacePhoto
import kr.ac.baekseok.readytogo.ui.format.durationText
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 타임라인 항목을 눌렀을 때 뜨는 상세 시트.
 *
 * 세 가지를 할 수 있다 — 지도에서 열기, 대안으로 교체, 일정에서 빼기.
 * 별도 화면으로 만들지 않은 이유: 하루에 항목이 대여섯 개라 들어갔다 나오는 일이 잦다.
 *
 * @param onPick 끼니를 다른 후보로 바꾼다. 관광지이거나 후보가 하나뿐이면 null이라
 *        목록 자체가 나오지 않는다 — 고를 것이 없는데 목록을 보여주면 고장으로 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailSheet(
    item: TimelineItem,
    onOpenMap: (String) -> Unit,
    onPick: ((String) -> Unit)?,
    /**
     * 같은 도시, 같은 종류로 이미 걸러 둔 즐겨찾기. 비어 있으면 섹션이 나오지 않는다.
     */
    favorites: List<FavoritePlace>,
    /** 그날 일정의 중심점. 멀리 있는 곳에 표시를 붙이는 기준이다. */
    dayCenter: LatLng?,
    /** 즐겨찾기를 끼워 넣을 때. null이면 끼울 수 없는 자리다. */
    onPickFavorite: ((FavoritePlace) -> Unit)?,
    /** 이 장소 하나를 공유할 때. null이면 보낼 장소가 없는 자리다. */
    onShare: (() -> Unit)?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalPalette.current.Background,
        shape = Tokens.Radius.sheet,
    ) {
        Column(
            // **스크롤이 반드시 있어야 한다.** 전에는 내용이 짧아 없어도 됐는데,
            // "다른 선택지"와 "내가 저장한 곳" 두 목록이 붙으면서 화면을 넘긴다.
            // 스크롤이 없으면 아래 섹션에 영영 닿을 수 없다 — 실제로 끼니 시트에서
            // "내가 저장한 곳"이 보이지 않았다.
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Tokens.Space.screenHorizontal,
                    end = Tokens.Space.screenHorizontal,
                    bottom = Tokens.Space.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            when (item) {
                is TimelineItem.Visit -> VisitDetail(item)
                is TimelineItem.Meal -> MealDetail(item)
                // 추천이 없는 끼니는 상세를 띄우지 않는다. 호출부가 막지만 타입상 분기가 필요하다.
                is TimelineItem.NoMeal -> Text(
                    text = stringResource(R.string.plan_sheet_no_restaurant),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            DashedDivider(color = LocalPalette.current.Divider)

            val mapUrl = item.mapUrl
            if (mapUrl.isNotBlank()) {
                SheetAction(
                    icon = Icons.Outlined.Map,
                    label = stringResource(R.string.plan_sheet_open_map),
                    onClick = {
                        onOpenMap(mapUrl)
                        onDismiss()
                    },
                )
            }

            if (onShare != null) {
                SheetAction(
                    icon = Icons.Outlined.Share,
                    label = stringResource(R.string.share_place),
                    onClick = {
                        onShare()
                        onDismiss()
                    },
                )
            }

            SheetAction(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.plan_sheet_remove),
                tint = LocalPalette.current.Danger,
                onClick = {
                    onDelete()
                    onDismiss()
                },
            )

            if (onPick != null && item is TimelineItem.Meal) {
                MealOptions(item = item, onPick = onPick, onDismiss = onDismiss)
            }

            if (onPickFavorite != null && favorites.isNotEmpty()) {
                SavedPlaces(
                    favorites = favorites,
                    dayCenter = dayCenter,
                    onPick = onPickFavorite,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/**
 * 같은 끼니의 다른 후보들.
 *
 * **버튼 하나로 뒤집는 대신 목록에서 고르게 한다.** 전에는 "대안으로 교체"가 추천과
 * 대안 둘 사이를 오갈 뿐이라, 마음에 드는 곳이 셋째였으면 닿을 방법이 없었다.
 *
 * 지금 보여주는 곳은 목록에서 빼지 않고 **고른 표시**를 해 둔다. 빼 버리면 뭘 고른
 * 상태인지 목록만 보고는 알 수 없고, 추천으로 되돌릴 방법도 없어진다.
 */
@Composable
private fun MealOptions(
    item: TimelineItem.Meal,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = item.options
    if (options.size <= 1) return

    DashedDivider(color = LocalPalette.current.Divider)

    Text(
        text = stringResource(R.string.plan_sheet_other_options),
        style = MaterialTheme.typography.labelLarge,
        color = LocalPalette.current.TextSecondary,
        modifier = Modifier.padding(top = Tokens.Space.sm),
    )

    options.forEach { option ->
        val chosen = option.item.id == item.restaurant.id
        MealOptionRow(
            option = option,
            chosen = chosen,
            onClick = {
                onPick(option.item.id)
                onDismiss()
            },
        )
    }
}

@Composable
private fun MealOptionRow(
    option: Recommendation<Restaurant>,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    val restaurant = option.item
    val photo = rememberPlacePhoto(restaurant.id)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.Space.sm),
    ) {
        PlacePhotoThumb(placeId = restaurant.id, photo = photo, size = Tokens.Size.placeThumb)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = restaurant.detailLine(),
                style = MaterialTheme.typography.bodySmall,
                color = LocalPalette.current.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = restaurant.priceText().replace("\n", " · "),
                style = MaterialTheme.typography.bodySmall,
                // 예산 초과는 코랄. 붉은 Danger는 되돌릴 수 없는 일에만 쓴다.
                color = if (option.overBudget) {
                    LocalPalette.current.Accent
                } else {
                    LocalPalette.current.TextSecondary
                },
            )
            // 고를 때 비교할 수 있게 후보마다 동선 근거를 붙인다. 위쪽 상세와 같은 문구다.
            option.detourLine()?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPalette.current.TextSecondary,
                )
            }
        }

        // 지금 고른 곳. 체크 하나로 표시한다 — 목록에서 빼면 되돌릴 방법이 없어진다.
        if (chosen) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.plan_sheet_chosen),
                tint = LocalPalette.current.Point,
            )
        }
    }
}

@Composable
private fun VisitDetail(item: TimelineItem.Visit) {
    val attraction = item.attraction
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Text(text = attraction.name, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.plan_attraction) + " · " +
                if (attraction.reviewCount > 0) {
                    stringResource(
                        R.string.plan_rating_reviews,
                        attraction.rating.toString(),
                        "%,d".format(attraction.reviewCount),
                    )
                } else {
                    "★${attraction.rating}"
                },
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPalette.current.TextSecondary,
        )
        if (attraction.address.isNotBlank()) {
            Text(
                text = attraction.address,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
            )
        }
        Text(
            text = stringResource(R.string.plan_sheet_stay_time, durationText(item.stayMinutes)),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalPalette.current.TextSecondary,
        )
    }
}

@Composable
private fun MealDetail(item: TimelineItem.Meal) {
    val restaurant = item.restaurant
    val overBudget = item.shown.overBudget

    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(item.meal.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point,
                fontWeight = FontWeight.Bold,
            )
            if (overBudget) {
                Text(
                    text = stringResource(R.string.plan_over_budget_suffix),
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalPalette.current.Accent,
                )
            }
        }
        Text(text = restaurant.name, style = MaterialTheme.typography.titleLarge)
        // detailLine에 카테고리·평점·주소가 이미 들어 있다. 주소를 따로 또 찍으면 두 번 나온다.
        Text(
            text = restaurant.detailLine(),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalPalette.current.TextSecondary,
        )
        Text(
            text = restaurant.priceText(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.TextPrimary,
        )
        item.shown.detourLine()?.let { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalPalette.current.TextSecondary,
            )
        }

        // 무엇으로 바꿀 수 있는지는 아래 "다른 선택지" 목록이 전부 보여준다.
        // 전에는 바뀔 한 곳만 미리 적었는데, 고를 수 있는 곳이 늘면서 쓸모가 없어졌다.
    }
}

/**
 * 왜 이 식당이 추천됐는지 동선 근거 한 줄. 점수를 우회 거리로 매기므로 근거도 우회 거리다.
 *
 * 즐겨찾기에서 직접 끼운 곳은 점수를 매기지 않아 값이 없고, 그때는 줄을 그리지 않는다.
 * 소수 첫째 자리에서 0.0으로 보일 만큼 작으면 "0.0km 돌아감"보다 "가는 길에 있음"이 바로 읽힌다.
 */
@Composable
private fun Recommendation<*>.detourLine(): String? {
    val km = detourKm ?: return null
    return if (km < ON_THE_WAY_KM) {
        stringResource(R.string.plan_meal_on_the_way)
    } else {
        stringResource(R.string.plan_meal_detour, "%.1f".format(km))
    }
}

/** 이보다 덜 돌아가면 "가는 길"로 본다. 소수 첫째 자리로 표시하면 0.0이 되는 크기다. */
private const val ON_THE_WAY_KM = 0.05

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = LocalPalette.current.TextPrimary,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}

/** 지도 앱으로 넘길 링크. Mock 데이터에는 없어서 비어 있을 수 있고, 그때는 버튼을 숨긴다. */
private val TimelineItem.mapUrl: String
    get() = when (this) {
        is TimelineItem.Visit -> attraction.mapUrl
        is TimelineItem.Meal -> restaurant.mapUrl
        is TimelineItem.NoMeal -> ""
    }

/**
 * 내가 저장한 곳에서 골라 끼운다.
 *
 * **고르는 순간에만 Places 상세 조회가 나간다.** 이 목록을 그리는 것으로는 호출이
 * 없다 — 즐겨찾기에 이미 있는 이름·좌표만으로 줄을 만든다.
 *
 * 그날 동선 중심에서 먼 곳은 표시만 하고 막지는 않는다. 멀어도 가고 싶어서 담아 둔
 * 곳일 수 있고, 그 판단은 사용자 몫이다.
 */
@Composable
private fun SavedPlaces(
    favorites: List<FavoritePlace>,
    dayCenter: LatLng?,
    onPick: (FavoritePlace) -> Unit,
    onDismiss: () -> Unit,
) {
    DashedDivider(color = LocalPalette.current.Divider)

    Text(
        text = stringResource(R.string.plan_sheet_saved_places),
        style = MaterialTheme.typography.labelLarge,
        color = LocalPalette.current.TextSecondary,
        modifier = Modifier.padding(top = Tokens.Space.sm),
    )

    favorites.forEach { favorite ->
        val km = dayCenter?.let { distanceKm(favorite.location, it) }
        SavedPlaceRow(
            favorite = favorite,
            far = km != null && km > FAR_FROM_CENTER_KM,
            onClick = {
                onPick(favorite)
                onDismiss()
            },
        )
    }
}

@Composable
private fun SavedPlaceRow(favorite: FavoritePlace, far: Boolean, onClick: () -> Unit) {
    val photo = rememberPlacePhoto(favorite.placeId)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.Radius.card)
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.Space.sm),
    ) {
        PlacePhotoThumb(placeId = favorite.placeId, photo = photo, size = Tokens.Size.placeThumb)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = favorite.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = favorite.category.ifBlank { stringResource(favorite.kind.labelRes) },
                style = MaterialTheme.typography.bodySmall,
                color = LocalPalette.current.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 멀다고 못 고르게 하지는 않는다. 알고 고르게만 한다.
        if (far) {
            TagChip(
                text = stringResource(R.string.plan_sheet_far_away),
                container = LocalPalette.current.Secondary,
                content = LocalPalette.current.TextPrimary,
            )
        }
    }
}

/**
 * 이 거리를 넘으면 "거리 먼 곳"을 붙인다.
 *
 * 10km는 도심 안에서 지하철로 20~30분쯤이다. 그 이상이면 하루 일정에 끼울 때
 * 이동만으로 한 시간이 날아간다.
 */
private const val FAR_FROM_CENTER_KM = 10.0
