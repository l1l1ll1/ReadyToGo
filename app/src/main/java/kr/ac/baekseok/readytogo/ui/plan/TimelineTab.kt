package kr.ac.baekseok.readytogo.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.plan.TimelineEntry
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.TravelLeg
import kr.ac.baekseok.readytogo.domain.plan.TravelMode
import kr.ac.baekseok.readytogo.ui.format.dateWithDay
import kr.ac.baekseok.readytogo.ui.format.durationText
import kr.ac.baekseok.readytogo.ui.format.travelLegText
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.domain.purpose.PurposeAttractionTypes
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.component.FavoriteButton
import kr.ac.baekseok.readytogo.ui.component.PhotoAttributionLine
import kr.ac.baekseok.readytogo.ui.component.PlacePhotoThumb
import kr.ac.baekseok.readytogo.ui.component.rememberPlacePhoto
import kr.ac.baekseok.readytogo.ui.component.TagChip
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 일정표 탭 — 날짜 칩으로 전환하는 세로 타임라인.
 *
 * 전에는 하루치를 카드 한 장에 "가볼 곳 / 아침 / 점심 / 저녁" 네 덩어리로 묶어 보여줬다.
 * 그러면 **실제로 움직이는 순서**가 보이지 않는다 — 아침 먹고 어디를 가는지, 두 곳 사이가
 * 걸어갈 거리인지 알 수 없었다. 관광지와 식당을 시간 순으로 섞고 사이에 이동시간을 넣는다.
 *
 * @param onDeleted 항목을 뺐을 때. 일차 번호와 항목 키를 넘긴다.
 */
@Composable
fun TimelineTab(
    plan: TripPlan,
    selectedDayNumber: Int,
    edits: TimelineEdits,
    pickedPlaces: PickedPlaces,
    favoriteIds: Set<String>,
    onSelectDay: (Int) -> Unit,
    onOpenMapForDay: (Int) -> Unit,
    onItemClick: (Int, TimelineItem) -> Unit,
    onToggleFavorite: (FavoritePlace) -> Unit,
    onReorder: (Int, List<String>) -> Unit,
    /** 끌기가 끝났을 때. 바뀐 순서를 이때 한 번만 저장한다. */
    onReorderCommit: () -> Unit,
    onDelete: (Int, String) -> Unit,
) {
    val day = plan.dailyPlans.firstOrNull { it.dayNumber == selectedDayNumber }
        ?: plan.dailyPlans.firstOrNull()

    Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = ((day?.dayNumber ?: 1) - 1).coerceAtLeast(0),
            edgePadding = 0.dp,
        ) {
            plan.dailyPlans.forEach { candidate ->
                Tab(
                    selected = candidate.dayNumber == day?.dayNumber,
                    onClick = { onSelectDay(candidate.dayNumber) },
                    text = {
                        Text(stringResource(R.string.plan_day_tab, candidate.dayNumber))
                    },
                )
            }
        }

        if (day == null) {
            EmptyState(title = stringResource(R.string.plan_timeline_empty), description = "")
            return@Column
        }

        val savedOrder = edits.orderIn(day.dayNumber)

        // **끌리는 동안에는 ViewModel을 건드리지 않는다.** 한 칸 옮길 때마다 edits를
        // 갱신하면 화면 전체가 다시 그려지고 이동시간까지 다시 계산된다. 순서는 여기에
        // 담아 두고 손을 뗄 때 한 번만 넘긴다.
        //
        // 저장된 순서가 바뀌면(=손을 떼고 넘긴 순서가 돌아오면) remember가 초기화되며
        // 저절로 버려진다. 손으로 비우면 그 한 프레임 동안 예전 순서가 보여 깜빡인다.
        var dragOrder by remember(day.dayNumber, savedOrder) { mutableStateOf<List<String>?>(null) }

        val entries = day.toTimeline(
            deletedKeys = edits.deletedIn(day.dayNumber),
            pickedMeals = edits.pickedIn(day.dayNumber),
            customOrder = dragOrder ?: savedOrder,
            pickedAttractions = edits.pickedAttractionsIn(day.dayNumber),
            picked = pickedPlaces,
        )

        // 하루의 첫 끼니. 여기에는 놓을 수 없다 — 아침보다 앞은 하루가 시작되기 전이다.
        val firstMealKey = entries.firstOrNull { it.item !is TimelineItem.Visit }?.item?.key

        val listState = rememberLazyListState()

        // **손가락 위치로 휴지통 판정을 한다.** 전에는 끌리는 항목의 좌표를 매 프레임
        // onGloballyPositioned로 재서 비교했는데, 그 측정이 다시 그리기를 부르면서
        // 위로 끌 때 항목이 손가락을 못 따라왔다. 항목 크기와도 무관해져서 판정도 정확해진다.
        var trashBounds by remember { mutableStateOf<Rect?>(null) }
        var pointer by remember { mutableStateOf<Offset?>(null) }
        var listOrigin by remember { mutableStateOf(Offset.Zero) }
        var draggingKey by remember { mutableStateOf<String?>(null) }

        val overTrash = draggingKey != null &&
            pointer?.let { trashBounds?.contains(it + listOrigin) } == true

        // **지금 보이는 목록을 봐야 한다.** 람다는 처음 만들 때 한 번만 잡히므로 entries를
        // 그대로 쓰면 첫 이동 뒤로 옛 목록을 들고 계산한다 — 위로 옮기는 것이 안 먹던 원인이다.
        val currentEntries by rememberUpdatedState(entries)

        // 옮길 수 없는 이동이면 null이 와서 아무것도 하지 않는다.
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            moveVisit(currentEntries, from.index, to.index)?.let { keys -> dragOrder = keys }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { listOrigin = it.positionInWindow() }
                // 손가락 위치만 **엿본다.** Initial 패스에서 읽고 아무것도 소비하지 않아
                // 목록 스크롤과 끌기는 그대로 동작한다.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            pointer = event.changes.firstOrNull()?.position
                        }
                    }
                },
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = Tokens.Space.lg,
                    end = Tokens.Space.lg,
                    top = Tokens.Space.md,
                    bottom = Tokens.Space.xxl,
                ),
            ) {
                item {
                    DayHeader(
                        day = day,
                        totalMinutes = entries.sumOf { entry ->
                            entry.item.stayMinutes + (entry.travelFromPrevious?.minutes ?: 0)
                        },
                        onOpenMap = { onOpenMapForDay(day.dayNumber) },
                    )
                }

                if (entries.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.plan_day_empty_title),
                            description = stringResource(R.string.plan_day_empty_body),
                        )
                    }
                }

                items(entries, key = { it.item.key }) { entry ->
                    // **관광지만 끌 수 있다.** 끼니는 하루의 뼈대라 자리가 고정이고,
                    // 식당을 바꾸는 일은 상세 시트의 "다른 선택지"가 맡는다.
                    val draggable = entry.item is TimelineItem.Visit

                    ReorderableItem(
                        state = reorderState,
                        key = entry.item.key,
                        // 아침은 드롭 대상에서 뺀다. 여기에 놓을 수 없게 하는 것이
                        // "아침 위로는 못 올린다"를 지키는 가장 확실한 방법이다 —
                        // 콜백에서 거절하면 라이브러리는 이미 옮긴 줄 알고 그려서 튄다.
                        enabled = entry.item.key != firstMealKey,
                    ) { isDragging ->
                        val handle = if (draggable) {
                            Modifier.longPressDraggableHandle(
                                onDragStarted = { draggingKey = entry.item.key },
                                onDragStopped = {
                                    // 끌리는 동안 바뀐 순서를 여기서 한 번만 넘긴다.
                                    // 옮길 때마다 넘기면 한 번 끄는 사이에 화면 전체가
                                    // 수십 번 다시 그려지고 Firestore 쓰기도 그만큼 나간다.
                                    dragOrder?.let { onReorder(day.dayNumber, it) }
                                    if (overTrash) {
                                        onDelete(day.dayNumber, entry.item.key)
                                    } else {
                                        onReorderCommit()
                                    }
                                    draggingKey = null
                                },
                            )
                        } else {
                            Modifier
                        }

                        TimelineRow(
                            entry = entry,
                            favoriteIds = favoriteIds,
                            cityName = plan.condition.destination?.city.orEmpty(),
                            purposes = plan.condition.purposes,
                            // 추천이 없는 끼니는 보여줄 상세가 없다. 시트를 띄우지 않는다.
                            onClick = if (entry.item is TimelineItem.NoMeal) {
                                null
                            } else {
                                { onItemClick(day.dayNumber, entry.item) }
                            },
                            onToggleFavorite = onToggleFavorite,
                            dragging = isDragging,
                            modifier = handle,
                        )
                    }
                }
            }

            TrashTarget(
                visible = draggingKey != null,
                active = overTrash,
                onBounds = { trashBounds = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * 끌어서 옮긴 결과 순서. 옮길 수 없으면 null.
 *
 * **끼니를 건너뛰지 않고 있는 그대로 한 칸씩 옮긴다.** 빼내는 것이 관광지 하나뿐이라
 * 끼니끼리의 앞뒤는 저절로 지켜진다 — 점심 앞에 놓으면 아침~점심 틈, 뒤에 놓으면
 * 점심~저녁 틈이다. 전에는 끼니를 만나면 다음 관광지 자리까지 밀어 버렸는데,
 * 그러면 (1) 관광지가 없는 틈으로는 영영 옮길 수 없었고 (2) 라이브러리가 예상한
 * 자리와 실제 자리가 달라져 들린 항목이 튀었다.
 *
 * 아침 위로는 애초에 올 수 없다. 첫 끼니를 드롭 대상에서 빼 두었다(`enabled = false`).
 *
 * 목록 맨 앞의 날짜 머리글(LazyColumn의 첫 item) 때문에 인덱스가 하나씩 밀려 있다.
 */
private fun moveVisit(
    entries: List<TimelineEntry>,
    fromIndex: Int,
    toIndex: Int,
): List<String>? {
    val from = fromIndex - HEADER_ITEM_COUNT
    val to = toIndex - HEADER_ITEM_COUNT
    if (from !in entries.indices || to !in entries.indices || from == to) return null

    // 끼니는 끌 수 없다. 하루의 뼈대라 자리가 고정이고, 식당은 상세 시트에서 바꾼다.
    if (entries[from].item !is TimelineItem.Visit) return null

    return entries.map { it.item.key }.toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * 끌어다 놓으면 지우는 자리.
 *
 * **끌기 전에는 보이지 않는다.** 늘 떠 있으면 화면 아래를 가리기만 하고, 끌기 시작할 때
 * 나타나야 "여기 놓으면 지워진다"가 읽힌다.
 *
 * 닿으면 코랄로 바뀐다. 색이 바뀌지 않으면 놓아도 되는지 알 수 없어 손을 떼지 못한다.
 */
@Composable
private fun TrashTarget(
    visible: Boolean,
    active: Boolean,
    onBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(bottom = Tokens.Space.xl),
    ) {
        val palette = LocalPalette.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Tokens.Size.trashTarget)
                .clip(CircleShape)
                .background(if (active) palette.Accent else palette.SurfaceMuted)
                .onGloballyPositioned { onBounds(it.boundsInWindow()) },
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.plan_drag_to_delete),
                tint = if (active) palette.Surface else palette.TextSecondary,
            )
        }
    }
}

/** LazyColumn 맨 앞의 날짜 머리글. 끌기 인덱스를 항목 인덱스로 바꿀 때 빼야 한다. */
private const val HEADER_ITEM_COUNT = 1

@Composable
private fun DayHeader(day: DayPlan, totalMinutes: Int, onOpenMap: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Tokens.Space.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.plan_day_header,
                    day.dayNumber,
                    dateWithDay(day.date),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            // 하루가 얼마나 빡빡한지 숫자로 알려준다. 항목만 세어서는 감이 안 온다.
            if (totalMinutes > 0) {
                Text(
                    text = stringResource(R.string.plan_day_total, durationText(totalMinutes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalPalette.current.TextSecondary,
                )
            }
        }
        TextButton(onClick = onOpenMap) { Text(stringResource(R.string.plan_open_map)) }
    }
}

/**
 * 타임라인 한 줄 — 왼쪽에 점과 세로줄, 오른쪽에 내용.
 *
 * 이동시간은 **항목 위**에 둔다. 아래에 두면 마지막 항목 뒤에 갈 곳 없는 이동이 남는다.
 */
@Composable
private fun TimelineRow(
    entry: TimelineEntry,
    favoriteIds: Set<String>,
    cityName: String,
    /** 목적 태그를 어떤 기준으로 붙일지. 비어 있으면 태그를 달지 않는다. */
    purposes: Set<TravelPurpose>,
    onClick: (() -> Unit)?,
    onToggleFavorite: (FavoritePlace) -> Unit,
    /** 지금 끌리고 있는지. 들어 올린 티를 내려고 배경을 깐다. */
    dragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(Tokens.Radius.card)
            .background(
                if (dragging) LocalPalette.current.PointLight else Color.Transparent,
            ),
    ) {
        entry.travelFromPrevious?.let { TravelRow(it) }

        Row(modifier = Modifier.fillMaxWidth()) {
            TimelineRail(icon = entry.item.icon)

            // 사진은 설정에서 켠 경우에만 받는다(rememberPlacePhoto). 꺼져 있으면 null이라
            // 썸네일 자리 자체가 생기지 않고 지금처럼 글자만 남는다.
            val placeId = entry.item.placeId
            val photo = placeId?.let { rememberPlacePhoto(it) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
                modifier = Modifier
                    .weight(1f)
                    .clip(Tokens.Radius.card)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(
                        start = Tokens.Space.md,
                        end = Tokens.Space.sm,
                        top = Tokens.Space.sm,
                        bottom = Tokens.Space.lg,
                    ),
            ) {
                if (photo != null && placeId != null) {
                    PlacePhotoThumb(
                        placeId = placeId,
                        photo = photo,
                        size = Tokens.Size.placeThumb,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    when (val item = entry.item) {
                        is TimelineItem.Visit -> VisitContent(item, purposes)
                        is TimelineItem.Meal -> MealContent(item)
                        is TimelineItem.NoMeal -> NoMealContent(item)
                    }
                    PhotoAttributionLine(photo)
                }
            }

            // ♥는 줄 전체 클릭과 겹치지 않게 Row의 바깥에 둔다.
            // 추천이 없는 끼니는 담을 것이 없어 자리만 비워 둔다.
            entry.item.toFavoritePlace(cityName)?.let { place ->
                FavoriteButton(
                    favorite = place.placeId in favoriteIds,
                    onToggle = { onToggleFavorite(place) },
                )
            }
        }
    }
}

/** 왼쪽 세로줄과 점. 항목이 한 줄로 이어져 보이게 한다. */
@Composable
private fun TimelineRail(icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(Tokens.Size.timelineRailWidth),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Tokens.Space.xxl)
                .clip(CircleShape)
                .background(LocalPalette.current.PointLight),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalPalette.current.PointDark,
                modifier = Modifier.size(Tokens.Space.lg),
            )
        }
        // 점 아래로 선을 늘려 다음 항목까지 이어 붙인다.
        Box(
            modifier = Modifier
                .width(Tokens.Size.timelineLineWidth)
                .height(Tokens.Size.timelineLineHeight)
                .background(LocalPalette.current.Divider),
        )
    }
}

@Composable
private fun VisitContent(item: TimelineItem.Visit, purposes: Set<TravelPurpose>) {
    val attraction = item.attraction
    Text(
        text = attraction.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )

    // **이 곳이 어떤 목적에 맞아서 들어왔는지** 보여준다. 점수에 목적 일치 가산점을 준
    // 바로 그 판정이라(AttractionScoring), 태그가 붙은 곳이 상위에 온 이유가 드러난다.
    // 맞는 목적이 없으면 붙일 이름이 없으므로 아무것도 그리지 않는다 — 평점·동선만으로
    // 올라온 곳도 있고, 그 자리에 "해당 없음" 같은 칸을 두면 흠처럼 보인다.
    val matched = PurposeAttractionTypes.matched(attraction.types, purposes)
    if (matched.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        ) {
            matched.forEach { purpose -> TagChip(text = stringResource(purpose.labelRes)) }
        }
    }

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
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
    Text(
        text = durationText(item.stayMinutes),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/**
 * 끼니 한 줄.
 *
 * 예산 초과 빨간 표시와 현지 통화 병기를 여기서도 유지한다 — 카드에서만 보이고 타임라인에서
 * 사라지면 같은 식당이 화면에 따라 다르게 보인다. 표기 함수는 결과 화면과 공유한다.
 */
@Composable
private fun MealContent(item: TimelineItem.Meal) {
    val restaurant = item.restaurant
    val overBudget = item.shown.overBudget
    // 예산 초과는 코랄. 붉은 Danger는 되돌릴 수 없는 일에만 쓴다(SummaryTab 주석 참고).
    val accent = if (overBudget) LocalPalette.current.Accent else LocalPalette.current.Point

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(item.meal.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        if (overBudget) {
            Text(
                text = stringResource(R.string.plan_over_budget_suffix),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
        }
    }
    Text(
        text = restaurant.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = restaurant.detailLine(),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
    Text(
        // priceText()가 원화와 현지 통화를 줄바꿈으로 함께 돌려준다.
        text = restaurant.priceText(),
        style = MaterialTheme.typography.bodyMedium,
        color = if (overBudget) accent else LocalPalette.current.TextPrimary,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = durationText(item.stayMinutes),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/**
 * 추천을 못 찾은 끼니.
 *
 * 왜 없는지 적는다 — 그냥 "없음"만 두면 앱이 빠뜨린 것으로 보인다.
 * 반경을 넓혀 다시 찾아도 없었다는 뜻이라, 사용자가 직접 찾아보는 수밖에 없다.
 */
@Composable
private fun NoMealContent(item: TimelineItem.NoMeal) {
    Text(
        text = stringResource(item.meal.labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.TextSecondary,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(R.string.plan_no_meal_title),
        style = MaterialTheme.typography.titleSmall,
        color = LocalPalette.current.TextSecondary,
    )
    Text(
        text = stringResource(R.string.plan_no_meal_body),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
    )
}

/** 두 항목 사이의 이동. 세로줄 자리에 아이콘을 맞춰 줄이 끊기지 않게 한다. */
@Composable
private fun TravelRow(leg: TravelLeg) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Tokens.Space.sm),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(Tokens.Size.timelineRailWidth),
        ) {
            Icon(
                imageVector = when (leg.mode) {
                    TravelMode.WALK -> Icons.Outlined.DirectionsWalk
                    TravelMode.TRANSIT -> Icons.Outlined.DirectionsBus
                },
                contentDescription = null,
                tint = LocalPalette.current.TextSecondary,
                modifier = Modifier.size(Tokens.Space.lg),
            )
        }
        Text(
            text = "${travelLegText(leg)} · ${"%.1f".format(leg.distanceKm)}km",
            style = MaterialTheme.typography.labelMedium,
            color = LocalPalette.current.TextSecondary,
            modifier = Modifier.padding(start = Tokens.Space.md),
        )
    }
}

private val TimelineItem.icon: ImageVector
    get() = when (this) {
        is TimelineItem.Visit -> Icons.Outlined.Place
        is TimelineItem.Meal -> Icons.Outlined.LocalCafe
        is TimelineItem.NoMeal -> Icons.Outlined.HelpOutline
    }

/** 장소의 id. 추천을 못 찾은 끼니는 가리킬 장소가 없다. */
internal val TimelineItem.placeId: String?
    get() = when (this) {
        is TimelineItem.Visit -> attraction.id
        is TimelineItem.Meal -> restaurant.id
        is TimelineItem.NoMeal -> null
    }

private fun TimelineItem.toFavoritePlace(cityName: String): FavoritePlace? = when (this) {
    is TimelineItem.Visit -> attraction.toFavorite(cityName)
    is TimelineItem.Meal -> restaurant.toFavorite(cityName)
    // 담을 장소가 없다. 호출부가 이 항목에는 하트를 그리지 않는다.
    is TimelineItem.NoMeal -> null
}
